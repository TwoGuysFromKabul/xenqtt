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
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.xenqtt.AppContext;
import net.sf.xenqtt.Log;
import net.sf.xenqtt.XenqttUtil;
import net.sf.xenqtt.client.AsyncMqttClient;
import net.sf.xenqtt.client.MqttClient;
import net.sf.xenqtt.client.MqttClientConfig;
import net.sf.xenqtt.client.Subscription;
import net.sf.xenqtt.client.SyncMqttClient;
import net.sf.xenqtt.test.XenqttTestClientStats.Gap;

/**
 * A test client that facilitates load and validation testing of the Xenqtt MQTT client.
 */
public final class XenqttTestClient {

	private final TestClientRunner runner;

	/**
	 * Create a new instance of this class.
	 * 
	 * @param context
	 *            The {@link AppContext context} that were passed in on the command-line
	 */
	public XenqttTestClient(AppContext context) {
		runner = new TestClientRunner(context);
	}

	/**
	 * Start the test client.
	 */
	public void start() {
		runner.start();
	}

	/**
	 * Stop the test client.
	 */
	public void stop() {
		runner.interrupt();
		try {
			runner.join(15000);
		} catch (Exception ex) {
			Log.error(ex, "Unable to wait for the test client runner to shutdown.");
		}

		if (runner.isAlive()) {
			Log.warn("Unable to cleanly shutdown the test client runner.");
		}
	}

	/**
	 * An enumeration that identifies the disparate MQTT client types available during the test. The desired client type is chosen via the configuration file
	 * and is a required property.
	 */
	static enum ClientType {

		/**
		 * Specifies usage of the {@link SyncMqttClient synchronous} MQTT client.
		 */
		SYNC("sync"),

		/**
		 * Specifies usage of the {@link AsyncMqttClient asynchronous} MQTT client.
		 */
		ASYNC("async");

		private final String type;

		private ClientType(String type) {
			this.type = type;
		}

		/**
		 * @return A textual representation of this {@link ClientType}
		 */
		String getType() {
			return type;
		}

		/**
		 * Get a {@link ClientType} instance based on a given textual representation.
		 * 
		 * @param type
		 *            The desired type
		 * 
		 * @return The {@link ClientType} that corresponds to the specified {@code type}
		 * 
		 * @throws IllegalArgumentException
		 *             If the specified {@code type} does not correspond to a known client type
		 */
		static ClientType getClientType(String type) {
			if (type == null) {
				throw new IllegalArgumentException("The client type cannot be null.");
			}

			for (ClientType clientType : values()) {
				if (clientType.type.equalsIgnoreCase(type)) {
					return clientType;
				}
			}

			throw new IllegalArgumentException(String.format("Unrecognized client type: %s", type));
		}

	}

	private static final class TestClientRunner extends Thread {

		private final AppContext context;
		private final TestClientConfiguration configuration;
		private final MqttClient client;

		private final ExecutorService publisherPool;
		private final CountDownLatch publisherCompleteLatch;
		private final CountDownLatch messageReceivedLatch;
		private final Semaphore inFlight;

		private final XenqttTestClientStats stats;
		private final Thread statsReporter;

