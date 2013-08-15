package net.sf.xenqtt.message;

/**
 * Sends {@link MqttMessage}s
 */
public interface MessageSender {

	/**
	 * Queues the specified message asynchronously.
	 * 
	 * @return True if this message is the only one pending. False otherwise.
	 */
	boolean enqueue(MqttMessage message);

	/**
	 * Sends as much data as possible.
	 * 
	 * @return True if there is still pending data. False if there is none.
	 */
	boolean send();
}
