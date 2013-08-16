package net.sf.xenqtt.message.client;

import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Queue;

import net.sf.xenqtt.message.MessageHandler;
import net.sf.xenqtt.message.MqttMessage;

/**
 * {@link MqttChannel} implementation using a socket for transport.
 */
public class MqttSocketChannel implements MqttChannel {

	// FIXME [jim] - implement

	private final Socket socket;
	private final MessageHandler handler;

	// reads the first byte of the fixed header
	private final ByteBuffer readHeader1 = ByteBuffer.allocate(2);

	// reads the next 3 bytes if the remaining length is > 127
	private final ByteBuffer readHeader2 = ByteBuffer.allocate(3);

	// created on the fly to read any remaining data.
	private ByteBuffer readRemaining;

	private final Queue<MqttMessage> writesPending = new ArrayDeque<MqttMessage>();

	private MqttMessage currentMessageBeingWritten;

	public MqttSocketChannel(Socket socket, MessageHandler handler) {
		this.socket = socket;
		this.handler = handler;
	}

	/**
	 * @see net.sf.xenqtt.message.client.MqttChannel#read()
	 */
	@Override
	public boolean read() {
		// FIXME [jim] - read and handle messages
		return false;
	}

	/**
	 * @see net.sf.xenqtt.message.client.MqttChannel#send(net.sf.xenqtt.message.MqttMessage)
	 */
	@Override
	public boolean send(MqttMessage message) {

		if (currentMessageBeingWritten == null) {
			currentMessageBeingWritten = message;
			// FIXME [jim] - maybe just call send? single threaded after all
			return true;
		}

		writesPending.offer(message);
		return false;
	}

	/**
	 * @see net.sf.xenqtt.message.client.MqttChannel#write()
	 */
	@Override
	public boolean write() {
		// FIXME [jim] - write messages
		return false;
	}
}
