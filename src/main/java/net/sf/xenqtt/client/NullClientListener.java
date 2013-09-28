package net.sf.xenqtt.client;

import net.sf.xenqtt.message.ConnectReturnCode;

/**
 * Acks any received messages. Ignores all other events.
 */
final class NullClientListener implements AsyncClientListener {

	/**
	 * @see net.sf.xenqtt.client.MqttClientListener#publishReceived(net.sf.xenqtt.client.MqttClient, net.sf.xenqtt.client.PublishMessage)
	 */
	@Override
	public void publishReceived(MqttClient client, PublishMessage message) {
	}

	/**
	 * @see net.sf.xenqtt.client.MqttClientListener#disconnected(net.sf.xenqtt.client.MqttClient, java.lang.Throwable, boolean)
	 */
	@Override
	public void disconnected(MqttClient client, Throwable cause, boolean reconnecting) {
	}

	/**
	 * @see net.sf.xenqtt.client.AsyncClientListener#connected(net.sf.xenqtt.client.MqttClient, net.sf.xenqtt.message.ConnectReturnCode)
	 */
	@Override
	public void connected(MqttClient client, ConnectReturnCode returnCode) {
	}

	/**
	 * @see net.sf.xenqtt.client.AsyncClientListener#subscribed(net.sf.xenqtt.client.MqttClient, net.sf.xenqtt.client.Subscription[],
	 *      net.sf.xenqtt.client.Subscription[], boolean)
	 */
	@Override
	public void subscribed(MqttClient client, Subscription[] requestedSubscriptions, Subscription[] grantedSubscriptions, boolean requestsGranted) {
	}

	/**
	 * @see net.sf.xenqtt.client.AsyncClientListener#unsubscribed(net.sf.xenqtt.client.MqttClient, java.lang.String[])
	 */
	@Override
	public void unsubscribed(MqttClient client, String[] topics) {
	}

	/**
	 * @see net.sf.xenqtt.client.AsyncClientListener#published(net.sf.xenqtt.client.MqttClient, net.sf.xenqtt.client.PublishMessage)
	 */
	@Override
	public void published(MqttClient client, PublishMessage message) {
		message.ack();
	}
}
