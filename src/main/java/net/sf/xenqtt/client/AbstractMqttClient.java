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
package net.sf.xenqtt.client;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.xenqtt.ConfigurableThreadFactory;
import net.sf.xenqtt.Log;
import net.sf.xenqtt.MqttCommandCancelledException;
import net.sf.xenqtt.MqttInterruptedException;
import net.sf.xenqtt.MqttQosNotGrantedException;
import net.sf.xenqtt.MqttTimeoutException;
import net.sf.xenqtt.MqttTooManyMessagesInFlightException;
import net.sf.xenqtt.message.ChannelManager;
import net.sf.xenqtt.message.ChannelManagerImpl;
import net.sf.xenqtt.message.ConnAckMessage;
import net.sf.xenqtt.message.ConnectMessage;
import net.sf.xenqtt.message.ConnectReturnCode;
import net.sf.xenqtt.message.DisconnectMessage;
import net.sf.xenqtt.message.MessageHandler;
import net.sf.xenqtt.message.MqttChannel;
import net.sf.xenqtt.message.MqttChannelRef;
import net.sf.xenqtt.message.MqttMessage;
import net.sf.xenqtt.message.PubAckMessage;
import net.sf.xenqtt.message.PubCompMessage;
import net.sf.xenqtt.message.PubMessage;
import net.sf.xenqtt.message.PubRecMessage;
import net.sf.xenqtt.message.PubRelMessage;
import net.sf.xenqtt.message.QoS;
import net.sf.xenqtt.message.SubAckMessage;
import net.sf.xenqtt.message.SubscribeMessage;
import net.sf.xenqtt.message.UnsubAckMessage;
import net.sf.xenqtt.message.UnsubscribeMessage;

/**
 * Base class for both synchronous and asynchronous {@link MqttClient} implementations
 */
abstract class AbstractMqttClient implements MqttClient {

	private final boolean ownedByFactory;
	private final MqttClientConfig config;
	private final String brokerUri;
	private final ChannelManager manager;

	private final Executor executor;
	private final ExecutorService executorService;
	private final ScheduledExecutorService scheduledExecutor;

	private final MessageHandler messageHandler;
	private final MqttClientListener mqttClientListener;
	private final AsyncClientListener asyncClientListener;
	private final MqttClientDebugListener debugListener;

	private final ConcurrentHashMap<Integer, Object> dataByMessageId;
	private final AtomicInteger messageIdGenerator = new AtomicInteger();

	private volatile MqttChannelRef channel;
	private volatile MqttChannelRef newChannel;
	private volatile ConnectMessage connectMessage;
	private volatile boolean firstConnectPending = true;
	private volatile Future<?> connectTimeoutFuture;
	private volatile boolean closeRequested;
	private volatile boolean shuttingDown;

	/**
	 * Constructs a synchronous instance of this class using an {@link Executor} owned by this class.
	 * 
	 * @param brokerUri
	 *            The URL to the broker to connect to. For example, tcp://q.m2m.io:1883
	 * @param mqttClientListener
	 *            Handles events from this client's channel
	 * @param messageHandlerThreadPoolSize
	 *            The number of threads used to handle incoming messages and invoke the {@link AsyncClientListener listener's} methods
	 * @param config
	 *            The configuration for the client
	 */
	AbstractMqttClient(String brokerUri, MqttClientListener mqttClientListener, int messageHandlerThreadPoolSize, MqttClientConfig config) {
		this(brokerUri, mqttClientListener, null, messageHandlerThreadPoolSize, null, config);
	}

