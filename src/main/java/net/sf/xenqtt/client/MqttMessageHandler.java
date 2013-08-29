package net.sf.xenqtt.client;

import net.sf.xenqtt.message.PublishMessage;

public interface MqttMessageHandler {

	// FIXME [jim] - need to be able to ack the message
	void publish(PublishMessage message);
}
