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
package net.sf.xenqtt.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Server that uses non-blocking IO to accept new connections and echo any data received back to the sender. The data must be in this format:
 * <ul>
 * <li>First 2 bytes: big endian length of the remaining paylaod</li>
 * <li>Remaining bytes: payload</li>
 * </ul>
 */
public class NonBlockingTcpEchoServer extends AbstractNonBlockingConnectionManager {

	private final int port;

	public static void main(String[] args) throws Exception {

		if (args.length != 2) {
			usage();
			System.exit(1);
		}

		int port = Integer.parseInt(args[0]);
		int threadsPerCore = Integer.parseInt(args[1]);

		new NonBlockingTcpEchoServer(port, threadsPerCore).run();
	}

	private NonBlockingTcpEchoServer(int port, int threadsPerCore) throws IOException {
		super(threadsPerCore);
		this.port = port;
	}

	private static void usage() {
		System.out.println("\nUsage: java -Xms1g -Xmx1g -server -cp:xenqtt.jar net.sf.xenqtt.test.NonBlockingTcpEchoServer port threadsPerCore");
		System.out.println("\tport: the port the server should listen on");
		System.out.println("\tthreadsPerCore: the number of threads to use per cpu core (0 for one thread for the app)");
		System.out.println();
	}

	@Override
	void messageReceived(SocketChannel channel, SelectionKey key, ChannelInfo info, ByteBuffer buffer) throws IOException {
		info.send(buffer, key);
	}

	private void run() throws IOException {

		start();

		ServerSocketChannel ssc = ServerSocketChannel.open();
		ssc.socket().bind(new InetSocketAddress(port));

		System.out.println("Listening on port " + port);
		System.out.println("Ctrl-C to exit...\n");

		for (;;) {
			SocketChannel channel = ssc.accept();
			newConnection(channel);
		}
	}
}
