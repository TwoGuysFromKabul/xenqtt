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

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.sf.xenqtt.MqttInterruptedException;
import net.sf.xenqtt.MqttTimeoutException;
import net.sf.xenqtt.XenqttUtil;
import net.sf.xenqtt.message.ChannelManager;
import net.sf.xenqtt.message.ChannelManagerImpl;
import net.sf.xenqtt.message.ConnAckMessage;
import net.sf.xenqtt.message.ConnectMessage;
import net.sf.xenqtt.message.MqttMessage;

/**
 * Used to create multiple "sibling" {@link MqttClient clients} that share an {@link Executor}, broker URI, etc.
 */
public final class MqttClientFactory {

	private final int connectTimeoutSeconds;
	private final boolean blocking;
	private final ChannelManager manager;
	private final ReconnectionStrategy reconnectionStrategy;
	private final Executor executor;
	private final ExecutorService executorService;
	private final ScheduledExecutorService reconnectionExecutor;
	private final String brokerUri;

	/**
	 * Constructs an object to create synchronous or asynchronous {@link MqttClient clients} using an {@link Executor} owned by this class with the following
	 * config:
	 * <ul>
	 * <li>reconnectionStrategy: {@link ProgressiveReconnectionStrategy} where the initial reconnection attempt happens after 50 millis then increases by a
	 * factor of 5 up to 30 seconds where it continues to retry indefinitely.</li>
	 * <li>connectTimeoutSeconds: 30</li>
	 * <li>messageResendIntervalSeconds: 30</li>
	 * </ul>
	 * 
	 * @param brokerUri
	 *            The URL to the broker to connect to. For example, tcp://q.m2m.io:1883
	 * @param messageHandlerThreadPoolSize
	 *            The number of threads used to handle incoming messages and invoke the {@link MqttClientListener listener's} methods
	 * @param synchronous
	 *            True to create synchronous clients, false to create asynchronous clients. If true then the synchronous clients' blockingTimeoutSeconds will be
	 *            0 (wait forever).
	 */
	public MqttClientFactory(String brokerUri, int messageHandlerThreadPoolSize, boolean synchronous) {
		this(brokerUri, new ProgressiveReconnectionStrategy(50, 5, Integer.MAX_VALUE, 30000), Executors.newFixedThreadPool(messageHandlerThreadPoolSize), 30,
				30, synchronous ? 0 : -1);
	}

	/**
	 * Constructs an object to create synchronous {@link MqttClient clients} using a user provided {@link Executor} with the following config:
	 * <ul>
	 * <li>reconnectionStrategy: {@link ProgressiveReconnectionStrategy} where the initial reconnection attempt happens after 50 millis then increases by a
	 * factor of 5 up to 30 seconds where it continues to retry indefinitely.</li>
	 * <li>connectTimeoutSeconds: 30</li>
	 * <li>messageResendIntervalSeconds: 30</li>
	 * <li>blockingTimeoutSeconds: 0 (waits forever)</li>
	 * </ul>
	 * 
	 * 
	 * @param brokerUri
	 *            The URL to the broker to connect to. For example, tcp://q.m2m.io:1883
	 * @param executor
	 *            Seconds until a blocked method invocation times out and an {@link MqttTimeoutException} is thrown. -1 will create a non-blocking API, 0 will
	 *            create a blocking API with no timeout, > 0 will create a blocking API with the specified timeout.
	 * @param synchronous
	 *            True to create synchronous clients, false to create asynchronous clients. If true then the synchronous clients' blockingTimeoutSeconds will be
	 *            0 (wait forever).
	 */
	public MqttClientFactory(String brokerUri, Executor executor, boolean synchronous) {
		this(brokerUri, new ProgressiveReconnectionStrategy(50, 5, Integer.MAX_VALUE, 30000), executor, 30, 30, synchronous ? 0 : -1);
	}

