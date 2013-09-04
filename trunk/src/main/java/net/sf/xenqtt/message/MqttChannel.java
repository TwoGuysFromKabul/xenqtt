package net.sf.xenqtt.message;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

/**
 * <p>
 * Sends and receives {@link MqttMessage}s over a channel. This may be client or server side. Since {@link #read(long)} may generate data to send it should
 * always be called before {@link #write(long)}. {@link #houseKeeping(long)} should be called after both {@link #read(long)} and {@link #write(long)} as those
 * methods may change paramaters used to determine what housekeeping is required.
 * </p>
 * <p>
 * If any exception occurs the channel is closed.
 * </p>
 */
public interface MqttChannel {

	/**
	 * Deregisters this channel. Cancels the underlying {@link SelectionKey}.
	 */
	void deregister();

	/**
	 * Registers this channel with the specified selector. The {@link SelectionKey} for the previously registered selector is canceled. The current
	 * {@link MessageHandler} is replaced with the specified one.
	 */
	void register(Selector selector, MessageHandler handler) throws IOException;

	/**
	 * Finishes a connection. This should be called when a {@link SelectionKey}s {@link SelectionKey#OP_CONNECT} op is ready.
	 */
	void finishConnect() throws IOException;

	/**
	 * Reads data. This will read as many messages as it can and pass them to a {@link MessageHandler}.This should be called when a {@link SelectionKey}s
	 * {@link SelectionKey#OP_READ} op is ready.
	 * 
	 * @param now
	 *            The timestamp to use as the "current" time
	 * 
	 * @return True if the stream is still open. False if end of stream is reached in which case the channel is closed.
	 */
	boolean read(long now) throws IOException;

	/**
	 * Sends the specified message asynchronously. When a {@link DisconnectMessage} or a {@link ConnAckMessage} where {@link ConnAckMessage#getReturnCode()} is
	 * not {@link ConnectReturnCode#ACCEPTED} is sent the channel is closed automatically.
	 * 
	 * @param now
	 *            The timestamp to use as the "current" time
	 */
	void send(long now, MqttMessage message) throws IOException;

	/**
	 * Writes as much data as possible. This should be called when a {@link SelectionKey}s {@link SelectionKey#OP_WRITE} op is ready.
	 * 
	 * @param now
	 *            The timestamp to use as the "current" time
	 */
	void write(long now) throws IOException;

	/**
	 * Closes the underlying channels, sockets, etc
	 */
	void close();

	/**
	 * Tells whether or not this channel is open. This channel is open if the underlying channels, sockets, etc are open
	 * 
	 * @return true if, and only if, this channel is open
	 */
	boolean isOpen();

	/**
	 * Tells whether or not this channel is connected. This channel is connected if {@link #isOpen()} is true, Connect/ConnectAck has finished, and no
	 * disconnect has been received/sent.
	 * 
	 * @return True if and only if this channel is connected.
	 */
	boolean isConnected();

	/**
	 * Tells whether or not a connection operation is in progress on this channel.
	 * 
	 * @return true if, and only if, a connection operation has been initiated on this channel but not yet completed by invoking the {@link #finishConnect()}
	 *         method
	 */
	boolean isConnectionPending();

	/**
	 * Performs housekeeping: message resends, ping requests, etc
	 * 
	 * @param now
	 *            The timestamp to use as the "current" time
	 * 
	 * @return Maximum millis until this method should be called again.
	 */
	long houseKeeping(long now) throws IOException;

	/**
	 * @return The number of messages in the send queue. This does not include any message currently in the process of being sent
	 */
	int sendQueueDepth();

	/**
	 * @return The number of messages currently in flight (QoS level > 0)
	 */
	int inFlightMessageCount();
}
