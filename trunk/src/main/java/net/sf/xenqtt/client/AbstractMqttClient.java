package net.sf.xenqtt.client;

import java.net.ConnectException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.xenqtt.Log;
import net.sf.xenqtt.MqttCommandCancelledException;
import net.sf.xenqtt.MqttInterruptedException;
import net.sf.xenqtt.MqttQosNotGrantedException;
import net.sf.xenqtt.MqttTimeoutException;
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

	private final String brokerUri;
	private final ChannelManager manager;

	private final ReconnectionStrategy reconnectionStrategy;

	private final Executor executor;
	private final ExecutorService executorService;
	private final ScheduledExecutorService reconnectionExecutor;

	private final MessageHandler messageHandler;
	private final MqttClientListener mqttClientListener;
	private final AsyncClientListener asyncClientListener;

	private final Map<Integer, Object> dataByMessageId;
	private final AtomicInteger messageIdGenerator = new AtomicInteger();

	private volatile MqttChannelRef channel;
	private volatile ConnectMessage connectMessage;
	private volatile boolean firstConnectPending = true;
	private volatile List<MqttMessage> unsentMessages;

	// FIXME [jim] - add constructors that take host/port and ctors that take URI
	/**
	 * Constructs a synchronous instance of this class using an {@link Executor} owned by this class.
	 * 
	 * @param brokerUri
	 *            The URL to the broker to connect to. For example, tcp://q.m2m.io:1883
	 * @param mqttClientListener
	 *            Handles events from this client's channel
	 * @param reconnectionStrategy
	 *            The algorithm used to reconnect to the broker if the connection is lost
	 * @param messageHandlerThreadPoolSize
	 *            The number of threads used to handle incoming messages and invoke the {@link AsyncClientListener listener's} methods
	 * @param messageResendIntervalSeconds
	 *            Seconds between attempts to resend a message that is {@link MqttMessage#isAckable()}. 0 to disable message resends
	 * @param blockingTimeoutSeconds
	 *            Seconds until a blocked method invocation times out and an {@link MqttTimeoutException} is thrown. -1 will create a non-blocking API, 0 will
	 *            create a blocking API with no timeout, > 0 will create a blocking API with the specified timeout.
	 */
	AbstractMqttClient(String brokerUri, MqttClientListener mqttClientListener, ReconnectionStrategy reconnectionStrategy, int messageHandlerThreadPoolSize,
			int messageResendIntervalSeconds, int blockingTimeoutSeconds) {
		this(brokerUri, mqttClientListener, null, reconnectionStrategy, Executors.newFixedThreadPool(messageHandlerThreadPoolSize),
				messageResendIntervalSeconds, blockingTimeoutSeconds);
	}

	/**
	 * Constructs a synchronous instance of this class using a user provided {@link Executor}.
	 * 
	 * @param brokerUri
	 *            The URL to the broker to connect to. For example, tcp://q.m2m.io:1883
	 * @param mqttClientListener
	 *            Handles events from this client's channel
	 * @param reconnectionStrategy
	 *            The algorithm used to reconnect to the broker if the connection is lost
	 * @param executor
	 *            The executor used to handle incoming messages and invoke the {@link AsyncClientListener listener's} methods. This class will NOT shut down the
	 *            executor.
	 * @param messageResendIntervalSeconds
	 *            Seconds between attempts to resend a message that is {@link MqttMessage#isAckable()}. 0 to disable message resends
	 * @param blockingTimeoutSeconds
	 *            Seconds until a blocked method invocation times out and an {@link MqttTimeoutException} is thrown. -1 will create a non-blocking API, 0 will
	 *            create a blocking API with no timeout, > 0 will create a blocking API with the specified timeout.
	 */
	AbstractMqttClient(String brokerUri, MqttClientListener mqttClientListener, ReconnectionStrategy reconnectionStrategy, Executor executor,
			int messageResendIntervalSeconds, int blockingTimeoutSeconds) {
		this(brokerUri, mqttClientListener, null, reconnectionStrategy, executor, messageResendIntervalSeconds, blockingTimeoutSeconds);
	}

	/**
	 * Constructs an asynchronous instance of this class using an {@link Executor} owned by this class.
	 * 
	 * @param brokerUri
	 *            The URL to the broker to connect to. For example, tcp://q.m2m.io:1883
	 * @param AsyncClientListener
	 *            asyncClientListener Handles events from this client's channel
	 * @param reconnectionStrategy
	 *            The algorithm used to reconnect to the broker if the connection is lost
	 * @param messageHandlerThreadPoolSize
	 *            The number of threads used to handle incoming messages and invoke the {@link AsyncClientListener listener's} methods
	 * @param messageResendIntervalSeconds
	 *            Seconds between attempts to resend a message that is {@link MqttMessage#isAckable()}. 0 to disable message resends
	 */
	AbstractMqttClient(String brokerUri, AsyncClientListener asyncClientListener, ReconnectionStrategy reconnectionStrategy, int messageHandlerThreadPoolSize,
			int messageResendIntervalSeconds) {
		this(brokerUri, asyncClientListener, asyncClientListener, reconnectionStrategy, Executors.newFixedThreadPool(messageHandlerThreadPoolSize),
				messageResendIntervalSeconds, -1);
	}

	/**
	 * Constructs an asynchronous instance of this class using a user provided {@link Executor}.
	 * 
	 * @param brokerUri
	 *            The URL to the broker to connect to. For example, tcp://q.m2m.io:1883
	 * @param asyncClientListener
	 *            Handles events from this client's channel
	 * @param reconnectionStrategy
	 *            The algorithm used to reconnect to the broker if the connection is lost
	 * @param executor
	 *            The executor used to handle incoming messages and invoke the {@link AsyncClientListener listener's} methods. This class will NOT shut down the
	 *            executor.
	 * @param messageResendIntervalSeconds
	 *            Seconds between attempts to resend a message that is {@link MqttMessage#isAckable()}. 0 to disable message resends
	 */
	AbstractMqttClient(String brokerUri, AsyncClientListener asyncClientListener, ReconnectionStrategy reconnectionStrategy, Executor executor,
			int messageResendIntervalSeconds) {
		this(brokerUri, asyncClientListener, asyncClientListener, reconnectionStrategy, executor, messageResendIntervalSeconds, -1);
	}

	/**
	 * @see net.sf.xenqtt.client.MqttClient#connect(java.lang.String, boolean, int, java.lang.String, java.lang.String, java.lang.String, java.lang.String,
	 *      net.sf.xenqtt.message.QoS, boolean)
	 */
	@Override
	public final ConnectReturnCode connect(String clientId, boolean cleanSession, int keepAliveSeconds, String userName, String password, String willTopic,
			String willMessage, QoS willQos, boolean willRetain) throws MqttCommandCancelledException, MqttTimeoutException, MqttInterruptedException {

		ConnectMessage message = new ConnectMessage(clientId, cleanSession, keepAliveSeconds, userName, password, willTopic, willMessage, willQos, willRetain);
		return doConnect(message);
	}

	/**
	 * @see net.sf.xenqtt.client.MqttClient#connect(java.lang.String, boolean, int)
	 */
	@Override
	public final ConnectReturnCode connect(String clientId, boolean cleanSession, int keepAliveSeconds) throws MqttCommandCancelledException,
			MqttTimeoutException, MqttInterruptedException {

		ConnectMessage message = new ConnectMessage(clientId, cleanSession, keepAliveSeconds);
		return doConnect(message);
	}

	/**
	 * @see net.sf.xenqtt.client.MqttClient#connect(java.lang.String, boolean, int, java.lang.String, java.lang.String)
	 */
	@Override
	public final ConnectReturnCode connect(String clientId, boolean cleanSession, int keepAliveSeconds, String userName, String password)
			throws MqttCommandCancelledException, MqttTimeoutException, InterruptedException {

		ConnectMessage message = new ConnectMessage(clientId, cleanSession, keepAliveSeconds, userName, password);
		return doConnect(message);
	}

	/**
	 * @see net.sf.xenqtt.client.MqttClient#connect(java.lang.String, boolean, int, java.lang.String, java.lang.String, net.sf.xenqtt.message.QoS, boolean)
	 */
	@Override
	public final ConnectReturnCode connect(String clientId, boolean cleanSession, int keepAliveSeconds, String willTopic, String willMessage, QoS willQos,
			boolean willRetain) throws MqttTimeoutException, MqttInterruptedException {

		ConnectMessage message = new ConnectMessage(clientId, cleanSession, keepAliveSeconds, willTopic, willMessage, willQos, willRetain);
		return doConnect(message);
	}

	/**
	 * @see net.sf.xenqtt.client.MqttClient#disconnect()
	 */
	@Override
	public final void disconnect() throws MqttCommandCancelledException, MqttTimeoutException, MqttInterruptedException {

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

		// FIXME [jim] - test message not set at qos 0
		if (message.pubMessage.getQoSLevel() > 0) {
			int messageId = nextMessageId(message);
			message.pubMessage.setMessageId(messageId);
		}
		manager.send(channel, message.pubMessage);
	}

	/**
	 * @see net.sf.xenqtt.client.MqttClient#close()
	 */
	@Override
	public final void close() throws MqttCommandCancelledException, MqttTimeoutException, MqttInterruptedException {

		manager.close(channel);
	}

	/**
	 * Stops this client. Closes the connection to the broker if it is open. Blocks until shutdown is complete. Any other methods called after this have
	 * unpredictable results.
	 * 
	 * @throws MqttInterruptedException
	 *             If the thread is {@link Thread#interrupt() interrupted}
	 */
	public final void shutdown() throws MqttInterruptedException {

		this.manager.shutdown();

		reconnectionExecutor.shutdownNow();
		try {
			reconnectionExecutor.awaitTermination(1, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			throw new MqttInterruptedException(e);
		}

		if (executorService != null) {
			executorService.shutdownNow();
			try {
				executorService.awaitTermination(1, TimeUnit.DAYS);
			} catch (InterruptedException e) {
				throw new MqttInterruptedException(e);
			}
		}
	}

	/**
	 * Package visible and only for use by the {@link MqttClientFactory}
	 */
	AbstractMqttClient(String brokerUri, MqttClientListener mqttClientListener, AsyncClientListener asyncClientListener,
			ReconnectionStrategy reconnectionStrategy, Executor executor, ChannelManager manager, ScheduledExecutorService reconnectionExecutor) {
		this.brokerUri = brokerUri;
		this.mqttClientListener = mqttClientListener;
		this.asyncClientListener = asyncClientListener;
		this.executor = executor;
		this.reconnectionExecutor = reconnectionExecutor;
		this.executorService = null;
		this.messageHandler = new AsyncMessageHandler();
		this.reconnectionStrategy = reconnectionStrategy;
		this.manager = manager;
		this.dataByMessageId = asyncClientListener == null ? null : new ConcurrentHashMap<Integer, Object>();
		this.channel = manager.newClientChannel(brokerUri, messageHandler);
	}

	private AbstractMqttClient(String brokerUri, MqttClientListener mqttClientListener, AsyncClientListener asyncClientListener,
			ReconnectionStrategy reconnectionStrategy, Executor executor, int messageResendIntervalSeconds, int blockingTimeoutSeconds) {
		this.brokerUri = brokerUri;
		this.mqttClientListener = mqttClientListener;
		this.asyncClientListener = asyncClientListener;
		this.executor = executor;
		this.executorService = null;
		this.reconnectionExecutor = Executors.newSingleThreadScheduledExecutor();
		this.messageHandler = new AsyncMessageHandler();
		this.reconnectionStrategy = reconnectionStrategy != null ? reconnectionStrategy : new NullReconnectStrategy();
		this.dataByMessageId = asyncClientListener == null ? null : new ConcurrentHashMap<Integer, Object>();
		this.manager = new ChannelManagerImpl(messageResendIntervalSeconds, blockingTimeoutSeconds);
		this.manager.init();
		this.channel = manager.newClientChannel(brokerUri, messageHandler);
	}

	private AbstractMqttClient(String brokerUri, MqttClientListener mqttClientListener, AsyncClientListener asyncClientListener,
			ReconnectionStrategy reconnectionStrategy, ExecutorService executorService, int messageResendIntervalSeconds, int blockingTimeoutSeconds) {
		this.brokerUri = brokerUri;
		this.mqttClientListener = mqttClientListener;
		this.asyncClientListener = asyncClientListener;
		this.executor = executorService;
		this.executorService = executorService;
		this.reconnectionExecutor = Executors.newSingleThreadScheduledExecutor();
		this.messageHandler = new AsyncMessageHandler();
		this.reconnectionStrategy = reconnectionStrategy != null ? reconnectionStrategy : new NullReconnectStrategy();
		this.dataByMessageId = asyncClientListener == null ? null : new ConcurrentHashMap<Integer, Object>();
		this.manager = new ChannelManagerImpl(messageResendIntervalSeconds, blockingTimeoutSeconds);
		this.manager.init();
		this.channel = manager.newClientChannel(brokerUri, messageHandler);
	}

	private int nextMessageId(Object messageData) {

		// TODO [jim] - need to deal with what happens when we have an in-flight message that is already using a message ID that we come around to again. Is
		// this even realistic?
		int next = messageIdGenerator.incrementAndGet();
		if (next > 0xffff) {
			messageIdGenerator.compareAndSet(next, 0);
			return nextMessageId(messageData);
		}

		if (dataByMessageId != null) {
			dataByMessageId.put(next, messageData);
		}

		return next;
	}

	private ConnectReturnCode doConnect(ConnectMessage message) {

		connectMessage = message;
		MqttMessage ack = manager.send(channel, message);
		return ack == null ? null : ((ConnAckMessage) ack).getReturnCode();
	}

	private Subscription[] grantedSubscriptions(Subscription[] requestedSubscriptions, SubAckMessage ack) {

		boolean match = true;

		// TODO [jim] - what if the number of granted qoses does not match the number of requested subscriptions?
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

	// FIXME [jim] - what happens if the client sends messages after the channel closes but before the reconnect is done? I think they will get lost.
	private void tryReconnect(MqttChannel closedChannel, Throwable cause) {

		boolean reconnecting = false;

		// if channel is null then it didn't even finish construction so we definitely don't want to reconnect
		if (channel != null && cause != null && !(cause instanceof ConnectException)) {
			long reconnectDelay = reconnectionStrategy.connectionLost(this, cause);
			reconnecting = reconnectDelay >= 0;

			if (reconnecting) {
				unsentMessages = manager.getUnsentMessages(closedChannel);
				reconnectionExecutor.schedule(new ClientReconnector(closedChannel), reconnectDelay, TimeUnit.MILLISECONDS);
			}
		}
		if (!reconnecting) {
			manager.cancelBlockingCommands(closedChannel);
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
		public void connAck(final MqttChannel channel, final ConnAckMessage message) throws Exception {

			if (message.getReturnCode() == ConnectReturnCode.ACCEPTED) {

				executor.execute(new Runnable() {

					@Override
					public void run() {

						reconnectionStrategy.connectionEstablished();
						if (unsentMessages != null) {
							for (MqttMessage message : unsentMessages) {
								manager.send(channel, message);
							}
						}
					}
				});
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
							PublishMessage publishMessage = (PublishMessage) dataByMessageId.get(message.getMessageId());
							asyncClientListener.published(client, publishMessage);
						} catch (Exception e) {
							Log.error(e, "Failed to process message for %s: %s", channel, message);
						}

					}
				});
			}
		}

		/**
		 * @see net.sf.xenqtt.message.MessageHandler#pubRec(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.PubRecMessage)
		 */
		@Override
		public void pubRec(final MqttChannel channel, final PubRecMessage message) throws Exception {
			// TODO [jim] - qos2 not supported
		}

		/**
		 * @see net.sf.xenqtt.message.MessageHandler#pubRel(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.PubRelMessage)
		 */
		@Override
		public void pubRel(final MqttChannel channel, final PubRelMessage message) throws Exception {
			// TODO [jim] - qos2 not supported
		}

		/**
		 * @see net.sf.xenqtt.message.MessageHandler#pubComp(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.PubCompMessage)
		 */
		@Override
		public void pubComp(final MqttChannel channel, final PubCompMessage message) throws Exception {
			// TODO [jim] - qos2 not supported
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
							Subscription[] requestedSubscriptions = (Subscription[]) dataByMessageId.get(message.getMessageId());
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
							String[] topics = (String[]) dataByMessageId.get(message.getMessageId());
							asyncClientListener.unsubscribed(client, topics);
						} catch (Exception e) {
							Log.error(e, "Failed to process message for %s: %s", channel, message);
						}

					}
				});
			}
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
							doConnect(connectMessage);
						} catch (Exception e) {
							Log.error(e, "Failed to process channelOpened for %s: cause=", channel);
						}

					}
				});
			} else {
				firstConnectPending = false;
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
					try {
						tryReconnect(channel, cause);
					} catch (Exception e) {
						Log.error(e, "Failed to process channelClosed for %s: cause=", channel, cause);
					}

				}
			});
		}
	}

	private final class ClientReconnector implements Runnable {

		private final MqttChannel closedChannel;

		public ClientReconnector(MqttChannel closedChannel) {
			this.closedChannel = closedChannel;
		}

		/**
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {

			try {
				channel = manager.newClientChannel(brokerUri, messageHandler);
			} catch (Throwable t) {
				tryReconnect(closedChannel, t);
			}
		}
	}
}