	/**
	 * Constructs a synchronous instance of this class using a user provided {@link Executor}.
	 * 
	 * @param brokerUri
	 *            The URL to the broker to connect to. For example, tcp://q.m2m.io:1883
	 * @param mqttClientListener
	 *            Handles events from this client's channel
	 * @param executor
	 *            The executor used to handle incoming messages and invoke the {@link AsyncClientListener listener's} methods. This class will NOT shut down the
	 *            executor.
	 * @param config
	 *            The configuration for the client
	 */
	AbstractMqttClient(String brokerUri, MqttClientListener mqttClientListener, Executor executor, MqttClientConfig config) {
		this(brokerUri, mqttClientListener, null, 0, executor, config);
	}

	/**
	 * Constructs an asynchronous instance of this class using an {@link Executor} owned by this class.
	 * 
	 * @param brokerUri
	 *            The URL to the broker to connect to. For example, tcp://q.m2m.io:1883
	 * @param AsyncClientListener
	 *            asyncClientListener Handles events from this client's channel
	 * @param messageHandlerThreadPoolSize
	 *            The number of threads used to handle incoming messages and invoke the {@link AsyncClientListener listener's} methods
	 * @param config
	 *            The configuration for the client
	 */
	AbstractMqttClient(String brokerUri, AsyncClientListener asyncClientListener, int messageHandlerThreadPoolSize, MqttClientConfig config) {
		this(brokerUri, asyncClientListener, asyncClientListener, messageHandlerThreadPoolSize, null, config);
	}

	/**
	 * Constructs an asynchronous instance of this class using a user provided {@link Executor}.
	 * 
	 * @param brokerUri
	 *            The URL to the broker to connect to. For example, tcp://q.m2m.io:1883
	 * @param asyncClientListener
	 *            Handles events from this client's channel
	 * @param executor
	 *            The executor used to handle incoming messages and invoke the {@link AsyncClientListener listener's} methods. This class will NOT shut down the
	 *            executor.
	 * @param config
	 *            The configuration for the client
	 */
	AbstractMqttClient(String brokerUri, AsyncClientListener asyncClientListener, Executor executor, MqttClientConfig config) {
		this(brokerUri, asyncClientListener, asyncClientListener, 0, executor, config);
	}

	/**
	 * @see net.sf.xenqtt.client.MqttClient#connect(java.lang.String, boolean, java.lang.String, java.lang.String, java.lang.String, java.lang.String,
	 *      net.sf.xenqtt.message.QoS, boolean)
	 */
	@Override
	public final ConnectReturnCode connect(String clientId, boolean cleanSession, String userName, String password, String willTopic, String willMessage,
			QoS willQos, boolean willRetain) throws MqttCommandCancelledException, MqttTimeoutException, MqttInterruptedException {

		ConnectMessage message = new ConnectMessage(clientId, cleanSession, config.getKeepAliveSeconds(), userName, password, willTopic, willMessage, willQos,
				willRetain);
		return doConnect(channel, message);
	}

	/**
	 * @see net.sf.xenqtt.client.MqttClient#connect(java.lang.String, boolean)
	 */
	@Override
	public final ConnectReturnCode connect(String clientId, boolean cleanSession) throws MqttCommandCancelledException, MqttTimeoutException,
			MqttInterruptedException {

		ConnectMessage message = new ConnectMessage(clientId, cleanSession, config.getKeepAliveSeconds());
		return doConnect(channel, message);
	}

	/**
	 * @see net.sf.xenqtt.client.MqttClient#connect(java.lang.String, boolean, java.lang.String, java.lang.String)
	 */
	@Override
	public final ConnectReturnCode connect(String clientId, boolean cleanSession, String userName, String password) throws MqttCommandCancelledException,
			MqttTimeoutException, MqttInterruptedException {

		ConnectMessage message = new ConnectMessage(clientId, cleanSession, config.getKeepAliveSeconds(), userName, password);
		return doConnect(channel, message);
	}

