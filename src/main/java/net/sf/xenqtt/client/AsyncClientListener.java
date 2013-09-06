package net.sf.xenqtt.client;

import net.sf.xenqtt.message.ConnectReturnCode;

/**
 * <p>
 * Implement this interface to use the {@link AsyncMqttClient}. The client will invoke the methods in this interface when various events happen. A single
 * instance of this interface may be used with multiple clients.
 * </p>
 */
public interface AsyncClientListener extends PublishListener {

	/**
	 * FIXME [jim] - needs javadoc
	 * 
	 * @param client
	 *            The client that is connected
	 * @param returnCode
	 */
	void connected(MqttClient client, ConnectReturnCode returnCode);

	/**
	 * FIXME [jim] - needs javadoc
	 * 
	 * @param client
	 *            The client that requested the subscriptions
	 * @param subscriptions
	 */
	void subscribed(MqttClient client, Subscription[] subscriptions);

	/**
	 * FIXME [jim] - needs javadoc
	 * 
	 * @param client
	 *            The client that requested the unsubscribe
	 * @param topics
	 */
	void unsubscribed(MqttClient client, String[] topics);

	/**
	 * FIXME [jim] - needs javadoc
	 * 
	 * @param client
	 *            The client the message was published to
	 * @param message
	 */
	void published(MqttClient client, PublishMessage message);

	/**
	 * FIXME [jim] - needs javadoc
	 * 
	 * @param client
	 *            The client that was disconnected
	 * @param cause
	 * @param reconnecting
	 */
	void disconnected(MqttClient client, Throwable cause, boolean reconnecting);
}
