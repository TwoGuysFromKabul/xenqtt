package net.sf.xenqtt.message;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Default {@link MqttChannel} implementation. This class is NOT thread safe. At construction a {@link SocketChannel} will be registered with the
 * {@link Selector} specified in the constructor. The new instance of this class will be available from {@link SelectionKey#attachment()}.
 */
// FIXME [jim] - have channel close on any exception
abstract class AbstractMqttChannel implements MqttChannel {

	private final Map<Integer, IdentifiableMqttMessage> inFlightMessages = new HashMap<Integer, IdentifiableMqttMessage>();
	private final List<IdentifiableMqttMessage> messagesToResend = new ArrayList<IdentifiableMqttMessage>();
	private final long messageResendIntervalMillis;

	private final SocketChannel channel;
	private SelectionKey selectionKey;
	private MessageHandler handler;

	// reads the first byte of the fixed header
	private final ByteBuffer readHeader1 = ByteBuffer.allocate(2);

	// reads the next 3 bytes if the remaining length is > 127
	private final ByteBuffer readHeader2 = ByteBuffer.allocate(3);

	// created on the fly to read any remaining data.
	private ByteBuffer readRemaining;

	// the remaining length value for the message currently being read
	private int remainingLength;

	private final Queue<MqttMessage> writesPending = new ArrayDeque<MqttMessage>();

	private MqttMessage sendMessageInProgress;

	private boolean connected;

	/**
	 * Starts an asynchronous connection to the specified host and port. When a {@link SelectionKey} for the specified selector has
	 * {@link SelectionKey#OP_CONNECT} as a ready op then {@link #finishConnect()} should be called.
	 * 
	 * @param messageResendIntervalMillis
	 *            Millis between attempts to resend a message that {@link MqttMessage#isAckable()}. 0 to disable message resends
	 */
	AbstractMqttChannel(String host, int port, MessageHandler handler, Selector selector, long messageResendIntervalMillis) throws IOException {

		this.messageResendIntervalMillis = messageResendIntervalMillis;
		this.channel = SocketChannel.open();
		this.channel.configureBlocking(false);
		this.handler = handler;
		this.selectionKey = channel.register(selector, SelectionKey.OP_CONNECT, this);
		this.channel.connect(new InetSocketAddress(host, port));
	}

	/**
	 * Use this constructor for clients accepted from a {@link ServerSocketChannel}.
	 * 
	 * @param messageResendIntervalMillis
	 *            Millis between attempts to resend a message that {@link MqttMessage#isAckable()}. 0 to disable message resends
	 */
	AbstractMqttChannel(SocketChannel channel, MessageHandler handler, Selector selector, long messageResendIntervalMillis) throws IOException {
		this.messageResendIntervalMillis = messageResendIntervalMillis;
		this.handler = handler;
		this.channel = channel;
		this.channel.configureBlocking(false);
		this.selectionKey = channel.register(selector, SelectionKey.OP_READ, this);
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#deregister()
	 */
	@Override
	public void deregister() {

		selectionKey.cancel();
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#register(java.nio.channels.Selector, net.sf.xenqtt.message.MessageHandler)
	 */
	@Override
	public void register(Selector selector, MessageHandler handler) throws IOException {

		int ops = sendMessageInProgress == null ? SelectionKey.OP_READ : SelectionKey.OP_READ | SelectionKey.OP_WRITE;

		selectionKey.cancel();
		selectionKey = channel.register(selector, ops, this);
		this.handler = handler;
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#finishConnect()
	 */
	@Override
	public void finishConnect() throws IOException {

		if (channel.finishConnect()) {
			int ops = sendMessageInProgress != null ? SelectionKey.OP_READ | SelectionKey.OP_WRITE : SelectionKey.OP_READ;
			selectionKey.interestOps(ops);
		}
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#read(long)
	 */
	@Override
	public boolean read(long now) throws IOException {

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
	 * @see net.sf.xenqtt.message.MqttChannel#send(long, net.sf.xenqtt.message.MqttMessage)
	 */
	@Override
	public void send(long now, MqttMessage message) throws IOException {

		if (sendMessageInProgress != null) {
			writesPending.offer(message);
			return;
		}

		sendMessageInProgress = message;

		if (channel.socket().isConnected()) {
			selectionKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
			write(now);
		}
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#write(long)
	 */
	@Override
	public void write(long now) throws IOException {

		while (sendMessageInProgress != null) {
			int bytesWritten = channel.write(sendMessageInProgress.buffer);
			if (bytesWritten == 0 || sendMessageInProgress.buffer.hasRemaining()) {
				return;
			}

			MessageType type = sendMessageInProgress.getMessageType();
			if (type == MessageType.DISCONNECT) {
				sendMessageInProgress = null;
				close();
				return;
			}

			if (type == MessageType.CONNACK) {
				ConnAckMessage m = (ConnAckMessage) sendMessageInProgress;
				if (m.getReturnCode() != ConnectReturnCode.ACCEPTED) {
					sendMessageInProgress = null;
					close();
					return;
				} else {
					connected = true;
				}
			}

			if (messageResendIntervalMillis > 0 && sendMessageInProgress.isAckable() && sendMessageInProgress.getQoSLevel() > 0) {
				IdentifiableMqttMessage m = (IdentifiableMqttMessage) sendMessageInProgress;
				m.nextSendTime = now + messageResendIntervalMillis;
				inFlightMessages.put(m.getMessageId(), m);
			}

			sendMessageInProgress = writesPending.poll();
		}

		selectionKey.interestOps(SelectionKey.OP_READ);
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#close()
	 */
	@Override
	public void close() {

		connected = false;

		try {
			selectionKey.cancel();
		} catch (Exception ignore) {
		}

		try {
			channel.close();
		} catch (Exception ignore) {
		}

		handler.channelClosed(this);
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#isOpen()
	 */
	@Override
	public boolean isOpen() {
		return channel.isOpen();
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#isConnected()
	 */
	@Override
	public boolean isConnected() {
		return connected;
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#isConnectionPending()
	 */
	@Override
	public boolean isConnectionPending() {
		return channel.isConnectionPending();
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#houseKeeping(long)
	 */
	@Override
	public long houseKeeping(long now) throws IOException {

		long maxIdleTime = Long.MAX_VALUE;

		if (messageResendIntervalMillis > 0) {
			maxIdleTime = resendMessages(now);
		}

		// FIXME [jim] - do keep alive

		return maxIdleTime;
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#sendQueueDepth()
	 */
	@Override
	public int sendQueueDepth() {

		return writesPending.size();
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#inFlightMessageCount()
	 */
	@Override
	public int inFlightMessageCount() {
		return inFlightMessages.size();
	}

	/**
	 * Called when a {@link PingReqMessage} is received.
	 */
	abstract void pingReq(PingReqMessage message);

	/**
	 * Called when a {@link PingRespMessage} is received.
	 */
	abstract void pingResp(PingRespMessage message);

	private long resendMessages(long now) throws IOException {

		long maxIdleTime = Long.MAX_VALUE;
		long minSendTime = now + 1000;

		Iterator<IdentifiableMqttMessage> msgIter = inFlightMessages.values().iterator();
		while (msgIter.hasNext()) {
			IdentifiableMqttMessage msg = msgIter.next();
			if (msg.nextSendTime <= minSendTime) {
				messagesToResend.add(msg);
				msgIter.remove();
			} else {
				long next = msg.nextSendTime - now;
				if (next < maxIdleTime) {
					maxIdleTime = next;
				}
			}
		}

		for (IdentifiableMqttMessage msg : messagesToResend) {
			msg.buffer.rewind();
			msg.setDuplicateFlag();
			send(now, msg);
		}

		messagesToResend.clear();

		return maxIdleTime;
	}

	private void processMessage(ByteBuffer buffer) {

		buffer.flip();

		try {
			handleMessage(buffer);
		} catch (Exception e) {
			e.printStackTrace();
			// FIXME [jim] - need to log this or something
		}

		readHeader1.clear();
		readHeader2.clear();
		readRemaining = null;
		remainingLength = 0;
	}

	private void handleMessage(ByteBuffer buffer) throws Exception {
		MessageType messageType = MessageType.lookup((buffer.get(0) & 0xf0) >> 4);
		switch (messageType) {
		case CONNECT:
			handler.connect(this, new ConnectMessage(buffer, remainingLength));
			break;
		case CONNACK:
			ConnAckMessage connAckMessage = new ConnAckMessage(buffer);
			connected = connAckMessage.getReturnCode() == ConnectReturnCode.ACCEPTED;
			handler.connAck(this, connAckMessage);
			break;
		case PUBLISH:
			handler.publish(this, new PublishMessage(buffer, remainingLength));
			break;
		case PUBACK:
			PubAckMessage pubAckMessage = new PubAckMessage(buffer);
			inFlightMessages.remove(pubAckMessage.getMessageId());
			handler.pubAck(this, pubAckMessage);
			break;
		case PUBREC:
			PubRecMessage pubRecMessage = new PubRecMessage(buffer);
			inFlightMessages.remove(pubRecMessage.getMessageId());
			handler.pubRec(this, pubRecMessage);
			break;
		case PUBREL:
			handler.pubRel(this, new PubRelMessage(buffer));
			break;
		case PUBCOMP:
			PubCompMessage pubCompMessage = new PubCompMessage(buffer);
			inFlightMessages.remove(pubCompMessage.getMessageId());
			handler.pubComp(this, pubCompMessage);
			break;
		case SUBSCRIBE:
			handler.subscribe(this, new SubscribeMessage(buffer, remainingLength));
			break;
		case SUBACK:
			SubAckMessage subAckMessage = new SubAckMessage(buffer, remainingLength);
			inFlightMessages.remove(subAckMessage.getMessageId());
			handler.subAck(this, subAckMessage);
			break;
		case UNSUBSCRIBE:
			handler.unsubscribe(this, new UnsubscribeMessage(buffer, remainingLength));
			break;
		case UNSUBACK:
			UnsubAckMessage unsubAckMessage = new UnsubAckMessage(buffer);
			inFlightMessages.remove(unsubAckMessage.getMessageId());
			handler.unsubAck(this, unsubAckMessage);
			break;
		case PINGREQ:
			pingReq(new PingReqMessage(buffer));
			break;
		case PINGRESP:
			pingResp(new PingRespMessage(buffer));
			break;
		case DISCONNECT:
			connected = false;
			handler.disconnect(this, new DisconnectMessage(buffer));
			break;
		default:
			throw new IllegalStateException("Unsupported message type: " + messageType);
		}
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