	/**
	 * @see net.sf.xenqtt.client.MqttClient#connect(java.lang.String, boolean, java.lang.String, java.lang.String, net.sf.xenqtt.message.QoS, boolean)
	 */
	@Override
	public final ConnectReturnCode connect(String clientId, boolean cleanSession, String willTopic, String willMessage, QoS willQos, boolean willRetain)
			throws MqttTimeoutException, MqttInterruptedException {

		ConnectMessage message = new ConnectMessage(clientId, cleanSession, config.getKeepAliveSeconds(), willTopic, willMessage, willQos, willRetain);
		return doConnect(channel, message);
	}

	/**
	 * @see net.sf.xenqtt.client.MqttClient#disconnect()
	 */
	@Override
	public final void disconnect() throws MqttCommandCancelledException, MqttTimeoutException, MqttInterruptedException {

		closeRequested = true;
		DisconnectMessage message = new DisconnectMessage();
		manager.send(channel, message);
	}

	/**
	 * @see net.sf.xenqtt.client.MqttClient#subscribe(net.sf.xenqtt.client.Subscription[])
	 */
	@Override
	public final Subscription[] subscribe(Subscription[] subscriptions) throws MqttCommandCancelledException, MqttTimeoutException, MqttInterruptedException {

		String[] topics = new String[subscriptions.length];
		QoS[] requestedQoses = new QoS[subscriptions.length];
		for (int i = 0; i < subscriptions.length; i++) {
			topics[i] = subscriptions[i].getTopic();
			requestedQoses[i] = subscriptions[i].getQos();
		}

		int messageId = nextMessageId(subscriptions);

		SubscribeMessage message = new SubscribeMessage(messageId, topics, requestedQoses);
		SubAckMessage ack = manager.send(channel, message);

		return ack == null ? null : grantedSubscriptions(subscriptions, ack);
	}

	/**
	 * @see net.sf.xenqtt.client.MqttClient#subscribe(java.util.List)
	 */
	@Override
	public final List<Subscription> subscribe(List<Subscription> subscriptions) throws MqttCommandCancelledException, MqttTimeoutException,
			MqttInterruptedException {

		Subscription[] array = subscribe(subscriptions.toArray(new Subscription[subscriptions.size()]));
		return array == null ? null : Arrays.asList(array);
	}

	/**
	 * @see net.sf.xenqtt.client.MqttClient#unsubscribe(java.lang.String[])
	 */
	@Override
	public final void unsubscribe(String[] topics) throws MqttCommandCancelledException, MqttTimeoutException, MqttInterruptedException {

		int messageId = nextMessageId(topics);
		UnsubscribeMessage message = new UnsubscribeMessage(messageId, topics);
		manager.send(channel, message);
	}

	/**
	 * @see net.sf.xenqtt.client.MqttClient#unsubscribe(java.util.List)
	 */
	@Override
	public final void unsubscribe(List<String> topics) throws MqttCommandCancelledException, MqttTimeoutException, MqttInterruptedException {

		unsubscribe(topics.toArray(new String[topics.size()]));
	}

	/**
	 * @see net.sf.xenqtt.client.MqttClient#publish(net.sf.xenqtt.client.PublishMessage)
	 */
	@Override
	public final void publish(PublishMessage message) throws MqttCommandCancelledException, MqttTimeoutException, MqttInterruptedException {

		PubMessage pubMessage = message.getPubMessage();
		if (pubMessage.getQoSLevel() > 0) {
			int messageId = nextMessageId(message);
			pubMessage.setMessageId(messageId);
		}
		manager.send(channel, pubMessage);
	}

	/**
	 * @see net.sf.xenqtt.client.MqttClient#close()
	 */
	@Override
	public final void close() throws MqttTimeoutException, MqttInterruptedException {

		closeRequested = true;
		manager.close(channel);
	}

	/**
	 * @see net.sf.xenqtt.client.MqttClient#isClosed()
	 */
	@Override
	public final boolean isClosed() {
		return scheduledExecutor.isShutdown();
	}

	/**
	 * @see net.sf.xenqtt.client.MqttClient#getStats(boolean)
	 */
	@Override
	public MessageStats getStats(boolean reset) {
		return manager.getStats(reset);
	}

