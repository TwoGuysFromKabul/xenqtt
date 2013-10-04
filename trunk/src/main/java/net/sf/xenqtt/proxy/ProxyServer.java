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
package net.sf.xenqtt.proxy;

import java.net.InetSocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import net.sf.xenqtt.AppContext;
import net.sf.xenqtt.Log;
import net.sf.xenqtt.MqttException;
import net.sf.xenqtt.XenqttApplication;

/**
 * FIXME [jim] - needs javadoc
 */
public class ProxyServer implements XenqttApplication {

	private final CountDownLatch readyLatch = new CountDownLatch(1);
	private final Selector selector;
	private final ServerSocketChannel serverChannel;
	private final ServerThread serverThread = new ServerThread();

	private volatile Exception ioException;
	private volatile int port;

	/**
	 * FIXME [jim] - needs javadoc
	 */
	public ProxyServer() {

		try {
			selector = Selector.open();
			serverChannel = ServerSocketChannel.open();
		} catch (Exception e) {
			throw new MqttException("Failed to initialize the proxy server", e);
		}
	}

	/**
	 * @see net.sf.xenqtt.XenqttApplication#start(net.sf.xenqtt.AppContext)
	 */
	@Override
	public void start(AppContext arguments) {

		port = arguments.getArgAsInt("p", 1883);
		serverThread.start();

		try {
			readyLatch.await();
			if (ioException != null) {
				throw ioException;
			}
		} catch (Exception e) {
			throw new MqttException("Failed to start the proxy server", e);
		}
	}

	/**
	 * @see net.sf.xenqtt.XenqttApplication#stop()
	 */
	@Override
	public void stop() {

		try {
			selector.close();
			serverThread.join();
			if (ioException != null) {
				throw ioException;
			}
		} catch (Exception e) {
			throw new MqttException("Failed to shut down the proxy server cleanly", e);
		}
	}

	/**
	 * @see net.sf.xenqtt.XenqttApplication#getUsageText()
	 */
	@Override
	public String getUsageText() {
		// TODO Auto-generated method stub
		return null;
	}

	private final void doIo() throws Exception {

		serverChannel.configureBlocking(false);
		serverChannel.socket().bind(new InetSocketAddress(port));
		port = serverChannel.socket().getLocalPort();

		try {
			readyLatch.countDown();
			for (;;) {
				selector.select();
				serverChannel.register(selector, SelectionKey.OP_ACCEPT);
				Set<SelectionKey> keys = selector.selectedKeys();

				for (SelectionKey key : keys) {
					try {
						if (key.isAcceptable()) {
							SocketChannel newClient = serverChannel.accept();
							// FIXME [jim] - accept new client
						}
						if (key.isReadable()) {
							// FIXME [jim] - read more, looking for connect message
						}
					} catch (CancelledKeyException ignore) {
					}
				}

				keys.clear();
			}
		} finally {
			serverChannel.close();
		}
	}

	private final class ServerThread extends Thread {

		/**
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {

			try {
				doIo();
			} catch (ClosedSelectorException ignore) {
			} catch (Exception e) {
				Log.error(e, "Mock broker IO error");
				ioException = new RuntimeException("Proxy IO thread failed", e);
			}
		}
	}
}
