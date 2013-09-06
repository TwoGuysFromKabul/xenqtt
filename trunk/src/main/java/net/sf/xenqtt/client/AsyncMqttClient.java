package net.sf.xenqtt.client;

import java.util.concurrent.Executor;

import net.sf.xenqtt.message.ConnectReturnCode;
import net.sf.xenqtt.message.QoS;

/**
 * An {@link MqttClient} that handles interactions with the MQTT broker in an asynchronous fashion.
 */
public final class AsyncMqttClient implements MqttClient {

	/**
	 * FIXME [jim] - needs javadoc
	 * 
	 * @param listener
	 * @param messageHandlerThreadPoolSize
	 */
	public AsyncMqttClient(AsyncClientListener listener, int messageHandlerThreadPoolSize) {
		// TODO Auto-generated constructor stub
	}

	/**
	 * FIXME [jim] - needs javadoc
	 * 
	 * @param listener
	 * @param executor
	 */
	public AsyncMqttClient(AsyncClientListener listener, Executor executor) {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see net.sf.xenqtt.client.MqttClient#connect()
	 */
	@Override
	public ConnectReturnCode connect() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see net.sf.xenqtt.client.MqttClient#disconnect()
	 */
	@Override
	public void disconnect() {
		// TODO Auto-generated method stub

	}

	/**
	 * @see net.sf.xenqtt.client.MqttClient#subscribe(net.sf.xenqtt.client.Subscription[])
	 */
	@Override
	public Subscription[] subscribe(Subscription[] subscriptions) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see net.sf.xenqtt.client.MqttClient#unsubscribe(java.lang.String[])
	 */
	@Override
	public void unsubscribe(String[] topics) {
		// TODO Auto-generated method stub

	}

	/**
	 * @see net.sf.xenqtt.client.MqttClient#publish(java.lang.String, net.sf.xenqtt.message.QoS, byte[])
	 */
	@Override
	public void publish(String topicName, QoS qos, byte[] payload) {
		// TODO Auto-generated method stub

	}

	/**
	 * @see net.sf.xenqtt.client.MqttClient#publish(java.lang.String, net.sf.xenqtt.message.QoS, byte[], boolean)
	 */
	@Override
	public void publish(String topicName, QoS qos, byte[] payload, boolean retain) {
		// TODO Auto-generated method stub

	}

	/**
	 * @see net.sf.xenqtt.client.MqttClient#publish(java.lang.String, net.sf.xenqtt.message.QoS, java.lang.String)
	 */
	@Override
	public void publish(String topicName, QoS qos, String payload) {
		// TODO Auto-generated method stub

	}

	/**
	 * @see net.sf.xenqtt.client.MqttClient#publish(java.lang.String, net.sf.xenqtt.message.QoS, java.lang.String, boolean)
	 */
	@Override
	public void publish(String topicName, QoS qos, String payload, boolean retain) {
		// TODO Auto-generated method stub

	}

	/**
	 * @see net.sf.xenqtt.client.MqttClient#newConnection()
	 */
	@Override
	public MqttClient newConnection() {
		// TODO Auto-generated method stub
		return null;
	}
}