	/**
	 * Package visible and only for use by the {@link MqttClientFactory}
	 */
	AbstractMqttClient(String brokerUri, MqttClientListener mqttClientListener, AsyncClientListener asyncClientListener, Executor executor,
			ChannelManager manager, ScheduledExecutorService scheduledExecutor, MqttClientConfig config) {
		this.ownedByFactory = true;
		this.brokerUri = brokerUri;
		this.config = config.clone();
		this.mqttClientListener = mqttClientListener;
		this.asyncClientListener = asyncClientListener;
		this.debugListener = config.getClientDebugListener();
		this.executor = executor;
		this.scheduledExecutor = scheduledExecutor;
		this.executorService = null;
		this.messageHandler = new AsyncMessageHandler();
		this.manager = manager;
		this.dataByMessageId = asyncClientListener == null ? null : new ConcurrentHashMap<Integer, Object>();
		this.channel = manager.newClientChannel(brokerUri, messageHandler);
	}

	private final void shutdown() throws MqttInterruptedException {

		shuttingDown = true;

		if (!ownedByFactory) {
			manager.shutdown();

			scheduledExecutor.shutdownNow();
			try {
				scheduledExecutor.awaitTermination(1, TimeUnit.DAYS);
			} catch (InterruptedException e) {
				throw new MqttInterruptedException(e);
			}

			if (executorService != null) {
				executorService.shutdown();
			}
		}
	}

	private AbstractMqttClient(String brokerUri, MqttClientListener mqttClientListener, AsyncClientListener asyncClientListener,
			int messageHandlerThreadPoolSize, Executor executor, MqttClientConfig config) {
		this.ownedByFactory = false;
		this.brokerUri = brokerUri;
		this.config = config.clone();
		this.mqttClientListener = mqttClientListener;
		this.asyncClientListener = asyncClientListener;
		this.debugListener = config.getClientDebugListener();
		this.executorService = executor == null ? Executors
				.newFixedThreadPool(messageHandlerThreadPoolSize, new ConfigurableThreadFactory("MqttClient", false)) : null;
		this.executor = executor == null ? executorService : executor;
		this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
		this.messageHandler = new AsyncMessageHandler();
		this.dataByMessageId = asyncClientListener == null ? null : new ConcurrentHashMap<Integer, Object>();
		int blockingTimeoutSeconds = asyncClientListener == null ? config.getBlockingTimeoutSeconds() : -1;
		this.manager = new ChannelManagerImpl(config.getMessageResendIntervalSeconds(), blockingTimeoutSeconds);
		this.manager.init();
		this.channel = manager.newClientChannel(brokerUri, messageHandler);
	}

	private int nextMessageId(Object messageData) {

		for (;;) {
			int next = messageIdGenerator.incrementAndGet();
			if (next > 0xffff) {
				messageIdGenerator.compareAndSet(next, 0);
				return nextMessageId(messageData);
			}

			if (dataByMessageId != null) {
				if (dataByMessageId.size() >= config.getMaxInFlightMessages()) {
					throw new MqttTooManyMessagesInFlightException();
				}
				if (dataByMessageId.putIfAbsent(next, messageData) != null) {
					continue;
				}
			}

			return next;
		}
	}

	private ConnectReturnCode doConnect(MqttChannelRef channel, ConnectMessage message) {

		connectMessage = message;
		if (config.getConnectTimeoutMillis() > 0) {
			connectTimeoutFuture = scheduledExecutor.schedule(new ConnectTimeout(), config.getConnectTimeoutMillis(), TimeUnit.MILLISECONDS);
		}
		MqttMessage ack = manager.send(channel, message);
		return ack == null ? null : ((ConnAckMessage) ack).getReturnCode();
	}

