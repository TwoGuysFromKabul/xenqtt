package net.sf.xenqtt.client;

import net.sf.xenqtt.message.ConnectReturnCode;
import net.sf.xenqtt.message.QoS;

public interface AsyncMessageHandler extends MqttMessageHandler {

	void connectDone(ConnectReturnCode returnCode);

	void subscribed(QoS[] qos);

	void unsubscribed();

	void published();
}