	/**
	 * Constructs an object to create synchronous {@link MqttClient clients} using an {@link Executor} owned by this class.
	 * 
	 * @param brokerUri
	 *            The URL to the broker to connect to. For example, tcp://q.m2m.io:1883
	 * @param reconnectionStrategy
	 *            The algorithm used to reconnect to the broker if the connection is lost
	 * @param messageHandlerThreadPoolSize
	 *            The number of threads used to handle incoming messages and invoke the {@link AsyncClientListener listener's} methods
	 * @param connectTimeoutSeconds
	 *            Seconds to wait for an {@link ConnAckMessage ack} to a {@link ConnectMessage connect message} before timing out and closing the channel. 0 to
	 *            wait forever.
	 * @param messageResendIntervalSeconds
	 *            Seconds between attempts to resend a message that is {@link MqttMessage#isAckable()}. 0 to disable message resends
	 * @param blockingTimeoutSeconds
	 *            Seconds until a blocked method invocation times out and an {@link MqttTimeoutException} is thrown. -1 will create a non-blocking API, 0 will
	 *            create a blocking API with no timeout, > 0 will create a blocking API with the specified timeout.
	 */
	public MqttClientFactory(String brokerUri, ReconnectionStrategy reconnectionStrategy, int messageHandlerThreadPoolSize, int connectTimeoutSeconds,
			int messageResendIntervalSeconds, int blockingTimeoutSeconds) {
		this(brokerUri, reconnectionStrategy, Executors.newFixedThreadPool(messageHandlerThreadPoolSize), connectTimeoutSeconds, messageResendIntervalSeconds,
				blockingTimeoutSeconds);
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
	 * @param connectTimeoutSeconds
	 *            Seconds to wait for an {@link ConnAckMessage ack} to a {@link ConnectMessage connect message} before timing out and closing the channel. 0 to
	 *            wait forever.
	 * @param messageResendIntervalSeconds
	 *            Seconds between attempts to resend a message that is {@link MqttMessage#isAckable()}. 0 to disable message resends
	 * @param blockingTimeoutSeconds
	 *            Seconds until a blocked method invocation times out and an {@link MqttTimeoutException} is thrown. -1 will create a non-blocking API, 0 will
	 *            create a blocking API with no timeout, > 0 will create a blocking API with the specified timeout.
	 */
	public MqttClientFactory(String brokerUri, ReconnectionStrategy reconnectionStrategy, Executor executor, int connectTimeoutSeconds,
			int messageResendIntervalSeconds, int blockingTimeoutSeconds) {

		this.connectTimeoutSeconds = connectTimeoutSeconds;
		this.blocking = blockingTimeoutSeconds >= 0;
		this.brokerUri = XenqttUtil.validateNotEmpty("brokerUri", brokerUri);
		this.executor = XenqttUtil.validateNotNull("executor", executor);
		this.executorService = null;
		this.reconnectionExecutor = Executors.newSingleThreadScheduledExecutor();
		this.reconnectionStrategy = reconnectionStrategy != null ? reconnectionStrategy : new NullReconnectStrategy();
		this.manager = new ChannelManagerImpl(XenqttUtil.validateGreaterThanOrEqualTo("messageResendIntervalSeconds", messageResendIntervalSeconds, 0),
				blockingTimeoutSeconds);
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
	 * @param connectTimeoutSeconds
	 *            Seconds to wait for an {@link ConnAckMessage ack} to a {@link ConnectMessage connect message} before timing out and closing the channel. 0 to
	 *            wait forever.
	 * @param messageResendIntervalSeconds
	 *            Seconds between attempts to resend a message that is {@link MqttMessage#isAckable()}. 0 to disable message resends
	 */
	public MqttClientFactory(String brokerUri, ReconnectionStrategy reconnectionStrategy, int messageHandlerThreadPoolSize, int connectTimeoutSeconds,
			int messageResendIntervalSeconds) {
		this(brokerUri, reconnectionStrategy, Executors.newFixedThreadPool(messageHandlerThreadPoolSize), connectTimeoutSeconds, messageResendIntervalSeconds,
				-1);
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
	 * @param connectTimeoutSeconds
	 *            Seconds to wait for an {@link ConnAckMessage ack} to a {@link ConnectMessage connect message} before timing out and closing the channel. 0 to
	 *            wait forever.
	 * @param messageResendIntervalSeconds
	 *            Seconds between attempts to resend a message that is {@link MqttMessage#isAckable()}. 0 to disable message resends
	 */
	public MqttClientFactory(String brokerUri, ReconnectionStrategy reconnectionStrategy, Executor executor, int connectTimeoutSeconds,
			int messageResendIntervalSeconds) {
		this(brokerUri, reconnectionStrategy, executor, connectTimeoutSeconds, messageResendIntervalSeconds, -1);
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
	 * @param mqttClientListener
	 *            Handles events from this client's channel. Use {@link MqttClientListener#NULL_LISTENER} if you don't want to receive messages or be notified
	 *            of events.
	 * 
	 * @return A new synchronous {@link MqttClient client}
	 * 
	 * @throws IllegalStateException
	 *             If this factory was constructed to create asynchronous clients and not synchronous clients.
	 */
	public MqttClient newSynchronousClient(MqttClientListener mqttClientListener) throws IllegalStateException {
		XenqttUtil.validateNotNull("mqttClientListener", mqttClientListener);

		if (!blocking) {
			throw new IllegalStateException("You may not create a synchronous client using a client factory configured to create asynchronous clients");
		}

		return new FactoryClient(brokerUri, mqttClientListener, null, reconnectionStrategy.clone(), executor, manager, reconnectionExecutor,
				connectTimeoutSeconds);
	}

	/**
	 * Creates an asynchronous {@link MqttClient client}. You may only use this method if the factory was constructed to create asynchronous clients.
	 * 
	 * @param asyncClientListener
	 *            Handles events from this client's channel. Use {@link AsyncClientListener#NULL_LISTENER} if you don't want to receive messages or be notified
	 *            of events.
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

		return new FactoryClient(brokerUri, asyncClientListener, asyncClientListener, reconnectionStrategy.clone(), executor, manager, reconnectionExecutor,
				connectTimeoutSeconds);
	}

	private MqttClientFactory(String brokerUri, ReconnectionStrategy reconnectionStrategy, ExecutorService executorService, int connectTimeoutSeconds,
			int messageResendIntervalSeconds, int blockingTimeoutSeconds) {

		this.connectTimeoutSeconds = connectTimeoutSeconds;
		this.blocking = blockingTimeoutSeconds >= 0;
		this.brokerUri = XenqttUtil.validateNotEmpty("brokerUri", brokerUri);
		this.executor = XenqttUtil.validateNotNull("executorService", executorService);
		this.executorService = executorService;
		this.reconnectionExecutor = Executors.newSingleThreadScheduledExecutor();
		this.reconnectionStrategy = reconnectionStrategy != null ? reconnectionStrategy : new NullReconnectStrategy();
		this.manager = new ChannelManagerImpl(XenqttUtil.validateGreaterThanOrEqualTo("messageResendIntervalSeconds", messageResendIntervalSeconds, 0),
				blockingTimeoutSeconds);
		this.manager.init();
	}

	private static final class FactoryClient extends AbstractMqttClient {

		FactoryClient(String brokerUri, MqttClientListener mqttClientListener, AsyncClientListener asyncClientListener,
				ReconnectionStrategy reconnectionStrategy, Executor executor, ChannelManager manager, ScheduledExecutorService reconnectionExecutor,
				int connectTimeoutSeconds) {
			super(brokerUri, mqttClientListener, asyncClientListener, reconnectionStrategy, executor, manager, reconnectionExecutor, connectTimeoutSeconds);
		}
	};
}
