package net.sf.xenqtt.client;

import net.sf.xenqtt.message.ConnectReturnCode;
import net.sf.xenqtt.message.QoS;

/**
 * An {@link MqttClient} that interacts with an MQTT broker in a synchronous fashion. All MQTT-related operations happen in a blocking style where method
 * invocations will return once the operation completes.
 */
public final class SynchronousMqttClient implements MqttClient {

	public SynchronousMqttClient(MqttMessageHandler messageHandler) {

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
