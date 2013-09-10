package net.sf.xenqtt.client;

/**
 * Reconnect strategy that does 0 reconnect attempts.
 */
final class NullReconnectStrategy implements ReconnectionStrategy {

	/**
	 * @see net.sf.xenqtt.client.ReconnectionStrategy#connectionLost(net.sf.xenqtt.client.MqttClient, java.lang.Throwable)
	 */
	@Override
	public long connectionLost(MqttClient client, Throwable cause) {

		return -1;
	}

}
