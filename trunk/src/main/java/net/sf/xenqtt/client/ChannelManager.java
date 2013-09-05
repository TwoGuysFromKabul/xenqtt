package net.sf.xenqtt.client;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;

import net.sf.xenqtt.message.MessageHandler;
import net.sf.xenqtt.message.MqttChannelRef;
import net.sf.xenqtt.message.MqttMessage;

/**
 * <p>
 * Specifies a type that manages zero or more {@link MqttChannelRef channels} that communicate via the MQTT protocol.
 * </p>
 * 
 * <p>
 * All of the methods in this specification are thread-safe and asynchronous.
 * </p>
 */
public interface ChannelManager {

	/**
	 * Create a new client side {@link MqttChannelRef} for use in exchanging data using the MQTT protocol. This is the client end of the connection. The broker
	 * will have the remote end of the connection.
	 * 
	 * @param host
	 *            The host name to connect to
	 * @param port
	 *            The port to connect to
	 * @param messageHandler
	 *            The {@link MessageHandler message handler} to use for all received messages
	 * 
	 * @return The newly-created channel. The channel may only be safely accessed from the {@link MessageHandler} callback methods.
	 */
	MqttChannelRef newClientChannel(String host, int port, MessageHandler messageHandler);

	/**
	 * Create a new broker side {@link MqttChannelRef} for use in exchanging data using the MQTT protocol. This is the broker end of the connection. The client
	 * will have the remove end of the connection.
	 * 
	 * @param channel
	 *            The {@link SocketChannel channel} to communicate over
	 * @param messageHandler
	 *            The {@link MessageHandler message handler} to use for all received messages
	 * 
	 * @return The newly-created channel.The channel may only be safely accessed from the {@link MessageHandler} callback methods.
	 */
	MqttChannelRef newBrokerChannel(SocketChannel channel, MessageHandler messageHandler);

	/**
	 * Send a {@link MqttMessage message} over a specified {@code channel}.
	 * 
	 * @param channel
	 *            The {@link MqttChannelRef channel} to send the message over. This channel should have been previously created via the
	 *            {@link #newChannel(String, int, MessageHandler)} or {@link #newChannel(SocketChannel, MessageHandler)} methods
	 * @param message
	 *            The {@code message} to send. This can be any type of MQTT message
	 */
	void send(MqttChannelRef channel, MqttMessage message) throws ClosedChannelException;

	/**
	 * Send a {@link MqttMessage message} to all channels managed by this channel manager.
	 * 
	 * @param message
	 *            The {@code message} to send. This can be any type of MQTT message
	 */
	void sendToAll(MqttMessage message);

	/**
	 * Close this {@link ChannelManager channel manager}. This will close all the channels currently managed within this channel manager. This method blocks
	 * until all channels are closed.
	 */
	void close();

}
