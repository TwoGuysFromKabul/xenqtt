package net.sf.xenqtt.client;

import net.sf.xenqtt.message.ConnectReturnCode;
import net.sf.xenqtt.message.QoS;

/**
 * An {@link MqttClient} that handles interactions with the MQTT broker in an asynchronous fashion.
 */
public final class AsyncMqttClient implements MqttClient {

	public AsyncMqttClient(AsyncMessageHandler messageHandler) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public ConnectReturnCode connect() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void disconnect() {
		// TODO Auto-generated method stub

	}

	@Override
	public QoS[] subscribe(String[] subscribeToTopics, QoS[] requestedQoses) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void unsubscribe() {
		// TODO Auto-generated method stub

	}

	@Override
	public void publish() {
		// TODO Auto-generated method stub

	}

	@Override
	public void pubRelease() {
		// TODO Auto-generated method stub

	}

	@Override
	public MqttClient newConnection() {
		// TODO Auto-generated method stub
		return null;
	}
}
