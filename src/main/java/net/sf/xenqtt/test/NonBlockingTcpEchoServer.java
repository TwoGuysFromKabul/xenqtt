package net.sf.xenqtt.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Server that uses non-blocking IO to accept new connections and echo any data received back to the sender. The data must be in this format:
 * <ul>
 * <li>First 2 bytes: big endian length of the remaining paylaod</li>
 * <li>Remaining bytes: payload</li>
 * </ul>
 */
public class NonBlockingTcpEchoServer {

	static final int MAX_QUEUED_MSGS_PER_CONNECTION = 1000;

	private final NonBlockingServerThread[] serverThreads;
	private final int port;

	private int nextIndex;

	public static void main(String[] args) throws Exception {

		if (args.length != 1) {
			usage();
			System.exit(1);
		}

		int port = Integer.parseInt(args[0]);

		new NonBlockingTcpEchoServer(port).run();
	}

	private NonBlockingTcpEchoServer(int port) throws IOException {

		this.port = port;
		int threadCount = Runtime.getRuntime().availableProcessors();
		this.serverThreads = new NonBlockingServerThread[threadCount];
	}

	private static void usage() {
		System.out.println("\nUsage: java -Xms1g -Xmx1g -server -cp:xenqtt.jar net.sf.xenqtt.test.NonBlockingTcpEchoServer port");
		System.out.println("\tport: the port the server should listen on");
		System.out.println();
	}

	private void run() throws IOException {

		for (int i = 0; i < serverThreads.length; i++) {
			serverThreads[i] = new NonBlockingServerThread();
			serverThreads[i].setName("Server-" + i);
			serverThreads[i].setDaemon(true);
			serverThreads[i].start();
		}

		ServerSocketChannel ssc = ServerSocketChannel.open();
		ssc.socket().bind(new InetSocketAddress(port));

		System.out.println("Listening on port " + port);
		System.out.println("Ctrl-C to exit...\n");

		for (;;) {
			SocketChannel channel = ssc.accept();
			channel.configureBlocking(false);
			serverThreads[nextIndex++].newClient(channel);
			if (nextIndex == serverThreads.length) {
				nextIndex = 0;
			}
		}

	}
}
