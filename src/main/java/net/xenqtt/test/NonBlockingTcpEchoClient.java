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
package net.xenqtt.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This client sends data to a TCP echo server using blocking IO. Data is in the following format:
 * <ul>
 * <li>First 2 bytes: big endian length of the remaining paylaod</li>
 * <li>Remaining bytes: payload</li>
 * </ul>
 */
public final class NonBlockingTcpEchoClient extends AbstractNonBlockingConnectionManager {

	private final SocketAddress address;
	private final String host;
	private final int port;
	private final int connectionCount;
	private final int messagesPerConnection;
	private final byte[] message;
	private final CountDownLatch messagesToReceive;

	private final AtomicInteger totalMessagesReceived = new AtomicInteger();

	public static void main(String[] args) throws Exception {

		if (args.length != 6) {
			usage();
			System.exit(1);
		}

		String host = args[0];
		int port = Integer.parseInt(args[1]);
		int threadsPerCore = Integer.parseInt(args[2]);
		int connectionCount = Integer.parseInt(args[3]);
		int messagesPerConnection = Integer.parseInt(args[4]);
		int messageSize = Integer.parseInt(args[5]);

		new NonBlockingTcpEchoClient(host, port, connectionCount, messagesPerConnection, messageSize, threadsPerCore).run();
	}

	@Override
	void channelReady(SelectionKey key, ChannelInfo info) {

		int count = Math.min(50, messagesPerConnection);
		for (int j = 0; j < count; j++) {
			ByteBuffer buffer = ByteBuffer.allocate(message.length + 2);
			buffer.putShort((short) message.length);
			buffer.put(message);
			buffer.flip();
			info.send(buffer, key);
		}
	}

	@Override
	void messageSent(SocketChannel channel, SelectionKey key, ChannelInfo info, ByteBuffer buffer) {
		if (info.messagesSent < messagesPerConnection) {
			buffer.clear();
			info.send(buffer, key);
		}
	}

	@Override
	void messageReceived(SocketChannel channel, SelectionKey key, ChannelInfo info, ByteBuffer buffer) {

		totalMessagesReceived.incrementAndGet();

		if (info.messagesReceived >= messagesPerConnection) {
			close(channel, key);
		}

		messagesToReceive.countDown();
	}

	private NonBlockingTcpEchoClient(String host, int port, int connectionCount, int messagesPerConnection, int messageSize, int threadsPerCore)
			throws IOException {
		super(threadsPerCore);
		this.host = host;
		this.port = port;
		this.connectionCount = connectionCount;
		this.messagesPerConnection = messagesPerConnection;
		this.address = new InetSocketAddress(host, port);
		this.message = new byte[messageSize];
		Arrays.fill(message, (byte) 9);

		this.messagesToReceive = new CountDownLatch(connectionCount * messagesPerConnection);
	}

	private static void usage() {
		System.out
				.println("\nUsage: java -Xms1g -Xmx1g -server -cp:xenqtt.jar net.sf.xenqtt.test.NonBlockingTcpEchoClient host port threadsPerCore connectionCount messagesPerConnection messageSize");
		System.out.println("\thost: the host the server is listening on");
		System.out.println("\tport: the port the server is listening on");
		System.out.println("\tthreadsPerCore: the number of threads to use per cpu core (0 to use a single thread for the app)");
		System.out.println("\tconnectionCount: the number of connections to make to the server");
		System.out.println("\tmessagesPerConnection: the number of messages for each connection to send to the server");
		System.out.println("\tmessageSize: the size, in bytes, of each message");
		System.out.println();
	}

	private void run() throws Exception {

		System.out.printf("Establishing %d connections to %s:%d...\n", connectionCount, host, port);

		start();

		long start = System.currentTimeMillis();

		for (int i = 0; i < connectionCount; i++) {
			SocketChannel channel = SocketChannel.open(address);
			newConnection(channel);
		}

		System.out.printf("Waiting for all messages to be sent: %d messages/connection * %d connections = %d messages\n", messagesPerConnection,
				connectionCount, messagesPerConnection * connectionCount);

		messagesToReceive.await();

		long end = System.currentTimeMillis();

		System.out.println("Messages sent/received: " + totalMessagesReceived.get());
		System.out.println("Elapsed millis: " + (end - start));
	}
}
