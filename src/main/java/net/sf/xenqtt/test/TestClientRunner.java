/**
    Copyright 2013 James McClure

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package net.sf.xenqtt.test;

import java.io.File;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.xenqtt.AppContext;
import net.sf.xenqtt.Log;
import net.sf.xenqtt.XenqttUtil;
import net.sf.xenqtt.client.AsyncClientListener;
import net.sf.xenqtt.client.MqttClient;
import net.sf.xenqtt.client.MqttClientConfig;
import net.sf.xenqtt.client.MqttClientFactory;
import net.sf.xenqtt.client.PublishMessage;
import net.sf.xenqtt.client.Subscription;
import net.sf.xenqtt.message.QoS;
import net.sf.xenqtt.test.XenqttTestClient.ClientType;
import net.sf.xenqtt.test.XenqttTestClientStats.Gap;

/**
 * Executes and manages load tests run by the Xenqtt test client.
 */
final class TestClientRunner extends Thread {

	private final AppContext context;
	private final TestClientConfiguration configuration;
	private final boolean async;

	private final XenqttTestClientStats stats;
	private final Thread statsReporterThread;

	private final StageControl stageControl;
	private final Semaphore inFlight;
	private final AsyncClientListener listener;

	private final MqttClientFactory factory;

	TestClientRunner(AppContext context) {
		super("TestClientRunner");
		this.context = context;
		configuration = new TestClientConfiguration(context);
		async = configuration.clientType == ClientType.ASYNC;

		stats = new XenqttTestClientStats(configuration.clientType);
		statsReporterThread = new StatsReporter(stats);
		stageControl = createStageControl();
		inFlight = new Semaphore(configuration.maxInFlightMessages);
		listener = new TestClientAsyncClientListener(stats, stageControl, inFlight);
		factory = createMqttClientFactory();
	}

	private StageControl createStageControl() {
		int connectionsToAwait = async ? configuration.publishers + configuration.subscribers : 0;
		if (configuration.isTimeBasedTest()) {
			return new StageControl(connectionsToAwait, configuration.duration);
		}

		int messagesToPublish = configuration.publishers > 0 ? configuration.publishers * configuration.messagesToPublish : 0;
		int messagesToReceive = configuration.subscribers > 0 ? configuration.messagesToReceive : 0;

		return new StageControl(connectionsToAwait, messagesToPublish, messagesToReceive);
	}

	private MqttClientFactory createMqttClientFactory() {
		MqttClientConfig config = getClientConfiguration();

		return new MqttClientFactory(configuration.brokerUri, configuration.messageHandlerThreadPoolSize, !async, config);
	}

	private MqttClientConfig getClientConfiguration() {
		MqttClientConfig config = new MqttClientConfig();
		config.setConnectTimeoutSeconds(configuration.connectTimeoutSeconds);
		config.setMessageResendIntervalSeconds(configuration.messageResendIntervalSeconds);
		config.setReconnectionStrategy(configuration.reconnectionStrategy);

		return config;
	}

