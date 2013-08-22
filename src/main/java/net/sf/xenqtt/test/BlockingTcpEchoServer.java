package net.sf.xenqtt.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Server that uses blocking IO to accept new connections and echo any data received back to the sender. The data must be in this format:
 * <ul>
 * <li>First 2 bytes: big endian length of the remaining paylaod</li>
 * <li>Remaining bytes: payload</li>
 * </ul>
 */
public final class BlockingTcpEchoServer {

	private final int port;

	public static void main(String[] args) throws Exception {

		if (args.length != 1) {
			usage();
			System.exit(1);
		}

		int port = Integer.parseInt(args[0]);

		new BlockingTcpEchoServer(port).run();
	}

	private BlockingTcpEchoServer(int port) {
		this.port = port;
	}

	private static void usage() {
		System.out.println("\nUsage: java -Xms1g -Xmx1g -server -cp:xenqtt.jar net.sf.xenqtt.test.BlockingTcpEchoServer port");
		System.out.println("\tport: the port the server should listen on");
		System.out.println();
	}

	private void run() throws IOException {

		ServerSocketChannel ssc = ServerSocketChannel.open();
		ssc.socket().bind(new InetSocketAddress(port));

		System.out.println("Listening on port " + port);
		System.out.println("Ctrl-C to exit...\n");

		for (;;) {
			SocketChannel channel = ssc.accept();
			new BlockingServerConnection(channel).start();
		}
	}

	private static final class BlockingServerConnection extends AbstractBlockingConnection {

		public BlockingServerConnection(SocketChannel channel) {
			super(channel);
		}

		/**
		 * @see net.sf.xenqtt.test.AbstractBlockingConnection#messageReceived(java.nio.ByteBuffer)
		 */
		@Override
		void messageReceived(ByteBuffer buffer) {
			send(buffer);
		}
	}
}
