package net.sf.xenqtt.client;

import net.sf.xenqtt.message.QoS;

/**
 * Implement this interface to use {@link SynchronousMqttClient}. The client will invoke the methods in this interface when a published message is received. A
 * single instance of this interface may be used with multiple clients.
 */
public interface PublishListener {

	/**
	 * Called when a published message is received from the broker. You should always call {@link PublishMessage#ack() ack()} when you are done processing the
	 * message. This is not required if the {@link PublishMessage#getQoS() QoS} is {@link QoS#AT_MOST_ONCE} but it is a good practice to always call it.
	 * 
	 * @param client
	 *            The client that received the message
	 * @param message
	 *            The message that was published
	 */
	void publish(MqttClient client, PublishMessage message);
}
