package net.sf.xenqtt.message;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

/**
 * Sends and receives {@link MqttMessage}s over a channel. This may be client or server side.
 */
public interface MqttChannel {

	/**
	 * Registers this channel with the specified selector. The {@link SelectionKey} for the previously registered selector is canceled.
	 */
	void register(Selector selector) throws IOException;

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
