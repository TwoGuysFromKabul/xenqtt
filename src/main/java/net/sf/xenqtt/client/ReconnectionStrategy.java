package net.sf.xenqtt.client;

/**
 * FIXME [jim] - needs javadoc
 */
public interface ReconnectionStrategy {

	/**
	 * FIXME [jim] - needs javadoc
	 * 
	 * @param cause
	 * @return
	 */
	long connectionLost(MqttClient client, Throwable cause);
}
