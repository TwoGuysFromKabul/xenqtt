package net.sf.xenqtt.message;

/**
 * Receives {@link MqttMessage}s. Implementations will invoke a {@link MessageHandler} for each received message.
 */
public interface MessageReceiver {

	/**
	 * Receives data. This will read as many messages as it can a pass then to
	 * 
	 * @return True if the stream is still open. False if end of stream is reached.
	 */
	boolean receive();
}
