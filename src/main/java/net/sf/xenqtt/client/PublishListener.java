package net.sf.xenqtt.client;

/**
 * Specifies a type that handles publishing messages to an MQTT broker.
 */
public interface PublishListener {

	void publish(MqttClient client, PublishMessage message);
}