		private TestClientRunner(AppContext context) {
			super("TestClientRunner");
			this.context = context;
			configuration = new TestClientConfiguration(context);
			stats = new XenqttTestClientStats(configuration.clientType);
			statsReporter = new Thread(new Runnable() {

				@Override
				public void run() {
					int shouldReportHeader = 0;
					DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
					for (;;) {
						try {
							Thread.sleep(5000);
							reportStats(shouldReportHeader, format);
							shouldReportHeader--;
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
						shouldReportHeader = 30;
					}

					System.out.printf("%-26s%-20d%-20.2f%-20.2f%3s%-20d%-20d%-20.2f%-20.2f\n", format.format(new Date()), stats.getNumMessagesPublished(),
							stats.getAveragePublishDuration(), stats.getPublishThroughput(), " | ", stats.getMessagesReceived(), stats.getDuplicates(),
							stats.getAverageMessageLatency(), stats.getMessagesReceivedThroughput());
				}

			});

			publisherCompleteLatch = configuration.publishers > 0 ? new CountDownLatch(configuration.publishers * configuration.messagesToPublish)
					: new CountDownLatch(0);
			messageReceivedLatch = configuration.messagesToReceive > 0 ? new CountDownLatch(configuration.messagesToReceive) : new CountDownLatch(0);
			inFlight = new Semaphore(configuration.maxInFlightMessages);
			client = createMqttClient();
			publisherPool = createPublisherPool(client);
		}

		private MqttClient createMqttClient() {
			TestClientAsyncClientListener listener = new TestClientAsyncClientListener(stats, publisherCompleteLatch, messageReceivedLatch, inFlight);
			MqttClientConfig config = getClientConfiguration();
			if (configuration.clientType == ClientType.SYNC) {
				config.setBlockingTimeoutSeconds(configuration.blockingTimeoutSeconds);
				return new SyncMqttClient(configuration.brokerUri, listener, configuration.messageHandlerThreadPoolSize, config);
			} else {
				return new AsyncMqttClient(configuration.brokerUri, listener, configuration.messageHandlerThreadPoolSize, config);
			}
		}

		private MqttClientConfig getClientConfiguration() {
			MqttClientConfig config = new MqttClientConfig();
			config.setConnectTimeoutSeconds(configuration.connectTimeoutSeconds);
			config.setMessageResendIntervalSeconds(configuration.messageResendIntervalSeconds);
			config.setReconnectionStrategy(configuration.reconnectionStrategy);

			return config;
		}

		private ExecutorService createPublisherPool(MqttClient client) {
			if (configuration.publishers < 1 || configuration.messagesToPublish < 1) {
				return null;
			}

			return Executors.newFixedThreadPool(configuration.publishers, new ConfigurableThreadFactory("Publisher", false));
		}

		@Override
		public void run() {
			try {
				System.out.println("Starting the Xenqtt Test Client");
				stats.testStarted();
				statsReporter.start();
				if (configuration.username != null) {
					client.connect(configuration.clientId, configuration.cleanSession, configuration.username, configuration.password);
				} else {
					client.connect(configuration.clientId, configuration.cleanSession);
				}

				if (publisherPool != null) {
					boolean async = client instanceof AsyncMqttClient;
					AtomicInteger ids = new AtomicInteger();
					for (int i = 0; i < configuration.publishers; i++) {
						Publisher publisher = getPublisher();
						publisherPool.execute(new PublishWorker(String.format("Publisher-%d", i), async, configuration.topicToPublishTo,
								configuration.messageSize, ids, configuration.qos, publisherCompleteLatch, stats, publisher, inFlight));
					}
				}

				if (configuration.topicToSubscribeTo != null && configuration.messagesToReceive > 0) {
					client.subscribe(new Subscription[] { new Subscription(configuration.topicToSubscribeTo, configuration.qos) });
				}

				publisherCompleteLatch.await();
				messageReceivedLatch.await();
				stats.testEnded();
				Log.info("Test complete. Commencing shutdown.");
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				Log.warn(ex, "The test client runner was interrupted. Shutting down.");
			} catch (Exception ex) {
				Log.error(ex, "Unable to start the test client.");
			}

			shutdown();
			System.out.println("Xenqtt Test Client Shutdown Complete");
			context.applicationDone();
		}

		private Publisher getPublisher() {
			boolean async = client instanceof AsyncMqttClient;
			if (configuration.isTimeBasedTest()) {
				return new TimeBasedPublisher(client, stats, configuration.duration, async);
			}

			return new FixedQuantityPublisher(client, configuration.messagesToPublish, stats, async);
		}

		private void shutdown() {
			statsReporter.interrupt();
			try {
				statsReporter.join(1000);
			} catch (InterruptedException ignore) {
			}

			reportStats();

			try {
				if (!publisherCompleteLatch.await(5, TimeUnit.SECONDS)) {
					Log.warn("Unable to cleanly shutdown all the publishers.");
				}
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}

			if (publisherPool != null) {
				publisherPool.shutdown();
				try {
					if (!publisherPool.awaitTermination(5, TimeUnit.SECONDS)) {
						Log.warn("Unable to cleanly shutdown the publisher pool.");
					}
				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
			}

			if (configuration.unsubscribeAtEnd && configuration.topicToSubscribeTo != null && configuration.messagesToReceive > 0) {
				client.unsubscribe(new String[] { configuration.topicToSubscribeTo });
			}

			client.disconnect();
		}

		private void reportStats() {
			String jarDirectory = XenqttUtil.getDirectoryHostingRunningXenqttJar();
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

	}

}