	/**
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		try {
			System.out.println("Starting the Xenqtt test client.");

			stats.testStarted();
			statsReporterThread.start();

			Map<Type, List<MqttClient>> clients = createClients();
			doConnect(clients);

			if (configuration.topicToPublishTo != null && configuration.publishers > 0) {
				int id = 0;
				AtomicInteger messageIds = new AtomicInteger();
				for (MqttClient client : clients.get(Type.PUBLISHERS)) {
					new Publisher(id++, client, async, inFlight, configuration.topicToPublishTo, configuration.messageSize, configuration.messagesToPublish,
							messageIds, configuration.qos, stats, stageControl).start();
				}
			}

			if (configuration.topicToSubscribeTo != null && (configuration.messagesToReceive > 0 || configuration.isTimeBasedTest())) {
				for (MqttClient client : clients.get(Type.SUBSCRIBERS)) {
					client.subscribe(new Subscription[] { new Subscription(configuration.topicToSubscribeTo, configuration.qos) });
				}
			}

			stageControl.awaitTestCompletion();
			stats.testEnded();

			shutdown(clients);
			System.out.println("Xenqtt Test Client Complete");
			context.applicationDone();
		} catch (Exception ex) {
			System.err.println("Unable to run the Xenqtt test client.");
			ex.printStackTrace();
		}
	}

	private Map<Type, List<MqttClient>> createClients() {
		Map<Type, List<MqttClient>> clients = new EnumMap<Type, List<MqttClient>>(Type.class);
		if (configuration.publishers > 0) {
			clients.putAll(createClients(Type.PUBLISHERS, configuration.publishers, listener));
		}

		if (configuration.subscribers > 0) {
			clients.putAll(createClients(Type.SUBSCRIBERS, configuration.subscribers, listener));
		}

		return clients;
	}

	private Map<? extends Type, ? extends List<MqttClient>> createClients(Type type, int numClients, AsyncClientListener listener) {
		Map<Type, List<MqttClient>> clients = new EnumMap<Type, List<MqttClient>>(Type.class);
		List<MqttClient> mqttClients = new ArrayList<MqttClient>(numClients);
		for (int i = 0; i < numClients; i++) {
			mqttClients.add(createClient(listener));
		}
		clients.put(type, mqttClients);

		return clients;
	}

	private MqttClient createClient(AsyncClientListener listener) {
		if (async) {
			return factory.newAsyncClient(listener);
		}

		return factory.newSynchronousClient(listener);
	}

	private void doConnect(Map<Type, List<MqttClient>> clients) {
		List<MqttClient> clientList = clients.get(Type.PUBLISHERS);
		if (clientList != null) {
			int idSuffix = 0;
			for (MqttClient client : clientList) {
				String clientId = createClientId(Type.PUBLISHERS, configuration.clusteredPublisher, configuration.clientId, idSuffix++);
				if (configuration.username != null) {
					client.connect(clientId, configuration.cleanSession, configuration.username, configuration.password);
				} else {
					client.connect(clientId, configuration.cleanSession);
				}
			}
		}

		clientList = clients.get(Type.SUBSCRIBERS);
		if (clientList != null) {
			int idSuffix = 0;
			for (MqttClient client : clientList) {
				String clientId = createClientId(Type.SUBSCRIBERS, configuration.clusteredSubscriber, configuration.clientId, idSuffix++);
				if (configuration.username != null) {
					client.connect(clientId, configuration.cleanSession, configuration.username, configuration.password);
				} else {
					client.connect(clientId, configuration.cleanSession);
				}
			}
		}

		stageControl.awaitConnect();
	}

	private String createClientId(Type type, boolean clustered, String clientId, int idSuffix) {
		String base = clientId + type.getName();
		if (!clustered) {
			return base + idSuffix;
		}

		return base;
	}

	private void shutdown(Map<Type, List<MqttClient>> clients) {
		statsReporterThread.interrupt();
		try {
			statsReporterThread.join(1000);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}

		reportStats();

		if (configuration.unsubscribeAtEnd && configuration.topicToSubscribeTo != null && configuration.messagesToReceive > 0) {
			for (MqttClient client : clients.get(Type.SUBSCRIBERS)) {
				client.unsubscribe(new String[] { configuration.topicToSubscribeTo });
			}
		}

		disconnect(clients);
	}

	private void reportStats() {
		String jarDirectory = XenqttUtil.getXenqttInstallDirectory().getAbsolutePath();
		File statsReportFile = new File(jarDirectory, "xenqtt-testresults.txt");
		try {
			PrintWriter writer = new PrintWriter(statsReportFile);
			writer.write("======================================== Test Stats Report ========================================\n");
			writer.write("\n----------------------------------------   General Stats   ----------------------------------------\n");
			writer.write(String.format("Test start:  %s\n", stats.getTestStart()));
			writer.write(String.format("Test end:    %s\n", stats.getTestEnd()));
			writer.write(String.format("Client type: %s\n", stats.getClientType()));
			writer.write(String.format("Test duration: %.2f seconds\n", stats.getTestDurationSeconds()));
			writer.write("\n----------------------------------------   Publish Stats   ----------------------------------------\n");
			writer.write(String.format("Messages Published:              %d\n", stats.getNumMessagesPublished()));
			writer.write(String.format("Average Publish Time:            %.2f\n", stats.getAveragePublishDuration()));
			writer.write(String.format("Publish Throughput (Per-Second): %.2f\n", stats.getPublishThroughput()));
			writer.write(String.format("Publish Gaps: %s\n", getGapReport(stats.getPublishMessageGaps())));
			writer.write("\n----------------------------------------  Subscribe Stats  ----------------------------------------\n");
			writer.write(String.format("Messages Received:                %d\n", stats.getMessagesReceived()));
			writer.write(String.format("Duplicates Received:              %d\n", stats.getDuplicates()));
			writer.write(String.format("Message Latency:                  %s\n", String.format("%.2f", stats.getAverageMessageLatency())));
			writer.write(String.format("Received Throughput (Per-Second): %.2f\n", stats.getMessagesReceivedThroughput()));
			writer.write(String.format("Receive Gaps: %s\n", getGapReport(stats.getReceivedMessageGaps())));
			writer.write("\n===================================================================================================\n");
			writer.close();
		} catch (Exception ex) {
			Log.error(ex, "Unable to write out the stats report.");
		}
	}

	private Object getGapReport(List<Gap> gaps) {
		if (gaps.isEmpty()) {
			return "None";
		}

		StringBuilder gapsReport = new StringBuilder();
		for (Gap gap : gaps) {
			gapsReport.append(String.format("%s, ", gap));
		}

		return gapsReport.toString().substring(0, gapsReport.length() - 2);
	}

	private void disconnect(Map<Type, List<MqttClient>> clients) {
		for (List<MqttClient> clientList : clients.values()) {
			for (MqttClient client : clientList) {
				client.disconnect();
			}
		}
	}

	private static enum Type {

		PUBLISHERS("Pub"), SUBSCRIBERS("Sub");

		private final String name;

		private Type(String name) {
			this.name = name;
		}

		private String getName() {
			return name;
		}

	}

	private static final class StatsReporter extends Thread {

		private final XenqttTestClientStats stats;

		private StatsReporter(XenqttTestClientStats stats) {
			super("StatsReporter");
			this.stats = stats;

			setDaemon(true);
		}

		@Override
		public void run() {
			int shouldReportHeader = 0;
			DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
			for (;;) {
				try {
					Thread.sleep(5000);
					reportStats(shouldReportHeader, format);
					shouldReportHeader--;
					if (shouldReportHeader < 0) {
						shouldReportHeader = 30;
					}
				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
					reportStats(shouldReportHeader, format);
					break;
				}
			}
		}

		private void reportStats(int shouldReportHeader, DateFormat format) {
			if (shouldReportHeader == 0) {
				System.out.printf("%-26s%-20s%-20s%-20s%3s%-20s%-20s%-20s%-20s\n", "", "Published", "Duration (Millis)", "Throughput (Per-Sec)", " | ",
						"Received", "Duplicates", "Latency (Millis)", "Throughput (Per-Sec)");
			}

			System.out.printf("%-26s%-20d%-20.2f%-20.2f%3s%-20d%-20d%-20.2f%-20.2f\n", format.format(new Date()), stats.getNumMessagesPublished(),
					stats.getAveragePublishDuration(), stats.getPublishThroughput(), " | ", stats.getMessagesReceived(), stats.getDuplicates(),
					stats.getAverageMessageLatency(), stats.getMessagesReceivedThroughput());
		}

	}

	private static final class Publisher extends Thread {

		private final MqttClient client;
		private final boolean async;
		private final Semaphore inFlight;
		private final String publishTopic;
		private final int messageSize;
		private final int messagesToPublish;
		private final AtomicInteger messageIds;
		private final QoS qos;
		private final XenqttTestClientStats stats;
		private final StageControl stageControl;

		private Publisher(int id, MqttClient client, boolean async, Semaphore inFlight, String publishTopic, int messageSize, int messagesToPublish,
				AtomicInteger messageIds, QoS qos, XenqttTestClientStats stats, StageControl stageControl) {
			super("Publisher-" + id);
			this.client = client;
			this.async = async;
			this.inFlight = inFlight;
			this.publishTopic = publishTopic;
			this.messageSize = messageSize;
			this.messagesToPublish = messagesToPublish;
			this.messageIds = messageIds;
			this.qos = qos;
			this.stats = stats;
			this.stageControl = stageControl;
		}

		@Override
		public void run() {
			boolean syncOrQosZero = !async || qos == QoS.AT_MOST_ONCE;
			int messagesRemaining = messagesToPublish;
			while (messagesRemaining > 0) {
				try {
					inFlight.acquire();

					PublishMessage message = new PublishMessage(publishTopic, qos, createPayload());
					client.publish(message);
					messagesRemaining--;
					if (syncOrQosZero) {
						inFlight.release();
						stats.publishComplete(message);
					}
				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
					Log.error(ex, "Thread was interrupted. Exiting...");
					return;
				} catch (Exception ex) {
					Log.error(ex, "Unable to publish a message.");
				} finally {
					if (syncOrQosZero) {
						stageControl.messagePublished();
					}
				}
			}
		}

		private byte[] createPayload() {
			int size = 12 + messageSize;
			byte[] payload = new byte[size];

			long now = System.currentTimeMillis();
			payload[0] = (byte) ((now & 0xff00000000000000L) >> 56);
			payload[1] = (byte) ((now & 0x00ff000000000000L) >> 48);
			payload[2] = (byte) ((now & 0x0000ff0000000000L) >> 40);
			payload[3] = (byte) ((now & 0x000000ff00000000L) >> 32);
			payload[4] = (byte) ((now & 0x00000000ff000000L) >> 24);
			payload[5] = (byte) ((now & 0x0000000000ff0000L) >> 16);
			payload[6] = (byte) ((now & 0x000000000000ff00L) >> 8);
			payload[7] = (byte) (now & 0x00000000000000ffL);

			int id = messageIds.getAndIncrement();
			payload[8] = (byte) ((id & 0xff000000) >> 24);
			payload[9] = (byte) ((id & 0x00ff0000) >> 16);
			payload[10] = (byte) ((id & 0x0000ff00) >> 8);
			payload[11] = (byte) (id & 0x000000ff);

			for (int i = 12; i < size; i++) {
				payload[i] = (byte) ((i - 12) & 0xff);
			}

			return payload;
		}

	}

}
