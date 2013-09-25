package net.sf.xenqtt.client;

import java.util.concurrent.Executor;

import net.sf.xenqtt.MqttTimeoutException;
import net.sf.xenqtt.XenqttUtil;
import net.sf.xenqtt.message.MqttMessage;

/**
 * An {@link MqttClient} that interacts with an MQTT broker in a synchronous fashion. All MQTT-related operations happen in a blocking style where method
 * invocations will return once the operation completes.
 */
public final class SynchronousMqttClient extends AbstractMqttClient {

	/**
	 * Constructs an instance of this class using an {@link Executor} owned by this class.
	 * 
	 * @param brokerUri
	 *            The URL to the broker to connect to. For example, tcp://q.m2m.io:1883
	 * @param listener
	 *            Handles {@link PublishMessage publish messages} received by this client
	 * @param reconnectionStrategy
	 *            The algorithm used to reconnect to the broker if the connection is lost
	 * @param messageHandlerThreadPoolSize
	 *            The number of threads used to handle incoming messages and invoke the {@link MqttClientListener listener's} methods
	 * @param messageResendIntervalSeconds
	 *            Seconds between attempts to resend a message that is {@link MqttMessage#isAckable()}. 0 to disable message resends
	 * @param blockingTimeoutSeconds
	 *            Seconds until a blocked method invocation times out and an {@link MqttTimeoutException} is thrown
	 */
	public SynchronousMqttClient(String brokerUri, MqttClientListener listener, ReconnectionStrategy reconnectionStrategy, int messageHandlerThreadPoolSize,
			int messageResendIntervalSeconds, int blockingTimeoutSeconds) {
		super(XenqttUtil.validateNotEmpty("brokerUri", brokerUri), //
				XenqttUtil.validateNotNull("listener", listener), //
				XenqttUtil.validateNotNull("reconnectionStrategy", reconnectionStrategy), //
				XenqttUtil.validateGreaterThan("messageHandlerThreadPoolSize", messageHandlerThreadPoolSize, 0), //
				messageResendIntervalSeconds, //
				XenqttUtil.validateGreaterThanOrEqualTo("blockingTimeoutSeconds", blockingTimeoutSeconds, 0));
	}

	/**
	 * Constructs an instance of this class using a user provided {@link Executor}.
	 * 
	 * @param brokerUri
	 *            The URL to the broker to connect to. For example, tcp://q.m2m.io:1883
	 * @param listener
	 *            Handles {@link PublishMessage publish messages} received by this client
	 * @param reconnectionStrategy
	 *            The algorithm used to reconnect to the broker if the connection is lost
	 * @param executor
	 *            The executor used to handle incoming messages and invoke the {@link MqttClientListener listener's} methods. This class will NOT shut down the
	 *            executor.
	 * @param messageResendIntervalSeconds
	 *            Seconds between attempts to resend a message that is {@link MqttMessage#isAckable()}. 0 to disable message resends
	 * @param blockingTimeoutSeconds
	 *            Seconds until a blocked method invocation times out and an {@link MqttTimeoutException} is thrown
	 */
	public SynchronousMqttClient(String brokerUri, MqttClientListener listener, ReconnectionStrategy reconnectionStrategy, Executor executor,
			int messageResendIntervalSeconds, int blockingTimeoutSeconds) {
		super(XenqttUtil.validateNotEmpty("brokerUri", brokerUri), //
				XenqttUtil.validateNotNull("listener", listener), //
				XenqttUtil.validateNotNull("reconnectionStrategy", reconnectionStrategy), //
				XenqttUtil.validateNotNull("executor", executor), //
				messageResendIntervalSeconds, //
				XenqttUtil.validateGreaterThanOrEqualTo("blockingTimeoutSeconds", blockingTimeoutSeconds, 0));
	}
}