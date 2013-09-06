package net.sf.xenqtt.client;

import java.util.List;
import java.util.concurrent.Executor;

import net.sf.xenqtt.MqttTimeoutException;
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
	public AsyncMqttClient(AsyncClientListener listener, ReconnectionStrategy reconnectionStrategy, int messageHandlerThreadPoolSize) {
		// TODO Auto-generated constructor stub
	}

	/**
	 * FIXME [jim] - needs javadoc
	 * 
	 * @param listener
	 * @param executor
	 */
	public AsyncMqttClient(AsyncClientListener listener, ReconnectionStrategy reconnectionStrategy, Executor executor) {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see net.sf.xenqtt.client.MqttClient#connect(java.lang.String, boolean, int, java.lang.String, java.lang.String, java.lang.String, java.lang.String,
	 *      net.sf.xenqtt.message.QoS, boolean)
	 */
	@Override
	public ConnectReturnCode connect(String clientId, boolean cleanSession, int keepAliveSeconds, String userName, String password, String willTopic,
			String willMessage, QoS willQos, boolean willRetain) throws MqttTimeoutException, InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see net.sf.xenqtt.client.MqttClient#connect(java.lang.String, boolean, int)
	 */
	@Override
	public ConnectReturnCode connect(String clientId, boolean cleanSession, int keepAliveSeconds) throws MqttTimeoutException, InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see net.sf.xenqtt.client.MqttClient#connect(java.lang.String, boolean, int, java.lang.String, java.lang.String)
	 */
	@Override
	public ConnectReturnCode connect(String clientId, boolean cleanSession, int keepAliveSeconds, String userName, String password)
			throws MqttTimeoutException, InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see net.sf.xenqtt.client.MqttClient#connect(java.lang.String, boolean, int, java.lang.String, java.lang.String, net.sf.xenqtt.message.QoS, boolean)
	 */
	@Override
	public ConnectReturnCode connect(String clientId, boolean cleanSession, int keepAliveSeconds, String willTopic, String willMessage, QoS willQos,
			boolean willRetain) throws MqttTimeoutException, InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see net.sf.xenqtt.client.MqttClient#disconnect()
	 */
	@Override
	public void disconnect() throws MqttTimeoutException, InterruptedException {
		// TODO Auto-generated method stub

	}

	/**
	 * @see net.sf.xenqtt.client.MqttClient#subscribe(net.sf.xenqtt.client.Subscription[])
	 */
	@Override
	public Subscription[] subscribe(Subscription[] subscriptions) throws MqttTimeoutException, InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see net.sf.xenqtt.client.MqttClient#subscribe(java.util.List)
	 */
	@Override
	public List<Subscription> subscribe(List<Subscription> subscriptions) throws MqttTimeoutException, InterruptedException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see net.sf.xenqtt.client.MqttClient#unsubscribe(java.lang.String[])
	 */
	@Override
	public void unsubscribe(String[] topics) throws MqttTimeoutException, InterruptedException {
		// TODO Auto-generated method stub

	}

	/**
	 * @see net.sf.xenqtt.client.MqttClient#unsubscribe(java.util.List)
	 */
	@Override
	public void unsubscribe(List<String> topics) throws MqttTimeoutException, InterruptedException {
		// TODO Auto-generated method stub

	}

	/**
	 * @see net.sf.xenqtt.client.MqttClient#publish(java.lang.String, net.sf.xenqtt.message.QoS, byte[])
	 */
	@Override
	public void publish(String topicName, QoS qos, byte[] payload) throws MqttTimeoutException, InterruptedException {
		// TODO Auto-generated method stub

	}

	/**
	 * @see net.sf.xenqtt.client.MqttClient#publish(java.lang.String, net.sf.xenqtt.message.QoS, byte[], boolean)
	 */
	@Override
	public void publish(String topicName, QoS qos, byte[] payload, boolean retain) throws MqttTimeoutException, InterruptedException {
		// TODO Auto-generated method stub

	}

	/**
	 * @see net.sf.xenqtt.client.MqttClient#publish(java.lang.String, net.sf.xenqtt.message.QoS, java.lang.String)
	 */
	@Override
	public void publish(String topicName, QoS qos, String payload) throws MqttTimeoutException, InterruptedException {
		// TODO Auto-generated method stub

	}

	/**
	 * @see net.sf.xenqtt.client.MqttClient#publish(java.lang.String, net.sf.xenqtt.message.QoS, java.lang.String, boolean)
	 */
	@Override
	public void publish(String topicName, QoS qos, String payload, boolean retain) throws MqttTimeoutException, InterruptedException {
		// TODO Auto-generated method stub

	}

	/**
	 * @see net.sf.xenqtt.client.MqttClient#newClient()
	 */
	@Override
	public MqttClient newClient() {
		// TODO Auto-generated method stub
		return null;
	}
}
