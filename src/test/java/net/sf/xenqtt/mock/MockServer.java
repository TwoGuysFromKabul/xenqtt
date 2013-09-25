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
package net.sf.xenqtt.mock;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;

/**
 * Thread safe class that accepts connections from clients
 */
public class MockServer {

	private final ServerSocketChannel ssc;
	private final Selector selector;
	private final int port;

	public MockServer() {

		try {
			ssc = ServerSocketChannel.open();
			ssc.configureBlocking(false);

			ServerSocket serverSocket = ssc.socket();
			serverSocket.bind(new InetSocketAddress(0));
			port = serverSocket.getLocalPort();

			selector = Selector.open();

			ssc.register(selector, SelectionKey.OP_ACCEPT);
		} catch (Exception e) {
			throw new RuntimeException("crap", e);
		}
	}

	/**
	 * shuts down the server
	 */
	public void close() {
		try {
			ssc.close();
		} catch (IOException ignore) {
		}
		try {
			selector.close();
		} catch (IOException ignore) {
		}
		try {
			selector.close();
		} catch (IOException ignore) {
		}
	}

	@Override
	protected void finalize() throws Throwable {
		close();
	};

	/**
	 * @return The port the server is listening on
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @param timeoutSeconds
	 *            Seconds to wait for a client
	 * @return The new client's {@link SocketChannel}. Null if the wait times out
	 */
	public SocketChannel nextClient(int timeoutSeconds) {

		long now = System.currentTimeMillis();
		long maxMillis = (timeoutSeconds * 1000);
		long end = now + maxMillis;
		long sleepMillis = maxMillis;

		try {
			while (sleepMillis > 0) {
				selector.select(sleepMillis);
				Set<SelectionKey> keys = selector.selectedKeys();
				for (SelectionKey key : keys) {
					if (key.isAcceptable()) {
						keys.clear();
						return ssc.accept();
					}
				}
				keys.clear();
				now = System.currentTimeMillis();
				sleepMillis = end - now;
			}
		} catch (IOException e) {
			throw new RuntimeException("crap", e);
		}

		return null;
	}
}
