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

	/**
	 * @see net.sf.xenqtt.client.ReconnectionStrategy#connectionEstablished()
	 */
	@Override
	public void connectionEstablished() {
	}

	/**
	 * @see java.lang.Object#clone()
	 * @see ReconnectionStrategy#clone()
	 */
	@Override
	public ReconnectionStrategy clone() {
		return this;
	}
}
