/**
    Copyright 2013 James McClure

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package net.sf.xenqtt.message;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
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

import net.sf.xenqtt.Log;

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

	private BlockingCommand<?> connectionCompleteCommand;
	private BlockingCommand<MqttMessage> connAckReceivedCommand;

	private MqttMessage sendMessageInProgress;

	private boolean connected;

	private long lastReceivedTime;
	private long pingIntervalMillis;

	private boolean channelCloseCalled;

	/**
	 * Starts an asynchronous connection to the specified host and port. When a {@link SelectionKey} for the specified selector has
	 * {@link SelectionKey#OP_CONNECT} as a ready op then {@link #finishConnect()} should be called.
	 * 
	 * @param messageResendIntervalMillis
	 *            Millis between attempts to resend a message that {@link MqttMessage#isAckable()}. 0 to disable message resends
	 * @param connectionCompleteCommand
	 *            If not null then this latch is {@link BlockingCommand#complete(Throwable) complete} when the connection is established.
	 */
	AbstractMqttChannel(String host, int port, MessageHandler handler, Selector selector, long messageResendIntervalMillis,
			BlockingCommand<?> connectionCompleteCommand) throws IOException {

		this.handler = handler;
		this.messageResendIntervalMillis = messageResendIntervalMillis;
		this.connectionCompleteCommand = connectionCompleteCommand;

		try {
			this.channel = SocketChannel.open();
			this.channel.configureBlocking(false);
			this.selectionKey = channel.register(selector, SelectionKey.OP_CONNECT, this);
			if (this.channel.connect(new InetSocketAddress(host, port))) {
				connectFinished();
			}
			Log.debug("%s connecting to %s:%s", this, host, port);
		} catch (IOException e) {
			doClose(e, "Failed to connect a client MQTT channel to %s:%d", host, port);
			throw e;
		} catch (RuntimeException e) {
			doClose(e, "Failed to connect a client MQTT channel to %s:%d", host, port);
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
		this.connectionCompleteCommand = null;
		this.channel = channel;

		try {
			this.channel.configureBlocking(false);
			this.selectionKey = channel.register(selector, SelectionKey.OP_READ, this);
			handler.channelOpened(this);
			Log.debug("%s opened", this);
		} catch (IOException e) {
			doClose(e, "Failed to construct a broker MQTT channel");
			throw e;
		} catch (RuntimeException e) {
			doClose(e, "Failed to construct a broker MQTT channel");
			throw e;
		}
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#deregister()
	 */
	@Override
	public final void deregister() {

		selectionKey.cancel();
		Log.debug("Deregistered %s", this);
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#register(java.nio.channels.Selector, net.sf.xenqtt.message.MessageHandler)
	 */
	@Override
	public final boolean register(Selector selector, MessageHandler handler) {

		try {
			int ops = sendMessageInProgress == null ? SelectionKey.OP_READ : SelectionKey.OP_READ | SelectionKey.OP_WRITE;

			selectionKey.cancel();
			selectionKey = channel.register(selector, ops, this);
			this.handler = handler;

			Log.debug("Registered %s with selector %s", this);
			return true;

		} catch (Exception e) {
			doClose(e, "Failed to register selector for %s", this);
		}

		return false;
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#finishConnect()
	 */
	@Override
	public final boolean finishConnect() {

		try {
			if (channel.finishConnect()) {
				connectFinished();
				Log.debug("%s finished connecting", this);
				return true;
			}
		} catch (Exception e) {
			doClose(e, "Failed to connect %s", this);
		}

		return false;
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#read(long)
	 */
	@Override
	public final boolean read(long now) {

		try {
			if (doRead(now)) {
				return true;
			}

			close();

		} catch (ClosedChannelException e) {
			close();
		} catch (Exception e) {
			doClose(e, "Failed to read from %s", this);
		}

		return false;
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#send(net.sf.xenqtt.message.MqttMessage, net.sf.xenqtt.message.BlockingCommand)
	 */
	@Override
	public boolean send(MqttMessage message, BlockingCommand<MqttMessage> blockingCommand) {

		if (message.getMessageType() == MessageType.CONNECT) {
			connAckReceivedCommand = blockingCommand;
		} else {
			message.blockingCommand = blockingCommand;
		}

		return doSend(message);
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#write(long)
	 */
	@Override
	public final boolean write(long now) {

		try {
			if (doWrite(now)) {
				return true;
			}

			close();

		} catch (ClosedChannelException e) {
			close();
		} catch (Exception e) {
			doClose(e, "Failed to write to %s", this);
		}

		return false;
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#close()
	 */
	@Override
	public final void close() {

		doClose(null, null);
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
	public final long houseKeeping(long now) {

		long maxIdleTime = Long.MAX_VALUE;

		if (messageResendIntervalMillis > 0) {
			try {
				maxIdleTime = resendMessages(now);
			} catch (Exception e) {
				Log.error(e, "Failed to resend unacknowledged messages for %s", this);
			}
		}

		try {
			long maxKeepAliveTime = keepAlive(now, lastReceivedTime);
			if (maxKeepAliveTime < maxIdleTime) {
				maxIdleTime = maxKeepAliveTime;
			}
		} catch (Exception e) {
			Log.error(e, "Failed to handle the keep alive protocol for %s", this);
		}

		return maxIdleTime;
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#sendQueueDepth()
	 */
	@Override
	public final int sendQueueDepth() {

		return sendMessageInProgress == null ? writesPending.size() : writesPending.size() + 1;
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#inFlightMessageCount()
	 */
	@Override
	public final int inFlightMessageCount() {
		return inFlightMessages.size();
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#cancelBlockingCommands()
	 */
	@Override
	public void cancelBlockingCommands() {

		cancelCommand(connectionCompleteCommand);
		cancelCommand(connAckReceivedCommand);
		if (sendMessageInProgress != null) {
			cancelCommand(sendMessageInProgress.blockingCommand);
		}
		for (MqttMessage message : writesPending) {
			cancelCommand(message.blockingCommand);
		}
		for (MqttMessage message : messagesToResend) {
			cancelCommand(message.blockingCommand);
		}
		for (MqttMessage message : inFlightMessages.values()) {
			cancelCommand(message.blockingCommand);
		}
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#getUnsentMessages()
	 */
	@Override
	public List<MqttMessage> getUnsentMessages() {

		List<MqttMessage> unsentMessages = new ArrayList<MqttMessage>(messagesToResend.size() + inFlightMessageCount() + sendQueueDepth());
		unsentMessages.addAll(messagesToResend);
		unsentMessages.addAll(inFlightMessages.values());
		if (sendMessageInProgress != null) {
			unsentMessages.add(sendMessageInProgress);
		}
		unsentMessages.addAll(writesPending);

		return unsentMessages;
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannel#getRemoteAddress()
	 */
	@Override
	public String getRemoteAddress() {

		Socket socket = channel.socket();
		return socket.isBound() ? socket.getRemoteSocketAddress().toString() : "";
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		Socket socket = channel.socket();
		if (!socket.isBound()) {
			return getClass().getSimpleName() + "[localAddress:N/A,remoteAddress:N/A]";
		}
		if (!channel.isOpen()) {
			return getClass().getSimpleName() + "[localAddress:N/A,remoteAddress:" + socket.getRemoteSocketAddress() + "]";
		}
		return getClass().getSimpleName() + "[localAddress:" + socket.getLocalSocketAddress() + ",remoteAddress:" + socket.getRemoteSocketAddress() + "]";
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

	private void connectFinished() {
		int ops = sendMessageInProgress != null ? SelectionKey.OP_READ | SelectionKey.OP_WRITE : SelectionKey.OP_READ;
		selectionKey.interestOps(ops);
		commandComplete(connectionCompleteCommand);
		connectionCompleteCommand = null;
		handler.channelOpened(this);
	}

	private boolean doSend(MqttMessage message) {

		try {
			message.buffer.rewind();

			Log.debug("%s sending %s", this, message);
			if (sendMessageInProgress != null) {
				writesPending.offer(message);
				return true;
			}

			sendMessageInProgress = message;

			if (selectionKey.isValid() && channel.socket().isConnected()) {
				selectionKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
				return true;
			}
		} catch (Exception e) {
			doClose(e, "Failed to send to %s", this);
		}

		return false;
	}

	/**
	 * @return False to have the channel closed
	 */
	private boolean doWrite(long now) throws IOException {

		while (sendMessageInProgress != null) {
			int bytesWritten = channel.write(sendMessageInProgress.buffer);
			if (bytesWritten == 0 || sendMessageInProgress.buffer.hasRemaining()) {
				return true;
			}

			Log.debug("%s sent %s", this, sendMessageInProgress);

			MessageType type = sendMessageInProgress.getMessageType();
			if (type == MessageType.DISCONNECT) {
				commandComplete(sendMessageInProgress.blockingCommand);
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

			boolean ackable = sendMessageInProgress.isAckable();
			if (ackable && messageResendIntervalMillis > 0) {
				IdentifiableMqttMessage m = (IdentifiableMqttMessage) sendMessageInProgress;
				m.nextSendTime = now + messageResendIntervalMillis;
				inFlightMessages.put(m.getMessageId(), m);
			}

			if (!ackable && type != MessageType.CONNECT) {
				commandComplete(sendMessageInProgress.blockingCommand);
			}

			sendMessageInProgress = writesPending.poll();
		}

		if (!selectionKey.isValid()) {
			return isOpen();
		}

		selectionKey.interestOps(SelectionKey.OP_READ);

		return true;
	}

	private void setFailureOnCommand(BlockingCommand<?> blockingCommand, Throwable cause) {
		if (blockingCommand != null) {
			blockingCommand.setFailureCause(cause);
		}
	}

	private void cancelCommand(BlockingCommand<?> blockingCommand) {
		if (blockingCommand != null) {
			blockingCommand.cancel();
		}
	}

	private void commandComplete(BlockingCommand<?> blockingCommand) {
		if (blockingCommand != null) {
			blockingCommand.complete();
		}
	}

	private void commandComplete(BlockingCommand<MqttMessage> blockingCommand, MqttMessage ack) {
		if (blockingCommand != null) {
			blockingCommand.setResult(ack);
			blockingCommand.complete();
		}
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

	private void setFailureOnBlockingCommands(Throwable cause) {

		setFailureOnCommand(connectionCompleteCommand, cause);
		setFailureOnCommand(connAckReceivedCommand, cause);
		if (sendMessageInProgress != null) {
			setFailureOnCommand(sendMessageInProgress.blockingCommand, cause);
		}
		for (MqttMessage message : writesPending) {
			setFailureOnCommand(message.blockingCommand, cause);
		}
		for (MqttMessage message : messagesToResend) {
			setFailureOnCommand(message.blockingCommand, cause);
		}
		for (MqttMessage message : inFlightMessages.values()) {
			setFailureOnCommand(message.blockingCommand, cause);
		}
	}

	private void doClose(Throwable cause, String messageFormat, Object... args) {

		if (cause != null) {
			Log.error(cause, messageFormat, args);
		}

		// FIXME [jim] - test setting failure
		if (cause != null) {
			setFailureOnBlockingCommands(cause);
		}

		if (channelCloseCalled) {
			return;
		}

		Log.debug("Closing %s", this);
		channelCloseCalled = true;

		if (connected) {
			try {
				disconnected();
			} catch (Exception e) {
				Log.error(e, "Disconnect method failed for %s", this);
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
			Log.error(e, "Message handler failed in channelClosed for %s", this);
		}
	}

	private long resendMessages(long now) {

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

		if (!messagesToResend.isEmpty()) {
			Log.debug("%s resending %d messages", this, messagesToResend.size());

			for (IdentifiableMqttMessage msg : messagesToResend) {
				msg.setDuplicateFlag();
				doSend(msg);
			}

			messagesToResend.clear();
		}

		return maxIdleTime;
	}

	/**
	 * @return False to have the channel closed
	 */
	private boolean processMessage(long now, ByteBuffer buffer) {

		lastReceivedTime = now;

		buffer.flip();

		boolean result = handleMessage(now, buffer);

		readHeader1.clear();
		readHeader2.clear();
		readRemaining = null;
		remainingLength = 0;

		return result;
	}

	/**
	 * @return False to have the channel closed
	 */
	private boolean handleMessage(long now, ByteBuffer buffer) {

		boolean result = true;
		MqttMessage msg = null;
		try {
			MessageType messageType = MessageType.lookup((buffer.get(0) & 0xf0) >> 4);
			switch (messageType) {
			case CONNECT:
				ConnectMessage connectMessage = new ConnectMessage(buffer, remainingLength);
				msg = connectMessage;
				pingIntervalMillis = connectMessage.getKeepAliveSeconds() * 1000;
				handler.connect(this, connectMessage);
				break;
			case CONNACK:
				ConnAckMessage connAckMessage = new ConnAckMessage(buffer);
				commandComplete(connAckReceivedCommand, connAckMessage);
				connAckReceivedCommand = null;
				msg = connAckMessage;
				result = connected = connAckMessage.getReturnCode() == ConnectReturnCode.ACCEPTED;
				if (connected) {
					connected(pingIntervalMillis);
				}
				handler.connAck(this, connAckMessage);
				break;
			case PUBLISH:
				PubMessage publishMessage = new PubMessage(buffer, remainingLength);
				msg = publishMessage;
				handler.publish(this, publishMessage);
				break;
			case PUBACK:
				PubAckMessage pubAckMessage = new PubAckMessage(buffer);
				msg = pubAckMessage;
				ackReceived(pubAckMessage);
				handler.pubAck(this, pubAckMessage);
				break;
			case PUBREC:
				PubRecMessage pubRecMessage = new PubRecMessage(buffer);
				msg = pubRecMessage;
				ackReceived(pubRecMessage);
				handler.pubRec(this, pubRecMessage);
				break;
			case PUBREL:
				PubRelMessage pubRelMessage = new PubRelMessage(buffer);
				msg = pubRelMessage;
				handler.pubRel(this, pubRelMessage);
				break;
			case PUBCOMP:
				PubCompMessage pubCompMessage = new PubCompMessage(buffer);
				msg = pubCompMessage;
				ackReceived(pubCompMessage);
				handler.pubComp(this, pubCompMessage);
				break;
			case SUBSCRIBE:
				SubscribeMessage subscribeMessage = new SubscribeMessage(buffer, remainingLength);
				msg = subscribeMessage;
				handler.subscribe(this, subscribeMessage);
				break;
			case SUBACK:
				SubAckMessage subAckMessage = new SubAckMessage(buffer, remainingLength);
				msg = subAckMessage;
				ackReceived(subAckMessage);
				handler.subAck(this, subAckMessage);
				break;
			case UNSUBSCRIBE:
				UnsubscribeMessage unsubscribeMessage = new UnsubscribeMessage(buffer, remainingLength);
				msg = unsubscribeMessage;
				handler.unsubscribe(this, unsubscribeMessage);
				break;
			case UNSUBACK:
				UnsubAckMessage unsubAckMessage = new UnsubAckMessage(buffer);
				msg = unsubAckMessage;
				ackReceived(unsubAckMessage);
				handler.unsubAck(this, unsubAckMessage);
				break;
			case PINGREQ:
				PingReqMessage pingReqMessage = new PingReqMessage(buffer);
				msg = pingReqMessage;
				pingReq(now, pingReqMessage);
				break;
			case PINGRESP:
				PingRespMessage pingRespMessage = new PingRespMessage(buffer);
				msg = pingRespMessage;
				pingResp(now, pingRespMessage);
				break;
			case DISCONNECT:
				result = false;
				DisconnectMessage disconnectMessage = new DisconnectMessage(buffer);
				msg = disconnectMessage;
				handler.disconnect(this, disconnectMessage);
				break;
			default:
				throw new IllegalStateException("Unsupported message type: " + messageType);
			}

			Log.debug("%s received %s", this, msg);

		} catch (Exception e) {

			if (msg != null) {
				Log.error(e, "Failed to process message for %s: %s", this, msg);
			} else {
				Log.error("Failed to parse message for %s: %s", this, MqttMessage.byteBufferToHex(buffer));
			}
			result = isOpen();
		}

		return result;
	}

	private void ackReceived(IdentifiableMqttMessage ackMessage) {

		IdentifiableMqttMessage ackedMessage = inFlightMessages.remove(ackMessage.getMessageId());
		if (ackedMessage != null) {
			commandComplete(ackedMessage.blockingCommand, ackMessage);
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
