package net.sf.xenqtt.message.client;

import java.net.Socket;

import net.sf.xenqtt.message.MessageReceiver;
import net.sf.xenqtt.message.MessageSender;

/**
 * {@link MqttChannel} implementation using a socket for transport.
 */
public class MqttSocketChannel implements MqttChannel {

	// FIXME [jim] - implement

	private final MessageReceiver receiver;
	private final MessageSender sender;
	private final Socket socket;

	public MqttSocketChannel(MessageReceiver receiver, MessageSender sender, Socket socket) {
		this.receiver = receiver;
		this.sender = sender;
		this.socket = socket;
	}
}
