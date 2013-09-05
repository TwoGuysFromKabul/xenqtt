package net.sf.xenqtt.message;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
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

	private long lastReceivedTime;
	private long pingIntervalMillis;

	/**
	 * Starts an asynchronous connection to the specified host and port. When a {@link SelectionKey} for the specified selector has
	 * {@link SelectionKey#OP_CONNECT} as a ready op then {@link #finishConnect()} should be called.
	 * 
	 * @param messageResendIntervalMillis
	 *            Millis between attempts to resend a message that {@link MqttMessage#isAckable()}. 0 to disable message resends
	 */
	AbstractMqttChannel(String host, int port, MessageHandler handler, Selector selector, long messageResendIntervalMillis) throws IOException {

		this.handler = handler;
		this.messageResendIntervalMillis = messageResendIntervalMillis;

		try {
			this.channel = SocketChannel.open();
			this.channel.configureBlocking(false);
			this.selectionKey = channel.register(selector, SelectionKey.OP_CONNECT, this);
			this.channel.connect(new InetSocketAddress(host, port));
		} catch (IOException e) {
			doClose(e);
			throw e;
		} catch (RuntimeException e) {
			doClose(e);
			throw e;
		}
	}

	/**
	 * Use this constructor for clients accepted from a {@link ServerSocketChannel}.
	 * 
	 * @param messageResendIntervalMillis
	 *            Millis between attempts to resend a message that {@link MqttMessage#isAckable()}. 0 to disable message resends
	 */
	AbstractMqttChannel(SocketChannel channel, MessageHandler handler, Selector selector, long messageResendIntervalMillis) throws IOException {

		this.handler = handler;
		this.messageResendIntervalMillis = messageResendIntervalMillis;
		this.channel = channel;

		try {
			this.channel.configureBlocking(false);
			this.selectionKey = channel.register(selector, SelectionKey.OP_READ, this);
			handler.channelOpened(this);
		} catch (IOException e) {
			doClose(e);
			throw e;
		} catch (RuntimeException e) {
			doClose(e);
			throw e;
		}
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#deregister()
	 */
	@Override
	public final void deregister() {

		selectionKey.cancel();
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#register(java.nio.channels.Selector, net.sf.xenqtt.message.MessageHandler)
	 */
	@Override
	public final void register(Selector selector, MessageHandler handler) throws IOException {

		try {
			int ops = sendMessageInProgress == null ? SelectionKey.OP_READ : SelectionKey.OP_READ | SelectionKey.OP_WRITE;

			selectionKey.cancel();
			selectionKey = channel.register(selector, ops, this);
			this.handler = handler;
		} catch (IOException e) {
			doClose(e);
			throw e;
		} catch (RuntimeException e) {
			doClose(e);
			throw e;
		}
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#finishConnect()
	 */
	@Override
	public final void finishConnect() throws IOException {

		try {
			if (channel.finishConnect()) {
				int ops = sendMessageInProgress != null ? SelectionKey.OP_READ | SelectionKey.OP_WRITE : SelectionKey.OP_READ;
				selectionKey.interestOps(ops);
				handler.channelOpened(this);
			}
		} catch (IOException e) {
			doClose(e);
			throw e;
		} catch (RuntimeException e) {
			doClose(e);
			throw e;
		}
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#read(long)
	 */
	@Override
	public final boolean read(long now) throws IOException {

		try {
			if (!doRead(now)) {
				close();
				return false;
			}

			return true;
		} catch (ClosedChannelException e) {
			doClose(null);
			return false;
		} catch (IOException e) {
			doClose(e);
			throw e;
		} catch (RuntimeException e) {
			doClose(e);
			throw e;
		}
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#send(long, net.sf.xenqtt.message.MqttMessage)
	 */
	@Override
	public final void send(long now, MqttMessage message) throws IOException {

		try {
			if (sendMessageInProgress != null) {
				writesPending.offer(message);
				return;
			}

			sendMessageInProgress = message;

			if (selectionKey.isValid() && channel.socket().isConnected()) {
				selectionKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
				write(now);
			}
		} catch (IOException e) {
			doClose(e);
			throw e;
		} catch (RuntimeException e) {
			doClose(e);
			throw e;
		}
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#write(long)
	 */
	@Override
	public final boolean write(long now) throws IOException {

		try {
			if (!doWrite(now)) {
				close();
				return false;
			}

			return true;

		} catch (ClosedChannelException e) {
			doClose(null);
			return false;
		} catch (IOException e) {
			doClose(e);
			throw e;
		} catch (RuntimeException e) {
			doClose(e);
			throw e;
		}
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#close()
	 */
	@Override
	public final void close() {

		doClose(null);
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#isOpen()
	 */
	@Override
	public final boolean isOpen() {
		return channel.isOpen();
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#isConnected()
	 */
	@Override
	public final boolean isConnected() {
		return connected;
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#isConnectionPending()
	 */
	@Override
	public final boolean isConnectionPending() {
		return channel.isConnectionPending();
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#houseKeeping(long)
	 */
	@Override
	public final long houseKeeping(long now) throws IOException {

		long maxIdleTime = Long.MAX_VALUE;

		if (messageResendIntervalMillis > 0) {
			maxIdleTime = resendMessages(now);
		}

		try {
			long maxKeepAliveTime = keepAlive(now, lastReceivedTime);
			if (maxKeepAliveTime < maxIdleTime) {
				maxIdleTime = maxKeepAliveTime;
			}
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			// FIXME [jim] - log
		}

		return maxIdleTime;
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#sendQueueDepth()
	 */
	@Override
	public final int sendQueueDepth() {

		return writesPending.size();
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#inFlightMessageCount()
	 */
	@Override
	public final int inFlightMessageCount() {
		return inFlightMessages.size();
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#getUnsentMessages()
	 */
	@Override
	public List<MqttMessage> getUnsentMessages() {

		List<MqttMessage> unsentMessages = new ArrayList<MqttMessage>(inFlightMessageCount() + sendQueueDepth() + 1);
		unsentMessages.addAll(inFlightMessages.values());
		if (sendMessageInProgress != null) {
			unsentMessages.add(sendMessageInProgress);
		}
		unsentMessages.addAll(writesPending);

		return unsentMessages;
	}

	/**
	 * Called when a {@link ConnAckMessage} is sent or received where {@link ConnAckMessage#getReturnCode()} == {@link ConnectReturnCode#ACCEPTED}. This will
	 * only be called once.
	 * 
	 * @param pingIntervalMillis
	 *            The ping interval from the sent or received {@link ConnectMessage} converted to millis
	 */
	abstract void connected(long pingIntervalMillis);

	/**
	 * Called when the channel is closed. Will be called at most once and only after {@link #connected(long)}
	 */
	abstract void disconnected();

	/**
	 * Called during {@link #houseKeeping(long)} to handle keep alive (pinging)
	 * 
	 * @param now
	 *            The timestamp to use as the "current" time
	 * @param lastMessageReceived
	 *            The last time a message was received
	 * @return Maximum time in millis until keep alive will have work to do and needs to be called again. < 0 if this method closes the channel.
	 */
	abstract long keepAlive(long now, long lastMessageReceived) throws Exception;

	/**
	 * Called when a {@link PingReqMessage} is received.
	 * 
	 * @param now
	 *            The timestamp to use as the "current" time
	 */
	abstract void pingReq(long now, PingReqMessage message) throws Exception;

	/**
	 * Called when a {@link PingRespMessage} is received.
	 * 
	 * @param now
	 *            The timestamp to use as the "current" time
	 */
	abstract void pingResp(long now, PingRespMessage message) throws Exception;

	/**
	 * @return False to have the channel closed
	 */
	private boolean doWrite(long now) throws IOException {

		while (sendMessageInProgress != null) {
			int bytesWritten = channel.write(sendMessageInProgress.buffer);
			if (bytesWritten == 0 || sendMessageInProgress.buffer.hasRemaining()) {
				return true;
			}

			MessageType type = sendMessageInProgress.getMessageType();
			if (type == MessageType.DISCONNECT) {
				sendMessageInProgress = null;
				return false;
			}

			if (type == MessageType.CONNECT) {
				ConnectMessage m = (ConnectMessage) sendMessageInProgress;
				pingIntervalMillis = m.getKeepAliveSeconds() * 1000;
			}

			if (type == MessageType.CONNACK) {
				ConnAckMessage m = (ConnAckMessage) sendMessageInProgress;
				if (m.getReturnCode() != ConnectReturnCode.ACCEPTED) {
					sendMessageInProgress = null;
					return false;
				} else {
					connected = true;
					connected(pingIntervalMillis);
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

		return true;
	}

	/**
	 * @return False to have the channel closed
	 */
	private boolean doRead(long now) throws IOException {
		if (readRemaining != null) {
			return readRemaining(now);
		}

		if (readHeader1.hasRemaining()) {
			int result = channel.read(readHeader1);
			if (readHeader1.hasRemaining()) {
				return result >= 0;
			}
		}

		byte firstLenByte = readHeader1.get(1);
		if (firstLenByte == 0) {
			return processMessage(now, readHeader1);
		}

		if ((firstLenByte & 0x80) == 0) {
			return readRemaining(now);
		}

		if (readHeader2.hasRemaining()) {
			int result = channel.read(readHeader2);
			if (readHeader2.hasRemaining()) {
				return result >= 0;
			}
		}

		return readRemaining(now);
	}

	private void doClose(Throwable cause) {

		if (connected) {
			try {
				disconnected();
			} catch (Exception e) {
				// FIXME [jim] - log
			}
		}

		connected = false;

		if (selectionKey != null) {
			try {
				selectionKey.cancel();
			} catch (Exception ignore) {
			}
		}

		if (channel != null) {
			try {
				channel.close();
			} catch (Exception ignore) {
			}
		}

		try {
			handler.channelClosed(this, cause);
		} catch (Exception e) {
			// FIXME [jim] - log or something
		}
	}

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

	/**
	 * @return False to have the channel closed
	 */
	private boolean processMessage(long now, ByteBuffer buffer) {

		lastReceivedTime = now;

		buffer.flip();

		boolean result = true;
		try {
			result = handleMessage(now, buffer);
		} catch (Exception e) {
			// result = isOpen();
			e.printStackTrace();
			// FIXME [jim] - need to log this or something
		}

		readHeader1.clear();
		readHeader2.clear();
		readRemaining = null;
		remainingLength = 0;

		return result;
	}

	/**
	 * @return False to have the channel closed
	 */
	private boolean handleMessage(long now, ByteBuffer buffer) throws Exception {

		boolean result = true;
		MessageType messageType = MessageType.lookup((buffer.get(0) & 0xf0) >> 4);
		switch (messageType) {
		case CONNECT:
			ConnectMessage connectMessage = new ConnectMessage(buffer, remainingLength);
			pingIntervalMillis = connectMessage.getKeepAliveSeconds() * 1000;
			handler.connect(this, connectMessage);
			break;
		case CONNACK:
			ConnAckMessage connAckMessage = new ConnAckMessage(buffer);
			result = connected = connAckMessage.getReturnCode() == ConnectReturnCode.ACCEPTED;
			if (connected) {
				connected(pingIntervalMillis);
			}
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
			pingReq(now, new PingReqMessage(buffer));
			break;
		case PINGRESP:
			pingResp(now, new PingRespMessage(buffer));
			break;
		case DISCONNECT:
			result = false;
			handler.disconnect(this, new DisconnectMessage(buffer));
			break;
		default:
			throw new IllegalStateException("Unsupported message type: " + messageType);
		}

		return result;
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

	/**
	 * @return False to have the channel closed
	 */
	private boolean readRemaining(long now) throws IOException {

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

		return processMessage(now, readRemaining);
	}
}
