package net.sf.xenqtt.client;

import net.sf.xenqtt.message.ConnectReturnCode;
import net.sf.xenqtt.message.QoS;

/**
 * <p>
 * Specifies a type that serves as a client to an MQTT broker. Such clients must implement the following functionality:
 * </p>
 * 
 * <ul>
 * <li>Connect - Connect to the MQTT broker</li>
 * <li>Subscribe - Subscribe to zero or more topics via specified topic filters</li>
 * <li>Publish - Publish messages to disparate MQTT topics</li>
 * <li>Publish Release - Issue a response to a publish release message</li>
 * </ul>
 * 
 * <p>
 * In addition, an {@code MqttClient} implementation must provide the ability to create a new connection based on certain parameters of the existing connection.
 * </p>
 */
public interface MqttClient {

	ConnectReturnCode connect();

	void disconnect();

	Subscription[] subscribe(Subscription[] subscriptions);

	void unsubscribe(String[] topics);

	void publish(String topicName, QoS qos, byte[] payload);

	void publish(String topicName, QoS qos, byte[] payload, boolean retain);

	void publish(String topicName, QoS qos, String payload);

	void publish(String topicName, QoS qos, String payload, boolean retain);

	MqttClient newConnection();
}
