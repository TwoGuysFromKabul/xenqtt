package net.sf.xenqtt.client;

import net.sf.xenqtt.message.QoS;

/**
 * Implement this interface to use {@link SynchronousMqttClient}. The client will invoke the methods in this interface when a published message is received. A
 * single instance of this interface may be used with multiple clients.
 */
public interface MqttClientListener {

	/**
	 * Called when a published message is received from the broker. You should always call {@link PublishMessage#ack() ack()} when you are done processing the
	 * message. This is not required if the {@link PublishMessage#getQoS() QoS} is {@link QoS#AT_MOST_ONCE} but it is a good practice to always call it.
	 * 
	 * @param client
	 *            The client that received the message
	 * @param message
	 *            The message that was published
	 */
	void publishReceived(MqttClient client, PublishMessage message);

	/**
	 * Called when the connection to the broker is lost either unintentionally or because the client requested the disconnect.
	 * 
	 * @param client
	 *            The client that was disconnected
	 * @param cause
	 *            The exception that caused the client to disconnect. Null if there was no exception.
	 * @param reconnecting
	 *            True if the client will attempt to reconnect. False if either all reconnect attempts have failed or the disconnect was not because of an
	 *            exception.
	 */
	void disconnected(MqttClient client, Throwable cause, boolean reconnecting);
}
