package net.sf.xenqtt.message;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.List;
import java.util.concurrent.CountDownLatch;

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
public interface MqttChannel extends MqttChannelRef {

	/**
	 * Deregisters this channel. Cancels the underlying {@link SelectionKey}.
	 */
	void deregister();

	/**
	 * Registers this channel with the specified selector. The {@link SelectionKey} for the previously registered selector, if any, is canceled. The current
	 * {@link MessageHandler} is replaced with the specified one.
	 * 
	 * @return A return value of true does NOT necessarily mean this channel is open but false does mean it is closed (or the connect hasn't finished yet).
	 */
	boolean register(Selector selector, MessageHandler handler);

	/**
	 * Finishes a connection. This should be called when a {@link SelectionKey}s {@link SelectionKey#OP_CONNECT} op is ready.
	 * 
	 * @return True if and only if this channel is now connected.
	 */
	boolean finishConnect();

	/**
	 * Reads data. This will read as many messages as it can and pass them to a {@link MessageHandler}.This should be called when a {@link SelectionKey}s
	 * {@link SelectionKey#OP_READ} op is ready.
	 * 
	 * @param now
	 *            The timestamp to use as the "current" time
	 * 
	 * @return True if the channel is left open. False if it is closed by this method or already closed when this method is called or the connect hasn't
	 *         finished yet.
	 */
	boolean read(long now);

	// FIXME [jim] - need a latch in channel ctor to trigger when connected
	/**
	 * Sends the specified message asynchronously. When a {@link DisconnectMessage} or a {@link ConnAckMessage} where {@link ConnAckMessage#getReturnCode()} is
	 * not {@link ConnectReturnCode#ACCEPTED} is sent the channel is closed automatically.
	 * 
	 * @param message
	 *            The message to send
	 * @param ackReceivedLatch
	 *            If not null then this latch is {@link CountDownLatch#countDown() triggered} when processing the message is complete. The definition of
	 *            complete is:
	 *            <ul>
	 *            <li>If the message is {@link MqttMessage#isAckable() ackable} processing is complete when the ack is received.</li>
	 *            <li>If none of the above is true then processing is complete when the message is written to the socket.</li>
	 *            </ul>
	 * 
	 * @return A return value of true does NOT necessarily mean this channel is open but false does mean it is closed (or the connect hasn't finished yet).
	 */
	boolean send(MqttMessage message, CountDownLatch ackReceivedLatch);

	/**
	 * Writes as much data as possible. This should be called when a {@link SelectionKey}s {@link SelectionKey#OP_WRITE} op is ready.
	 * 
	 * @param now
	 *            The timestamp to use as the "current" time
	 * 
	 * @return True if the channel is left open. False if it is closed by this method or already closed when this method is called or the connect hasn't
	 *         finished yet.
	 */
	boolean write(long now);

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
	 * @return Maximum millis until this method should be called again. This result is only valid when this method is called. Future calls to
	 *         {@link #read(long)} or {@link #write(long)} may change this value. Returns < 0 if this method closes the channel.
	 */
	long houseKeeping(long now);

	/**
	 * @return The number of messages in the send queue. This does not include any message currently in the process of being sent
	 */
	int sendQueueDepth();

	/**
	 * @return The number of messages currently in flight (QoS level > 0)
	 */
	int inFlightMessageCount();

	/**
	 * @return All messages that have not been sent. This includes messages queued to be sent, any partially sent message, and all in flight messages.
	 */
	List<MqttMessage> getUnsentMessages();
}
