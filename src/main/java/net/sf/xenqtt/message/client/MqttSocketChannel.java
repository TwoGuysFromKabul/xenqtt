package net.sf.xenqtt.message.client;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Queue;

import net.sf.xenqtt.message.ConnAckMessage;
import net.sf.xenqtt.message.ConnectMessage;
import net.sf.xenqtt.message.DisconnectMessage;
import net.sf.xenqtt.message.MessageHandler;
import net.sf.xenqtt.message.MessageType;
import net.sf.xenqtt.message.MqttMessage;
import net.sf.xenqtt.message.PingReqMessage;
import net.sf.xenqtt.message.PingRespMessage;
import net.sf.xenqtt.message.PubAckMessage;
import net.sf.xenqtt.message.PubCompMessage;
import net.sf.xenqtt.message.PubRecMessage;
import net.sf.xenqtt.message.PubRelMessage;
import net.sf.xenqtt.message.PublishMessage;
import net.sf.xenqtt.message.SubAckMessage;
import net.sf.xenqtt.message.SubscribeMessage;
import net.sf.xenqtt.message.UnsubAckMessage;
import net.sf.xenqtt.message.UnsubscribeMessage;

// FIXME [jim] - for the gateway we want to always invoke all read ops as they will generate writes. then call all write ops
/**
 * {@link MqttChannel} implementation using a socket for transport.
 */
public class MqttSocketChannel implements MqttChannel {

	private final MessageHandler handler;
	private final Socket socket;
	private final SocketChannel channel;
	private final SelectionKey selectionKey;

	// reads the first byte of the fixed header
	private final ByteBuffer readHeader1 = ByteBuffer.allocate(2);

	// reads the next 3 bytes if the remaining length is > 127
	private final ByteBuffer readHeader2 = ByteBuffer.allocate(3);

	// created on the fly to read any remaining data.
	private ByteBuffer readRemaining;

	// the remaining length value for the message currently being read
	private int remainingLength;

	private final Queue<ByteBuffer> writesPending = new ArrayDeque<ByteBuffer>();

	private ByteBuffer sendBuffer;

	public MqttSocketChannel(Selector selector, Socket socket, MessageHandler handler) throws ClosedChannelException {
		this.socket = socket;
		this.handler = handler;
		this.channel = socket.getChannel();
		selectionKey = channel.register(selector, SelectionKey.OP_READ, this);
	}

	/**
	 * @see net.sf.xenqtt.message.client.MqttChannel#read()
	 */
	@Override
	public boolean read() throws IOException {

		if (readRemaining != null) {
			return readRemaining();
		}

		if (readHeader1.hasRemaining()) {
			int result = channel.read(readHeader1);
			if (readHeader1.hasRemaining()) {
				return result >= 0;
			}
		}

		byte firstLenByte = readHeader1.get(1);
		if (firstLenByte == 0) {
			processMessage(readHeader1);
			return true;
		}

		if ((firstLenByte & 0x80) == 0) {
			return readRemaining();
		}

		if (readHeader2.hasRemaining()) {
			int result = channel.read(readHeader2);
			if (readHeader2.hasRemaining()) {
				return result >= 0;
			}
		}

		return readRemaining();
	}

	/**
	 * @see net.sf.xenqtt.message.client.MqttChannel#send(net.sf.xenqtt.message.MqttMessage)
	 */
	@Override
	public void send(MqttMessage message) throws IOException {

		ByteBuffer buffer = message.getBuffer();
		buffer.flip();

		if (sendBuffer != null) {
			writesPending.offer(buffer);
			return;
		}

		sendBuffer = buffer;
		selectionKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
		write();
	}

	// FIXME [jim] - all need unit tests
	/**
	 * @see net.sf.xenqtt.message.client.MqttChannel#write()
	 */
	@Override
	public void write() throws IOException {

		while (sendBuffer != null) {
			int bytesWritten = channel.write(sendBuffer);
			if (bytesWritten == 0 || sendBuffer.hasRemaining()) {
				return;
			}
			sendBuffer = writesPending.poll();
		}

		selectionKey.interestOps(SelectionKey.OP_READ);
	}

	/**
	 * @see net.sf.xenqtt.message.client.MqttChannel#close()
	 */
	@Override
	public void close() {

		try {
			selectionKey.cancel();
			socket.close();
		} catch (IOException ignore) {
		}
	}

	private void processMessage(ByteBuffer buffer) {

		buffer.flip();

		MessageType messageType = MessageType.lookup((buffer.get(0) >> 4) & 0xff);
		switch (messageType) {
		case CONNECT:
			handler.handle(new ConnectMessage(buffer, remainingLength));
			break;
		case CONNACK:
			handler.handle(new ConnAckMessage(buffer, remainingLength));
			break;
		case PUBLISH:
			handler.handle(new PublishMessage(buffer, remainingLength));
			break;
		case PUBACK:
			handler.handle(new PubAckMessage(buffer, remainingLength));
			break;
		case PUBREC:
			handler.handle(new PubRecMessage(buffer, remainingLength));
			break;
		case PUBREL:
			handler.handle(new PubRelMessage(buffer, remainingLength));
			break;
		case PUBCOMP:
			handler.handle(new PubCompMessage(buffer, remainingLength));
			break;
		case SUBSCRIBE:
			handler.handle(new SubscribeMessage(buffer, remainingLength));
			break;
		case SUBACK:
			handler.handle(new SubAckMessage(buffer, remainingLength));
			break;
		case UNSUBSCRIBE:
			handler.handle(new UnsubscribeMessage(buffer, remainingLength));
			break;
		case UNSUBACK:
			handler.handle(new UnsubAckMessage(buffer, remainingLength));
			break;
		case PINGREQ:
			handler.handle(new PingReqMessage(buffer, remainingLength));
			break;
		case PINGRESP:
			handler.handle(new PingRespMessage(buffer, remainingLength));
			break;
		case DISCONNECT:
			handler.handle(new DisconnectMessage(buffer, remainingLength));
			break;
		default:
			throw new IllegalStateException("Unsupported message type: " + messageType);
		}

		readHeader1.clear();
		readHeader2.clear();
		readRemaining = null;
		remainingLength = 0;
	}

	/**
	 * Sets {@link #remainingLength}
	 * 
	 * @return The number of bytes in the remaining length field in the message
	 */
	private int calculateRemainingLength() {

		int byteCount = 0;
		byte b;
		int multiplier = 1;
		do {
			b = byteCount == 0 ? readHeader1.get(1) : readHeader2.get(byteCount - 1);
			remainingLength += (b & 0x7f) * multiplier;
			multiplier *= 0x80;
			byteCount++;
		} while ((b & 0x80) != 0);

		return byteCount;
	}

	private boolean readRemaining() throws IOException {

		if (readRemaining == null) {
			int remainingLengthSize = calculateRemainingLength();
			int headerSize = 1 + remainingLengthSize;
			readRemaining = ByteBuffer.allocate(remainingLength + headerSize);
			readHeader1.flip();
			readRemaining.put(readHeader1);

			if (readHeader2.position() > 0) {
				readHeader2.flip();
				readRemaining.put(readHeader2);
			}
		}

		int result = channel.read(readRemaining);
		if (readRemaining.hasRemaining()) {
			return result >= 0;
		}

		processMessage(readRemaining);

		return true;
	}
}
