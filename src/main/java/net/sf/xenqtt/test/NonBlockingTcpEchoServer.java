package net.sf.xenqtt.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Set;

/**
 * Server that uses non-blocking IO to accept new connections and echo any data received back to the sender. The data must be in this format:
 * <ul>
 * <li>First 2 bytes: big endian length of the remaining paylaod</li>
 * <li>Remaining bytes: payload</li>
 * </ul>
 */
public class NonBlockingTcpEchoServer {

	private final Selector selector;
	private final ServerSocketChannel ssc;
	private final int port;

	public static void main(String[] args) throws Exception {

		if (args.length != 1) {
			usage();
			System.exit(1);
		}

		int port = Integer.parseInt(args[0]);

		new NonBlockingTcpEchoServer(port).run();
	}

	private NonBlockingTcpEchoServer(int port) throws IOException {

		this.selector = Selector.open();
		this.ssc = ServerSocketChannel.open();
		this.port = port;
	}

	private static void usage() {
		System.out.println("\nUsage: java -Xms1g -Xmx1g -server -cp:xenqtt.jar net.sf.xenqtt.test.NonBlockingTcpEchoServer port");
		System.out.println("\tport: the port the server should listen on");
		System.out.println();
	}

	private void run() throws IOException {

		ssc.configureBlocking(false);
		ssc.socket().bind(new InetSocketAddress(port));
		ssc.register(selector, SelectionKey.OP_ACCEPT);

		System.out.println("Listening on port " + port);
		System.out.println("Ctrl-C to exit...\n");

		for (;;) {

			selector.select();

			Set<SelectionKey> keys = selector.selectedKeys();
			for (SelectionKey key : keys) {
				handleKey(key);
			}

			keys.clear();
		}
	}

	private void handleKey(SelectionKey key) throws IOException {

		if (!key.isValid()) {
			return;
		}

		if (key.isAcceptable()) {
			SocketChannel newChannel = ssc.accept();
			newChannel.configureBlocking(false);
			SelectionKey newKey = newChannel.register(selector, SelectionKey.OP_READ);
			newKey.attach(new ChannelInfo());
			return;
		}

		SocketChannel channel = (SocketChannel) key.channel();
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

			info.currentSendMessage = info.sendQueue.poll();
		}
	}

	private void read(SocketChannel channel, ChannelInfo info, SelectionKey key) throws IOException {

		for (;;) {
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

			if (info.currentSendMessage == null) {
				info.currentSendMessage = info.receivePayload;
				key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_READ);
			} else {
				info.sendQueue.add(info.receivePayload);
			}

			info.receiveHeader.clear();
			info.receivePayload = null;
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
	}
}
