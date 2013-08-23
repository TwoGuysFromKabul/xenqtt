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

final class NonBlockingServerThread extends Thread {

	private final int enableReadThreshold = (int) (NonBlockingTcpEchoServer.MAX_QUEUED_MSGS_PER_CONNECTION * .75);

	private final BlockingQueue<SocketChannel> newClients = new LinkedBlockingQueue<SocketChannel>();

	private final Selector selector;

	public NonBlockingServerThread() throws IOException {
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
				for (SocketChannel client = newClients.poll(); client != null; client = newClients.poll()) {
					client.register(selector, SelectionKey.OP_READ, new ChannelInfo());
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

	public void newClient(SocketChannel channel) {
		newClients.add(channel);
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
				key.cancel();
				key.attach(null);
				channel.close();
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

			if (!isOpEnabled(key, SelectionKey.OP_READ) && info.sendQueue.size() <= enableReadThreshold) {
				enableOp(key, SelectionKey.OP_READ);
			}
			info.currentSendMessage = info.sendQueue.poll();
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

			if (info.currentSendMessage == null) {
				info.currentSendMessage = info.receivePayload;
				enableOp(key, SelectionKey.OP_WRITE);
			} else {
				info.sendQueue.add(info.receivePayload);
				if (info.sendQueue.size() == NonBlockingTcpEchoServer.MAX_QUEUED_MSGS_PER_CONNECTION) {
					disableOp(key, SelectionKey.OP_READ);
				}
			}

			info.receiveHeader.clear();
			info.receivePayload = null;
		}
	}

	private boolean read(SelectionKey key, SocketChannel channel, ByteBuffer buffer) throws IOException {

		if (!buffer.hasRemaining()) {
			return true;
		}

		if (channel.read(buffer) < 0) {
			key.cancel();
			key.attach(null);
			channel.close();

			return false;
		}

		return !buffer.hasRemaining();
	}

	private boolean isOpEnabled(SelectionKey key, int op) {
		return (key.interestOps() & op) == op;
	}

	private void enableOp(SelectionKey key, int op) {
		key.interestOps(key.interestOps() | op);
	}

	private void disableOp(SelectionKey key, int op) {
		key.interestOps(key.interestOps() & (~op));
	}

	private static final class ChannelInfo {

		final Queue<ByteBuffer> sendQueue = new ArrayDeque<ByteBuffer>(NonBlockingTcpEchoServer.MAX_QUEUED_MSGS_PER_CONNECTION);
		final ByteBuffer receiveHeader = ByteBuffer.allocate(2);
		ByteBuffer receivePayload;
		ByteBuffer currentSendMessage;
	}
}
