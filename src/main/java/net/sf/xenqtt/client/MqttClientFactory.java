package net.sf.xenqtt.client;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.sf.xenqtt.MqttInterruptedException;
import net.sf.xenqtt.MqttTimeoutException;
import net.sf.xenqtt.message.ChannelManager;
import net.sf.xenqtt.message.ChannelManagerImpl;
import net.sf.xenqtt.message.MqttMessage;

/**
 * Used to create multiple "sibling" {@link MqttClient clients} that share an {@link Executor}, broker URI, etc.
 */
public final class MqttClientFactory {

	private final boolean blocking;
	private final ChannelManager manager;
	private final ReconnectionStrategy reconnectionStrategy;
	private final Executor executor;
	private final ExecutorService executorService;
	private final ScheduledExecutorService reconnectionExecutor;
	private final String brokerUri;

	/**
	 * Constructs an object to create synchronous {@link MqttClient clients} using an {@link Executor} owned by this class.
	 * 
	 * @param brokerUri
	 *            The URL to the broker to connect to. For example, tcp://q.m2m.io:1883
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
	public MqttClientFactory(String brokerUri, ReconnectionStrategy reconnectionStrategy, int messageHandlerThreadPoolSize, int messageResendIntervalSeconds,
			int blockingTimeoutSeconds) {
		this(brokerUri, reconnectionStrategy, Executors.newFixedThreadPool(messageHandlerThreadPoolSize), messageResendIntervalSeconds, blockingTimeoutSeconds);
	}

	/**
	 * Constructs an object to create synchronous {@link MqttClient clients} using a user provided {@link Executor}.
	 * 
	 * @param brokerUri
	 *            The URL to the broker to connect to. For example, tcp://q.m2m.io:1883
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
	public MqttClientFactory(String brokerUri, ReconnectionStrategy reconnectionStrategy, Executor executor, int messageResendIntervalSeconds,
			int blockingTimeoutSeconds) {

		this.blocking = blockingTimeoutSeconds >= 0;
		this.brokerUri = brokerUri;
		this.executor = executor;
		this.executorService = null;
		this.reconnectionExecutor = Executors.newSingleThreadScheduledExecutor();
		this.reconnectionStrategy = reconnectionStrategy != null ? reconnectionStrategy : new NullReconnectStrategy();
		this.manager = new ChannelManagerImpl(messageResendIntervalSeconds, blockingTimeoutSeconds);
		this.manager.init();
	}

	/**
	 * Constructs an object to create asynchronous {@link MqttClient clients} using an {@link Executor} owned by this class.
	 * 
	 * @param brokerUri
	 *            The URL to the broker to connect to. For example, tcp://q.m2m.io:1883
	 * @param reconnectionStrategy
	 *            The algorithm used to reconnect to the broker if the connection is lost
	 * @param messageHandlerThreadPoolSize
	 *            The number of threads used to handle incoming messages and invoke the {@link AsyncClientListener listener's} methods
	 * @param messageResendIntervalSeconds
	 *            Seconds between attempts to resend a message that is {@link MqttMessage#isAckable()}. 0 to disable message resends
	 */
	public MqttClientFactory(String brokerUri, ReconnectionStrategy reconnectionStrategy, int messageHandlerThreadPoolSize, int messageResendIntervalSeconds) {
		this(brokerUri, reconnectionStrategy, Executors.newFixedThreadPool(messageHandlerThreadPoolSize), messageResendIntervalSeconds, -1);
	}

	/**
	 * Constructs an object to create asynchronous {@link MqttClient clients} using a user provided {@link Executor}.
	 * 
	 * @param brokerUri
	 *            The URL to the broker to connect to. For example, tcp://q.m2m.io:1883
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
	public MqttClientFactory(String brokerUri, ReconnectionStrategy reconnectionStrategy, Executor executor, int messageResendIntervalSeconds) {
		this(brokerUri, reconnectionStrategy, executor, messageResendIntervalSeconds, -1);
	}

	/**
	 * Stops this factory. Closes all open connections to the broker. Blocks until shutdown is complete. Any other methods called after this have unpredictable
	 * results.
	 */
	public void shutdown() {

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
	 * Creates a synchronous {@link MqttClient client}. You may only use this method if the factory was constructed to create synchronous clients.
	 * 
	 * @param publishListener
	 *            Handles events from this client's channel
	 * 
	 * @return A new synchronous {@link MqttClient client}
	 * 
	 * @throws IllegalStateException
	 *             If this factory was constructed to create asynchronous clients and not synchronous clients.
	 */
	public MqttClient newSynchronousClient(PublishListener publishListener) throws IllegalStateException {

		if (!blocking) {
			throw new IllegalStateException("You may not create a synchronous client using a client factory configured to create asynchronous clients");
		}

		return new FactoryClient(brokerUri, publishListener, null, reconnectionStrategy.clone(), executor, manager, reconnectionExecutor);
	}

	/**
	 * Creates an asynchronous {@link MqttClient client}. You may only use this method if the factory was constructed to create asynchronous clients.
	 * 
	 * @param asyncClientListener
	 *            Handles events from this client's channel
	 * 
	 * @return A new asynchronous {@link MqttClient client}
	 * 
	 * @throws IllegalStateException
	 *             If this factory was constructed to create synchronous clients and not asynchronous clients.
	 */
	public MqttClient newAsyncClient(AsyncClientListener asyncClientListener) throws IllegalStateException {

		if (blocking) {
			throw new IllegalStateException("You may not create aa asynchronous client using a client factory configured to create synchronous clients");
		}

		return new FactoryClient(brokerUri, asyncClientListener, asyncClientListener, reconnectionStrategy.clone(), executor, manager, reconnectionExecutor);
	}

	private MqttClientFactory(String brokerUri, ReconnectionStrategy reconnectionStrategy, ExecutorService executorService, int messageResendIntervalSeconds,
			int blockingTimeoutSeconds) {

		this.blocking = blockingTimeoutSeconds >= 0;
		this.brokerUri = brokerUri;
		this.executor = executorService;
		this.executorService = executorService;
		this.reconnectionExecutor = Executors.newSingleThreadScheduledExecutor();
		this.reconnectionStrategy = reconnectionStrategy != null ? reconnectionStrategy : new NullReconnectStrategy();
		this.manager = new ChannelManagerImpl(messageResendIntervalSeconds, blockingTimeoutSeconds);
		this.manager.init();
	}

	private static final class FactoryClient extends AbstractMqttClient {

		FactoryClient(String brokerUri, PublishListener publishListener, AsyncClientListener asyncClientListener, ReconnectionStrategy reconnectionStrategy,
				Executor executor, ChannelManager manager, ScheduledExecutorService reconnectionExecutor) {
			super(brokerUri, publishListener, asyncClientListener, reconnectionStrategy, executor, manager, reconnectionExecutor);
		}
	};
}
