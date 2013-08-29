package net.sf.xenqtt.client;

import net.sf.xenqtt.message.ConnectReturnCode;
import net.sf.xenqtt.message.QoS;

public interface MqttClient {

	ConnectReturnCode connect();

	void disconnect();

	QoS[] subscribe();

	void unsubscribe();

	void publish();

	void pubRelease();

	MqttClient newConnection();
}
