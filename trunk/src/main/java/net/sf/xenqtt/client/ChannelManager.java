package net.sf.xenqtt.client;

import java.nio.channels.SocketChannel;

import net.sf.xenqtt.message.ConnectMessage;
import net.sf.xenqtt.message.MessageHandler;
import net.sf.xenqtt.message.MqttChannel;
import net.sf.xenqtt.message.MqttMessage;

/**
 * <p>
 * All of the methods in this specification are thread-safe.
 * </p>
 */
public interface ChannelManager {

	MqttChannel newChannel(String host, int port, MessageHandler messageHandler);

	MqttChannel newChannel(SocketChannel channel, MessageHandler messageHandler);

	void addClient(MqttChannel channel, ConnectMessage connectMessage);

	void send(MqttChannel channel, MqttMessage message);

	void disconnect(MqttChannel channel);

	void disconnectAll();

	/**
	 * Close this {@link ChannelManager connection manager}. This will close all the channels currently managed within the connection manager. This method
	 * blocks until all the channels have been closed.
	 */
	void close();

}
