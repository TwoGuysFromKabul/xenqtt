package net.sf.xenqtt.test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

abstract class AbstractNonBlockingConnectionManager {

	static final int MAX_QUEUED_MSGS_PER_CONNECTION = 1000;
	private static final int ENABLE_READ_THRESHOLD = (int) (MAX_QUEUED_MSGS_PER_CONNECTION * .75);

	private final SelectorThread[] serverThreads;
	private int nextIndex;

	public AbstractNonBlockingConnectionManager() throws IOException {
		int threadCount = Runtime.getRuntime().availableProcessors();
		this.serverThreads = new SelectorThread[threadCount];

		for (int i = 0; i < serverThreads.length; i++) {
			serverThreads[i] = new SelectorThread();
			serverThreads[i].setName("Server-" + i);
			serverThreads[i].setDaemon(true);
		}
	}

	public void start() {
		for (SelectorThread t : serverThreads) {
			t.start();
		}
	}

	public void newConnection(SocketChannel channel) throws IOException {

		channel.configureBlocking(false);
		serverThreads[nextIndex++].newConnection(channel);
		if (nextIndex == serverThreads.length) {
			nextIndex = 0;
		}
	}

	public final void send(ByteBuffer buffer) {
	}

	void channelReady(SelectionKey key, ChannelInfo info) throws IOException {
	}

	void messageReceived(SocketChannel channel, SelectionKey key, ChannelInfo info, ByteBuffer buffer) throws IOException {
	}

	void messageSent(SocketChannel channel, SelectionKey key, ChannelInfo info, ByteBuffer buffer) throws IOException {
	}

	final void close(SocketChannel channel, SelectionKey key) {
		key.cancel();
		key.attach(null);
		try {
			channel.close();
		} catch (IOException ignore) {
		}
	}

	static boolean isOpEnabled(SelectionKey key, int op) {
		return (key.interestOps() & op) == op;
	}

	static void enableOp(SelectionKey key, int op) {
		key.interestOps(key.interestOps() | op);
	}

	static void disableOp(SelectionKey key, int op) {
		key.interestOps(key.interestOps() & (~op));
	}

	final class SelectorThread extends Thread {

		private final BlockingQueue<SocketChannel> newConnections = new LinkedBlockingQueue<SocketChannel>();

		private final Selector selector;

		public SelectorThread() throws IOException {
			this.selector = Selector.open();
		}

		/**
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			for (;;) {
				try {
					selector.select();
					for (SocketChannel client = newConnections.poll(); client != null; client = newConnections.poll()) {
						ChannelInfo info = new ChannelInfo();
						SelectionKey key = client.register(selector, SelectionKey.OP_READ, info);
						channelReady(key, info);
					}

					Set<SelectionKey> keys = selector.selectedKeys();
					for (SelectionKey key : keys) {
						handleKey(key);
					}

					keys.clear();
				} catch (Exception e) {
					System.err.println("IO Failure. Exiting...");
					e.printStackTrace();
					System.exit(1);
				}
			}
		}

		private void newConnection(SocketChannel channel) {
			newConnections.add(channel);
			selector.wakeup();
		}

		private void handleKey(SelectionKey key) throws IOException {

			if (!key.isValid()) {
				return;
			}

			SocketChannel channel = (SocketChannel) key.channel();

			try {

				ChannelInfo info = (ChannelInfo) key.attachment();
				if (key.isReadable()) {
					read(channel, info, key);
				}

				if (key.isValid() && key.isWritable()) {
					write(channel, info, key);
				}

			} catch (Exception e) {
				if (channel.isOpen()) {
					System.err.println("Error on channel. Closing...");
					e.printStackTrace();
					close(channel, key);
				}
			}
		}

		private void write(SocketChannel channel, ChannelInfo info, SelectionKey key) throws IOException {

			for (;;) {

				if (info.currentSendMessage == null) {
					disableOp(key, SelectionKey.OP_WRITE);
					return;
				}

				channel.write(info.currentSendMessage);
				if (info.currentSendMessage.hasRemaining()) {
					return;
				}

				messageSent(channel, key, info, info.currentSendMessage);

				info.currentSendMessage = info.sendQueue.poll();

				if (!isOpEnabled(key, SelectionKey.OP_READ) && info.sendQueue.size() <= ENABLE_READ_THRESHOLD) {
					enableOp(key, SelectionKey.OP_READ);
				}
			}
		}

		private void read(SocketChannel channel, ChannelInfo info, SelectionKey key) throws IOException {

			for (;;) {
				if (!read(key, channel, info.receiveHeader)) {
					return;
				}

				if (info.receivePayload == null) {
					int len = info.receiveHeader.getShort(0) & 0xffff;
					info.receivePayload = ByteBuffer.allocate(len + 2);
					info.receivePayload.putShort((short) len);
				}

				if (!read(key, channel, info.receivePayload)) {
					return;
				}

				info.receivePayload.flip();
				info.messagesReceived++;
				messageReceived(channel, key, info, info.receivePayload);

				info.receiveHeader.clear();
				info.receivePayload = null;
			}
		}

		private boolean read(SelectionKey key, SocketChannel channel, ByteBuffer buffer) throws IOException {

			if (!buffer.hasRemaining()) {
				return true;
			}

			if (channel.read(buffer) < 0) {
				close(channel, key);

				return false;
			}

			return !buffer.hasRemaining();
		}
	}

	public static final class ChannelInfo {

		final Queue<ByteBuffer> sendQueue = new ArrayDeque<ByteBuffer>(NonBlockingTcpEchoServer.MAX_QUEUED_MSGS_PER_CONNECTION);
		final ByteBuffer receiveHeader = ByteBuffer.allocate(2);
		ByteBuffer receivePayload;
		ByteBuffer currentSendMessage;
		int messagesSent;
		int messagesReceived;

		public void send(ByteBuffer buffer, SelectionKey key) {

			messagesSent++;

			if (currentSendMessage == null) {
				currentSendMessage = buffer;
				enableOp(key, SelectionKey.OP_WRITE);
			} else {
				sendQueue.add(buffer);

				if (sendQueue.size() == MAX_QUEUED_MSGS_PER_CONNECTION) {
					disableOp(key, SelectionKey.OP_READ);
				}
			}
		}
	}
}
