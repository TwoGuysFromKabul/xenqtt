package net.sf.xenqtt.client;

import net.sf.xenqtt.message.ConnectReturnCode;
import net.sf.xenqtt.message.QoS;

/**
 * <p>
 * A {@link MqttMessageHandler} that specifies an implementation contract that requires asynchronous handling of activities related to interacting with an MQTT
 * broker. This includes the following:
 * </p>
 * 
 * <ul>
 * <li>Notifications when a connection to the broker has finished</li>
 * <li>Notifications when publish attempts of messages have completed</li>
 * <li>Notifications when subscription requests have completed</li>
 * <li>Notifications when unsubscribe requests have been completed</li>
 * </ul>
 */
public interface AsyncMessageHandler extends MqttMessageHandler {

	void connectDone(ConnectReturnCode returnCode);

	void subscribed(QoS[] qos);

	void unsubscribed();

	void published();
}
