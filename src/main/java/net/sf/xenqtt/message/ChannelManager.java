package net.sf.xenqtt.message;

import java.net.URI;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;

import net.sf.xenqtt.MqttCommandCancelledException;
import net.sf.xenqtt.MqttInterruptedException;
import net.sf.xenqtt.MqttTimeoutException;

/**
 * <p>
 * Specifies a type that manages zero or more {@link MqttChannelRef channels} that communicate via the MQTT protocol.
 * </p>
 * 
 * <p>
 * All of the methods in this specification are thread-safe. Implementations may be blocking or non-blocking
 * </p>
 */
public interface ChannelManager {

	/**
	 * Create a new client side {@link MqttChannelRef} for use in exchanging data using the MQTT protocol. This is the client end of the connection. The broker
	 * will have the remote end of the connection. This method only blocks long enough for the channel to be created, not for the TCP connection to happen.
	 * 
	 * @param brokerUri
	 *            URI of the broker to connect to. For example, tcp://q.m2m.io:1883.
	 * @param messageHandler
	 *            The {@link MessageHandler message handler} to use for all received messages
	 * 
	 * @return The newly-created channel. The channel may only be safely accessed from the {@link MessageHandler} callback methods.
	 * 
	 * @throws MqttCommandCancelledException
	 *             The channel manager uses a command pattern to process this request on the IO thread. If the command is cancelled for some reason, like the
	 *             channel closes, this exception is thrown.
	 * @throws MqttTimeoutException
	 *             Thrown when using a synchronous implementation and the timeout specified for a blocked method expires
	 * @throws MqttInterruptedException
	 *             Thrown when the calling thread is interrupted
	 */
	MqttChannelRef newClientChannel(String brokerUri, MessageHandler messageHandler) throws MqttCommandCancelledException, MqttTimeoutException,
			MqttInterruptedException;

	/**
	 * Create a new client side {@link MqttChannelRef} for use in exchanging data using the MQTT protocol. This is the client end of the connection. The broker
	 * will have the remote end of the connection. This method only blocks long enough for the channel to be created, not for the TCP connection to happen. This
	 * will throw a {@link RuntimeException} wrapping any exception thrown while initializing the connection like {@link UnresolvedAddressException}
	 * 
	 * @param brokerUri
	 *            URI of the broker to connect to. For example, tcp://q.m2m.io:1883.
	 * @param messageHandler
	 *            The {@link MessageHandler message handler} to use for all received messages
	 * 
	 * @return The newly-created channel. The channel may only be safely accessed from the {@link MessageHandler} callback methods.
	 * 
	 * @throws MqttCommandCancelledException
	 *             The channel manager uses a command pattern to process this request on the IO thread. If the command is cancelled for some reason, like the
	 *             channel closes, this exception is thrown.
	 * @throws MqttTimeoutException
	 *             Thrown when using a synchronous implementation and the timeout specified for a blocked method expires
	 * @throws MqttInterruptedException
	 *             Thrown when the calling thread is interrupted
	 */
	MqttChannelRef newClientChannel(URI brokerUri, MessageHandler messageHandler) throws MqttCommandCancelledException, MqttTimeoutException,
			MqttInterruptedException;

	/**
	 * Create a new client side {@link MqttChannelRef} for use in exchanging data using the MQTT protocol. This is the client end of the connection. The broker
	 * will have the remote end of the connection. This method only blocks long enough for the channel to be created, not for the TCP connection to happen. This
	 * will throw a {@link RuntimeException} wrapping any exception thrown while initializing the connection like {@link UnresolvedAddressException}
	 * 
	 * @param host
	 *            The host name to connect to
	 * @param port
	 *            The port to connect to
	 * @param messageHandler
	 *            The {@link MessageHandler message handler} to use for all received messages
	 * 
	 * @return The newly-created channel. The channel may only be safely accessed from the {@link MessageHandler} callback methods.
	 * 
	 * @throws MqttCommandCancelledException
	 *             The channel manager uses a command pattern to process this request on the IO thread. If the command is cancelled for some reason, like the
	 *             channel closes, this exception is thrown.
	 * @throws MqttTimeoutException
	 *             Thrown when using a synchronous implementation and the timeout specified for a blocked method expires
	 * @throws MqttInterruptedException
	 *             Thrown when the calling thread is interrupted
	 */
	MqttChannelRef newClientChannel(String host, int port, MessageHandler messageHandler) throws MqttCommandCancelledException, MqttTimeoutException,
			MqttInterruptedException;

