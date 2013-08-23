package net.sf.xenqtt.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;
import java.util.Set;

/**
 * This client sends data to a TCP echo server using blocking IO. Data is in the following format:
 * <ul>
 * <li>First 2 bytes: big endian length of the remaining paylaod</li>
 * <li>Remaining bytes: payload</li>
 * </ul>
 */
public final class NonBlockingTcpEchoClient {

	private final SocketAddress address;
	private final Selector selector;
	private final String host;
	private final int port;
	private final int connectionCount;
	private final int messagesPerConnection;
	private final byte[] message;

	private int totalMessagesReceived;
	private int connectionsOpened;
	private int connectionsClosed;

	public static void main(String[] args) throws Exception {

		if (args.length != 5) {
			usage();
			System.exit(1);
		}

		String host = args[0];
		int port = Integer.parseInt(args[1]);
		int connectionCount = Integer.parseInt(args[2]);
		int messagesPerConnection = Integer.parseInt(args[3]);
		int messageSize = Integer.parseInt(args[4]);

		new NonBlockingTcpEchoClient(host, port, connectionCount, messagesPerConnection, messageSize).run();
	}

	private NonBlockingTcpEchoClient(String host, int port, int connectionCount, int messagesPerConnection, int messageSize) throws IOException {
		this.host = host;
		this.port = port;
		this.connectionCount = connectionCount;
		this.messagesPerConnection = messagesPerConnection;
		this.selector = Selector.open();
		this.address = new InetSocketAddress(host, port);
		this.message = new byte[messageSize];
		Arrays.fill(message, (byte) 9);
	}

	private static void usage() {
		System.out
				.println("\nUsage: java -Xms1g -Xmx1g -server -cp:xenqtt.jar net.sf.xenqtt.test.NonBlockingTcpEchoClient host port connectionCount messagesPerConnection messageSize");
		System.out.println("\thost: the host the server is listening on");
		System.out.println("\tport: the port the server is listening on");
		System.out.println("\tconnectionCount: the number of connections to make to the server");
		System.out.println("\tmessagesPerConnection: the number of messages for each connection to send to the server");
		System.out.println("\tmessageSize: the size, in bytes, of each message");
		System.out.println();
	}

	private void run() throws IOException {

		System.out.printf(
				"Establishing %d connections to %s:%d and waiting for all messages to be sent: %d messages/connection * %d connections = %d messages\n",
				connectionCount, host, port, messagesPerConnection, connectionCount, messagesPerConnection * connectionCount);

		long start = System.currentTimeMillis();

		openConnection();

		while (connectionsOpened < connectionCount || connectionsClosed < connectionCount) {

			selector.select();

			Set<SelectionKey> keys = selector.selectedKeys();
			for (SelectionKey key : keys) {
				handleKey(key);
			}

			keys.clear();
		}

		long end = System.currentTimeMillis();

		System.out.println("Messages sent/received: " + totalMessagesReceived);
		System.out.println("Elapsed millis: " + (end - start));
	}

	private void openConnection() throws IOException {

		SocketChannel channel = SocketChannel.open();
		channel.configureBlocking(false);
		SelectionKey newKey = channel.register(selector, SelectionKey.OP_CONNECT);
		ChannelInfo info = new ChannelInfo();
		newKey.attach(info);
		info.messagesSent = Math.min(50, messagesPerConnection);
		for (int j = 0; j < info.messagesSent; j++) {
			ByteBuffer buffer = ByteBuffer.allocate(message.length + 2);
			buffer.putShort((short) message.length);
			buffer.put(message);
			buffer.flip();
			info.sendQueue.add(buffer);
		}
		info.currentSendMessage = info.sendQueue.poll();

		channel.connect(address);
		connectionsOpened++;
	}

	private void handleKey(SelectionKey key) throws IOException {

		if (!key.isValid()) {
			return;
		}

		SocketChannel channel = (SocketChannel) key.channel();
		if (key.isConnectable()) {
			channel.finishConnect();
			key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
			openConnection();
			return;
		}

		ChannelInfo info = (ChannelInfo) key.attachment();
		if (key.isReadable()) {
			read(channel, info, key);
		}

		if (key.isValid() && key.isWritable()) {
			write(channel, info, key);
		}
	}

	private void write(SocketChannel channel, ChannelInfo info, SelectionKey key) throws IOException {

		for (;;) {

			if (info.currentSendMessage == null) {
				key.interestOps(SelectionKey.OP_READ);
				return;
			}

			channel.write(info.currentSendMessage);
			if (info.currentSendMessage.hasRemaining()) {
				return;
			}

			if (info.messagesSent++ < messagesPerConnection) {
				info.currentSendMessage.clear();
				info.sendQueue.add(info.currentSendMessage);
				key.interestOps(SelectionKey.OP_READ);
				return;
			}

			info.currentSendMessage = info.sendQueue.poll();
			if (info.currentSendMessage == null) {
				key.interestOps(SelectionKey.OP_READ);
			}
		}
	}

	private void read(SocketChannel channel, ChannelInfo info, SelectionKey key) throws IOException {

		for (;;) {
			if (!key.isValid()) {
				return;
			}

			if (!read(channel, info.receiveHeader)) {
				return;
			}

			if (info.receivePayload == null) {
				int len = info.receiveHeader.getShort(0) & 0xffff;
				info.receivePayload = ByteBuffer.allocate(len + 2);
				info.receivePayload.putShort((short) len);
			}

			if (!read(channel, info.receivePayload)) {
				return;
			}

			info.receivePayload.flip();

			totalMessagesReceived++;
			info.messagesReceived++;
			info.receiveHeader.clear();
			info.receivePayload = null;

			if (info.messagesReceived >= messagesPerConnection) {
				key.cancel();
				key.attach(null);
				channel.close();
				connectionsClosed++;
			}
		}
	}

	private boolean read(SocketChannel channel, ByteBuffer buffer) throws IOException {

		if (!buffer.hasRemaining()) {
			return true;
		}

		if (channel.read(buffer) < 0) {
			channel.close();
			return false;
		}

		return !buffer.hasRemaining();
	}

	private static final class ChannelInfo {

		final Queue<ByteBuffer> sendQueue = new ArrayDeque<ByteBuffer>();
		final ByteBuffer receiveHeader = ByteBuffer.allocate(2);
		ByteBuffer receivePayload;
		ByteBuffer currentSendMessage;
		int messagesSent;
		int messagesReceived;
	}
}
