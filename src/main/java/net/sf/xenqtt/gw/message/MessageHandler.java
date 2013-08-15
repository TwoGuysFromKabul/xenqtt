package net.sf.xenqtt.gw.message;

/**
 * Handles received {@link MqttMessage}s. There is a method for each message type.
 */
public interface MessageHandler {

	/**
	 * Called when a {@link ConnectMessage} is received
	 */
	void handle(ConnectMessage message);

	/**
	 * Called when a {@link ConnAckMessage} is received
	 */
	void handle(ConnAckMessage message);
	// FIXME [jim] - need to add methods for other message types
}