	/**
	 * Create a new broker side {@link MqttChannelRef} for use in exchanging data using the MQTT protocol. This is the broker end of the connection. The client
	 * will have the remove end of the connection. If an exception is thrown the socketChannel will be closed. This method only blocks long enough for the
	 * channel to be created.
	 * 
	 * @param socketChannel
	 *            The {@link SocketChannel channel} to communicate over
	 * @param messageHandler
	 *            The {@link MessageHandler message handler} to use for all received messages
	 * 
	 * @return The newly-created channel.The channel may only be safely accessed from the {@link MessageHandler} callback methods.
	 * 
	 * @throws MqttCommandCancelledException
	 *             The channel manager uses a command pattern to process this request on the IO thread. If the command is cancelled for some reason, like the
	 *             channel closes, this exception is thrown.
	 * @throws MqttTimeoutException
	 *             Thrown when using a synchronous implementation and the timeout specified for a blocked method expires
	 * @throws MqttInterruptedException
	 *             Thrown when the calling thread is interrupted
	 */
	MqttChannelRef newBrokerChannel(SocketChannel socketChannel, MessageHandler messageHandler) throws MqttCommandCancelledException, MqttTimeoutException,
			MqttInterruptedException;

	/**
	 * Send a {@link MqttMessage message} over a specified {@code channel}. This method only blocks until the message is queued to send to the channel.
	 * 
	 * @param channel
	 *            The {@link MqttChannelRef channel} to send the message over. This channel should have been previously created via the
	 *            {@link #newChannel(String, int, MessageHandler)} or {@link #newChannel(SocketChannel, MessageHandler)} methods
	 * @param message
	 *            The {@code message} to send. This can be any type of MQTT message
	 * @return In a synchronous implementation this returns the ack message if the message being sent is a {@link ConnectMessage} or has a
	 *         {@link MqttMessage#getQoSLevel() QoS} > 0.
	 * 
	 * @throws MqttCommandCancelledException
	 *             The channel manager uses a command pattern to process this request on the IO thread. If the command is cancelled for some reason, like the
	 *             channel closes, this exception is thrown.
	 * @throws MqttTimeoutException
	 *             Thrown when using a synchronous implementation and the timeout specified for a blocked method expires
	 * @throws MqttInterruptedException
	 *             Thrown when the calling thread is interrupted
	 */
	MqttMessage send(MqttChannelRef channel, MqttMessage message) throws MqttCommandCancelledException, MqttTimeoutException, MqttInterruptedException;

	/**
	 * Closes the specified channel. This method blocks until the channel is closed.
	 * 
	 * @throws MqttCommandCancelledException
	 *             The channel manager uses a command pattern to process this request on the IO thread. If the command is cancelled for some reason, like the
	 *             channel closes, this exception is thrown.
	 * @throws MqttTimeoutException
	 *             Thrown when using a synchronous implementation and the timeout specified for a blocked method expires
	 * @throws MqttInterruptedException
	 *             Thrown when the calling thread is interrupted
	 */
	void close(MqttChannelRef channel) throws MqttCommandCancelledException, MqttTimeoutException, MqttInterruptedException;

	/**
	 * Starts this channel manager. Must be called before any other methods
	 */
	void init();

	/**
	 * Stops this channel manager. Any other methods called after this have unpredictable results. Closes all open channels. Blocks until shutdown is complete.
	 */
	void shutdown();

	/**
	 * @return True if this manager is running and not yet stopped. This really means the manager's IO thread is running.
	 */
	boolean isRunning();
}
