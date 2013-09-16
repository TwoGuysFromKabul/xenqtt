package net.sf.xenqtt.client;

import java.util.concurrent.Executor;

import net.sf.xenqtt.message.ChannelManager;
import net.sf.xenqtt.message.MqttMessage;

/**
 * An {@link MqttClient} that handles interactions with the MQTT broker in an asynchronous fashion. The {@link ChannelManager} used by this class is shut down
 * when the last connected client disconnects. An {@link Executor} is used to handle incoming messages. The executor can be owned by this class or user
 * supplied. If it is user supplied then it is the responsibility of the API user to shut it down. If the executor is owned by this class then this class will
 * shut it down when the last client disconnects.
 */
public final class AsyncMqttClient extends AbstractMqttClient {

	/**
	 * Constructs an instance of this class using an {@link Executor} owned by this class.
	 * 
	 * @param brokerUri
	 *            The URL to the broker to connect to. For example, tcp://q.m2m.io:1883
	 * @param listener
	 *            Handles events from this client
	 * @param reconnectionStrategy
	 *            The algorithm used to reconnect to the broker if the connection is lost
	 * @param messageHandlerThreadPoolSize
	 *            The number of threads used to handle incoming messages and invoke the {@link AsyncClientListener listener's} methods
	 * @param messageResendIntervalSeconds
	 *            Seconds between attempts to resend a message that is {@link MqttMessage#isAckable()}. 0 to disable message resends
	 */
	public AsyncMqttClient(String brokerUri, AsyncClientListener listener, ReconnectionStrategy reconnectionStrategy, int messageHandlerThreadPoolSize,
			int messageResendIntervalSeconds) {
		super(brokerUri, listener, reconnectionStrategy, messageHandlerThreadPoolSize, messageResendIntervalSeconds, -1);
	}

	/**
	 * Constructs an instance of this class using a user provided {@link Executor}.
	 * 
	 * @param brokerUri
	 *            The URL to the broker to connect to. For example, tcp://q.m2m.io:1883
	 * @param listener
	 *            Handles events from this client
	 * @param reconnectionStrategy
	 *            The algorithm used to reconnect to the broker if the connection is lost
	 * @param executor
	 *            The executor used to handle incoming messages and invoke the {@link AsyncClientListener listener's} methods. This class will NOT shut down the
	 *            executor.
	 * @param messageResendIntervalSeconds
	 *            Seconds between attempts to resend a message that is {@link MqttMessage#isAckable()}. 0 to disable message resends
	 */
	public AsyncMqttClient(String brokerUri, AsyncClientListener listener, ReconnectionStrategy reconnectionStrategy, Executor executor,
			int messageResendIntervalSeconds) {
		super(brokerUri, listener, reconnectionStrategy, executor, messageResendIntervalSeconds, -1);
	}
}