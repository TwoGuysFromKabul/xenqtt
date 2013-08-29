package net.sf.xenqtt.client;

import net.sf.xenqtt.message.PublishMessage;

/**
 * Specifies a type that handles publishing messages to an MQTT broker.
 */
public interface MqttMessageHandler {

	// FIXME [jim] - need to be able to ack the message
	void publish(PublishMessage message);
}
