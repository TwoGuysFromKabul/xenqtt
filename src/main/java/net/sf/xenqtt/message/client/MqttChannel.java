package net.sf.xenqtt.message.client;

import java.io.IOException;

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
	boolean read() throws IOException;

	/**
	 * Sends the specified message asynchronously.
	 */
	void send(MqttMessage message) throws IOException;

	/**
	 * Writes as much data as possible.
	 */
	void write() throws IOException;

	/**
	 * Closes the underlying channels, sockets, etc
	 */
	void close();
}