	private Subscription[] grantedSubscriptions(Subscription[] requestedSubscriptions, SubAckMessage ack) {

		boolean match = true;

		QoS[] grantedQoses = ack.getGrantedQoses();
		Subscription[] grantedSubscriptions = new Subscription[requestedSubscriptions.length];
		for (int i = 0; i < requestedSubscriptions.length; i++) {
			grantedSubscriptions[i] = new Subscription(requestedSubscriptions[i].getTopic(), grantedQoses[i]);
			if (requestedSubscriptions[i].getQos() != grantedQoses[i]) {
				match = false;
			}
		}

		if (!match) {
			throw new MqttQosNotGrantedException(grantedSubscriptions);
		}

		return grantedSubscriptions;
	}

	private void tryReconnect(Throwable cause) {

		boolean reconnecting = false;

		// if channel is null then it didn't even finish construction so we definitely don't want to reconnect
		if (channel != null && !shuttingDown) {
			if (!closeRequested) {

				if (firstConnectPending) {
					Log.error(cause, "First attempt to connect to broker failed; not scheduling a reconnect attempt for channel: %s", channel);
				} else {
					long reconnectDelay = config.getReconnectionStrategy().connectionLost(this, cause);
					reconnecting = reconnectDelay > 0;

					if (reconnecting) {
						Log.warn("Connection to broker lost; scheduling a reconnect attempt for channel: %s", channel);
						scheduledExecutor.schedule(new ClientReconnector(), reconnectDelay, TimeUnit.MILLISECONDS);
					} else {
						Log.warn("Connection to broker lost; not scheduling a reconnect attempt for channel: %s", channel);
					}
				}
			}
			if (!reconnecting) {
				manager.cancelBlockingCommands(channel);
			}
		}

		if (!reconnecting) {
			shutdown();
		}

		mqttClientListener.disconnected(this, cause, reconnecting);
	}

	private final class AsyncMessageHandler implements MessageHandler {

		private final MqttClient client = AbstractMqttClient.this;

		/**
		 * @see net.sf.xenqtt.message.MessageHandler#connect(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.ConnectMessage)
		 */
		@Override
		public void connect(final MqttChannel channel, final ConnectMessage message) throws Exception {
			// this should never be received by a client
		}

		/**
		 * @see net.sf.xenqtt.message.MessageHandler#connAck(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.ConnAckMessage)
		 */
		@Override
		public void connAck(final MqttChannel chan, final ConnAckMessage message) throws Exception {

			if (connectTimeoutFuture != null) {
				connectTimeoutFuture.cancel(false);
			}

			if (message.getReturnCode() == ConnectReturnCode.ACCEPTED) {

				executor.execute(new Runnable() {

					@Override
					public void run() {

						config.getReconnectionStrategy().connectionEstablished();
						if (newChannel != null) {
							manager.transfer(AbstractMqttClient.this.channel, newChannel);
							AbstractMqttClient.this.channel = newChannel;
							newChannel = null;
						}
					}
				});
			} else {
				closeRequested = true;
			}

			if (asyncClientListener != null) {
				executor.execute(new Runnable() {
					@Override
					public void run() {
						try {
							asyncClientListener.connected(client, message.getReturnCode());
						} catch (Exception e) {
							Log.error(e, "Failed to process message for %s: %s", channel, message);
						}

					}
				});
			}

			debugMessageReceivedIfApplicable(chan, message);
		}

		/**
		 * @see net.sf.xenqtt.message.MessageHandler#publish(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.PubMessage)
		 */
		@Override
		public void publish(final MqttChannel channel, final PubMessage message) throws Exception {
			executor.execute(new Runnable() {
				@Override
				public void run() {
					try {
						mqttClientListener.publishReceived(client, new PublishMessage(manager, channel, message));
					} catch (Exception e) {
						Log.error(e, "Failed to process message for %s: %s", channel, message);
					}

				}
			});

			debugMessageReceivedIfApplicable(channel, message);
		}

