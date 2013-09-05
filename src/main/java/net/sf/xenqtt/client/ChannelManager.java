package net.sf.xenqtt.client;

import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import net.sf.xenqtt.message.MessageHandler;
import net.sf.xenqtt.message.MqttChannel;
import net.sf.xenqtt.message.MqttMessage;

/**
 * <p>
 * Specifies a type that manages zero or more {@link MqttChannel channels} that communicate via the MQTT protocol. The channel manager is responsible for
 * managing the {@link Selector selector} for IO work. The channel manager facilitates the creation of new channels, assigning selectors as-appropriate, and
 * then the addition of those channels. Sending data as well as disconnecting channels are also provided via the channel manager.
 * </p>
 * 
 * <p>
 * All of the methods in this specification are thread-safe and asynchronous.
 * </p>
 */
public interface ChannelManager {

	/**
	 * Create a new {@link MqttChannel} for use in exchanging data using the MQTT protocol. The implementation is responsible for assigning the {@link Selector}
	 * as-appropriate.
	 * 
	 * @param host
	 *            The host name to connect to
	 * @param port
	 *            The port to connect to
	 * @param messageHandler
	 *            The {@link MessageHandler message handler} to use for all received messages
	 * 
	 * @return The newly-created channel
	 */
	MqttChannel newChannel(String host, int port, MessageHandler messageHandler);

	/**
	 * Create a new {@link MqttChannel} for use in exchanging data using the MQTT protocol. The implementation is responsible for assigning the {@link Selector}
	 * as-appropriate.
	 * 
	 * @param channel
	 *            The {@link SocketChannel channel} to communicate over
	 * @param messageHandler
	 *            The {@link MessageHandler message handler} to use for all received messages
	 * 
	 * @return The newly-created channel
	 */
	MqttChannel newChannel(SocketChannel channel, MessageHandler messageHandler);

	/**
	 * Send a {@link MqttMessage message} over a specified {@code channel}.
	 * 
	 * @param channel
	 *            The {@link MqttChannel channel} to send the message over. This channel should have been previously created via the
	 *            {@link #newChannel(String, int, MessageHandler)} or {@link #newChannel(SocketChannel, MessageHandler)} methods
	 * @param message
	 *            The {@code message} to send. This can be any type of MQTT message
	 */
	void send(MqttChannel channel, MqttMessage message);

	/**
	 * Send a {@link MqttMessage message} to all channels managed by this channel manager.
	 * 
	 * @param message
	 *            The {@code message} to send. This can be any type of MQTT message
	 */
	void sendToAll(MqttMessage message);

	/**
	 * Close this {@link ChannelManager channel manager}. This will close all the channels currently managed within the connection manager. This method blocks
	 * until all channels are closed.
	 */
	void close();

}
