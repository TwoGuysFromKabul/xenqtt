package net.sf.xenqtt.client;

import java.util.List;

import net.sf.xenqtt.message.ConnectReturnCode;

/**
 * <p>
 * Implement this interface to use the {@link AsyncMqttClient}. The client will invoke the methods in this interface when various events happen. A single
 * instance of this interface may be used with multiple clients.
 * </p>
 */
public interface AsyncClientListener extends PublishListener {

	/**
	 * Called after the client has received a connect acknowledgment from the broker.
	 * 
	 * @param client
	 *            The client that is connected
	 * 
	 * @param returnCode
	 *            The connect return code from the broker. Anything other than {@link ConnectReturnCode#ACCEPTED} will result in the client being immediately
	 *            disconnected.
	 */
	void connected(MqttClient client, ConnectReturnCode returnCode);

	/**
	 * Called when the client receives a subscribe acknowledgment from the broker. This method is called when the subscription request was made by
	 * {@link MqttClient#subscribe(Subscription[])}.
	 * 
	 * @param client
	 *            The client that requested the subscriptions
	 * @param requestedSubscriptions
	 *            The subscriptions requested. The topics will be the same as those in grantedSubscriptions and the {@link Subscription#getQos() QoS} will be
	 *            the QoS the client requested.
	 * @param grantedSubscriptions
	 *            The subscriptions. The topics will be the same as in requestedSubscriptions but the {@link Subscription#getQos() QoS} will be the QoS granted
	 *            by the broker, not the QoS requested by the client.
	 */
	void subscribed(MqttClient client, Subscription[] requestedSubscriptions, Subscription[] grantedSubscriptions);

	/**
	 * This method is the same as {@link #subscribed(MqttClient, Subscription[], Subscription[])} except it uses {@link List lists} instead of arrays and is
	 * called when the subscription request was made by {@link MqttClient#subscribe(java.util.List)}.
	 * 
	 * @see AsyncClientListener#subscribed(MqttClient, Subscription[], Subscription[])
	 */
	void subscribed(MqttClient client, List<Subscription> requestedSubscriptions, List<Subscription> grantedSubscriptions);

	/**
	 * Called when an unsubscribe acknowledgment is received from the broker. This method is called when the request was made by
	 * {@link MqttClient#unsubscribe(String[])}.
	 * 
	 * @param client
	 *            The client that requested the unsubscribe
	 * @param topics
	 *            The topics unsubscribe from. These may include wildcards.
	 */
	void unsubscribed(MqttClient client, String[] topics);

	/**
	 * This method is the same as {@link #unsubscribed(MqttClient, String[])} except it uses {@link List lists} instead of arrays and is called when the
	 * subscription request was made by {@link MqttClient#unsubscribe(List)}.
	 * 
	 * @see AsyncClientListener#subscribed(MqttClient, Subscription[], Subscription[])
	 */
	void unsubscribed(MqttClient client, List<String> topics);

	/**
	 * Called when an acknowledgment is received from the broker to a client published message.
	 * 
	 * @param client
	 *            The client the message was published to
	 * @param message
	 *            The message that was published.
	 */
	void published(MqttClient client, PublishMessage message);

	/**
	 * Called when the connection to the broker is lost either unintentionally or because the client requested the disconnect.
	 * 
	 * @param client
	 *            The client that was disconnected
	 * @param cause
	 *            The exception that caused the client to disconnect. Null if there was no exception.
	 * @param reconnecting
	 *            True if the client will attempt to reconnect. False if either all reconnect attempts have failed or the disconnect was requested by the
	 *            client.
	 */
	void disconnected(MqttClient client, Throwable cause, boolean reconnecting);
}