		/**
		 * @see net.sf.xenqtt.message.MessageHandler#pubAck(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.PubAckMessage)
		 */
		@Override
		public void pubAck(final MqttChannel channel, final PubAckMessage message) throws Exception {

			if (asyncClientListener != null) {
				executor.execute(new Runnable() {
					@Override
					public void run() {
						try {
							PublishMessage publishMessage = (PublishMessage) dataByMessageId.remove(message.getMessageId());
							asyncClientListener.published(client, publishMessage);
						} catch (Exception e) {
							Log.error(e, "Failed to process message for %s: %s", channel, message);
						}

					}
				});
			}

			debugMessageReceivedIfApplicable(channel, message);
		}

		/**
		 * @see net.sf.xenqtt.message.MessageHandler#pubRec(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.PubRecMessage)
		 */
		@Override
		public void pubRec(final MqttChannel channel, final PubRecMessage message) throws Exception {
			// TODO qos2 not supported
		}

		/**
		 * @see net.sf.xenqtt.message.MessageHandler#pubRel(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.PubRelMessage)
		 */
		@Override
		public void pubRel(final MqttChannel channel, final PubRelMessage message) throws Exception {
			// TODO qos2 not supported
		}

		/**
		 * @see net.sf.xenqtt.message.MessageHandler#pubComp(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.PubCompMessage)
		 */
		@Override
		public void pubComp(final MqttChannel channel, final PubCompMessage message) throws Exception {
			// TODO qos2 not supported
		}

		/**
		 * @see net.sf.xenqtt.message.MessageHandler#subscribe(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.SubscribeMessage)
		 */
		@Override
		public void subscribe(final MqttChannel channel, final SubscribeMessage message) throws Exception {
			// this should never be received by a client
		}

		/**
		 * @see net.sf.xenqtt.message.MessageHandler#subAck(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.SubAckMessage)
		 */
		@Override
		public void subAck(final MqttChannel channel, final SubAckMessage message) throws Exception {
			if (asyncClientListener != null) {
				executor.execute(new Runnable() {
					@Override
					public void run() {
						try {
							Subscription[] requestedSubscriptions = (Subscription[]) dataByMessageId.remove(message.getMessageId());
							try {
								Subscription[] grantedSubscriptions = grantedSubscriptions(requestedSubscriptions, message);
								asyncClientListener.subscribed(client, requestedSubscriptions, grantedSubscriptions, true);
							} catch (MqttQosNotGrantedException e) {
								asyncClientListener.subscribed(client, requestedSubscriptions, e.getGrantedSubscriptions(), false);
							}
						} catch (Exception e) {
							Log.error(e, "Failed to process message for %s: %s", channel, message);
						}

					}
				});
			}

			debugMessageReceivedIfApplicable(channel, message);
		}

		/**
		 * @see net.sf.xenqtt.message.MessageHandler#unsubscribe(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.UnsubscribeMessage)
		 */
		@Override
		public void unsubscribe(final MqttChannel channel, final UnsubscribeMessage message) throws Exception {
			// this should never be received by a client
		}

		/**
		 * @see net.sf.xenqtt.message.MessageHandler#unsubAck(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.UnsubAckMessage)
		 */
		@Override
		public void unsubAck(final MqttChannel channel, final UnsubAckMessage message) throws Exception {

			if (asyncClientListener != null) {
				executor.execute(new Runnable() {
					@Override
					public void run() {
						try {
							String[] topics = (String[]) dataByMessageId.remove(message.getMessageId());
							asyncClientListener.unsubscribed(client, topics);
						} catch (Exception e) {
							Log.error(e, "Failed to process message for %s: %s", channel, message);
						}

					}
				});
			}

			debugMessageReceivedIfApplicable(channel, message);
		}

		/**
		 * @see net.sf.xenqtt.message.MessageHandler#disconnect(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.DisconnectMessage)
		 */
		@Override
		public void disconnect(final MqttChannel channel, final DisconnectMessage message) throws Exception {
			// this should never be received by a client
		}

