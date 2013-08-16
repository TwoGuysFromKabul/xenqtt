package net.sf.xenqtt.message.client;

import net.sf.xenqtt.message.MessageHandler;
import net.sf.xenqtt.message.MqttMessage;

/**
 * Sends and receives {@link MqttMessage}s over a channel. This may be client or server side.
 */
public interface MqttChannel {

	/**
	 * Reads data. This will read as many messages as it can and pass them to a {@link MessageHandler}.
	 * 
	 * @return True if the stream is still open. False if end of stream is reached.
	 */
	boolean read();

	/**
	 * Sends the specified message asynchronously.
	 * 
	 * @return True if this message is being sent immediately. False if there are other messages ahead of it.
	 */
	boolean send(MqttMessage message);

	/**
	 * Writes as much data as possible.
	 * 
	 * @return True if there is still pending data to send. False if there is none.
	 */
	boolean write();
}
