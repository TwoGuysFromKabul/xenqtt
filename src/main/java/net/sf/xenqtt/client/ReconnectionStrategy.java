package net.sf.xenqtt.client;

/**
 * Implementations are strategies used by {@link MqttClient} implementation to reconnect to the broker if the connection is lost. Implementations must be thread
 * safe.
 */
public interface ReconnectionStrategy {

	/**
	 * Called by an {@link MqttClient} each time the connection to the broker is lost other than by an intentional disconnect.
	 * 
	 * @param cause
	 *            The exception that cause the connection to close or resulted from the connection closing. May be {@code null}.
	 * 
	 * @return Milliseconds the client should wait before trying to connect to the broker again. If < 0 the client will stop trying to connect to the broker.
	 */
	long connectionLost(MqttClient client, Throwable cause);

	/**
	 * Called by an {@link MqttClient} instance when a connection to the broker is established.
	 */
	void connectionEstablished();
}