		/**
		 * @see net.sf.xenqtt.message.MessageHandler#channelOpened(net.sf.xenqtt.message.MqttChannel)
		 */
		@Override
		public void channelOpened(final MqttChannel channel) {

			if (!firstConnectPending && connectMessage != null) {
				executor.execute(new Runnable() {

					@Override
					public void run() {
						try {
							doConnect(channel, connectMessage);
						} catch (Exception e) {
							Log.error(e, "Failed to process channelOpened for %s: cause=", channel);
						}
					}
				});
			} else {
				firstConnectPending = false;
			}

			if (debugListener != null) {
				executor.execute(new Runnable() {
					@Override
					public void run() {
						try {
							debugListener.connectionOpened(client, channel.getLocalAddress(), channel.getRemoteAddress());
						} catch (Exception ex) {
							Log.error(ex, "Failed to update the debug listener for %s", channel);
						}
					}
				});
			}
		}

		/**
		 * @see net.sf.xenqtt.message.MessageHandler#channelClosed(net.sf.xenqtt.message.MqttChannel, java.lang.Throwable)
		 */
		@Override
		public void channelClosed(final MqttChannel channel, final Throwable cause) {

			executor.execute(new Runnable() {

				@Override
				public void run() {
					if (debugListener != null) {
						try {
							debugListener.connectionClosed(client, channel.getLocalAddress(), channel.getRemoteAddress());
						} catch (Exception ex) {
							Log.error(ex, "Unable to notify the debug listener of channel closed.");
						}
					}

					try {
						tryReconnect(cause);
					} catch (Exception e) {
						Log.error(e, "Failed to process channelClosed for %s: cause=", channel, cause);
					}

				}
			});
		}

		/**
		 * @see net.sf.xenqtt.message.MessageHandler#channelAttached(net.sf.xenqtt.message.MqttChannel)
		 */
		@Override
		public void channelAttached(MqttChannel channel) {
			// this should never be called for a client
		}

		/**
		 * @see net.sf.xenqtt.message.MessageHandler#channelDetached(net.sf.xenqtt.message.MqttChannel)
		 */
		@Override
		public void channelDetached(MqttChannel channel) {
			// this should never be called for a client
		}

		/**
		 * @see net.sf.xenqtt.message.MessageHandler#messageSent(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.MqttMessage)
		 */
		@Override
		public void messageSent(final MqttChannel channel, final MqttMessage message) {
			if (debugListener != null) {
				executor.execute(new Runnable() {
					@Override
					public void run() {
						try {
							debugListener.messageSent(client, channel.getLocalAddress(), channel.getRemoteAddress(), message);
						} catch (Exception ex) {
							Log.error(ex, "Unable to debug a message that was sent.");
						}
					}
				});
			}
		}

		private void debugMessageReceivedIfApplicable(final MqttChannel channel, final MqttMessage message) {
			if (debugListener != null) {
				executor.execute(new Runnable() {
					@Override
					public void run() {
						try {
							debugListener.messageReceived(client, channel.getLocalAddress(), channel.getRemoteAddress(), message);
						} catch (Exception ex) {
							Log.error(ex, "Failed to provide a debug hook for %s: %s", channel, message);
						}
					}

				});
			}
		}
	}

	private final class ConnectTimeout implements Runnable {

		@Override
		public void run() {

			try {
				manager.close(channel, new MqttTimeoutException("Timed out waiting for a response from the broker to the connect message"));
			} catch (Throwable t) {
				Log.error(t, "Failed to close channel after connection timed out");
			}
		}
	}

	private final class ClientReconnector implements Runnable {

		@Override
		public void run() {

			try {
				newChannel = manager.newClientChannel(brokerUri, messageHandler);
			} catch (Throwable t) {
				tryReconnect(t);
			}
		}
	}
}