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
package net.xenqtt;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;

import net.xenqtt.message.ChannelManager;
import net.xenqtt.message.ChannelManagerImpl;
import net.xenqtt.message.MessageHandler;
import net.xenqtt.message.MqttMessage;

/**
 * Simple broker implementation. Can be used as the base class for more advanced broker implementations. Handles accepting new connections and adding them to a
 * {@link ChannelManager}.
 */
public class SimpleBroker {

	/**
	 * {@link ChannelManager} all new broker channels are added to.
	 */
	protected final ChannelManager manager;

	private final CountDownLatch readyLatch = new CountDownLatch(1);
	private final ServerSocketChannel server;
	private final Thread serverThread = new ServerThread();

	private MessageHandler messageHandler;

	private volatile Exception ioException;
	private volatile int port;

	/**
	 * @param messageResendIntervalSeconds
	 *            Seconds between attempts to resend a message that is {@link MqttMessage#isAckable()}. 0 to disable message resends.
	 * @param port
	 *            The port for the server to listen on. 0 will choose an arbitrary available port which you can get from {@link #getPort()} after calling
	 *            {@link #init()}.
	 */
	public SimpleBroker(long messageResendIntervalSeconds, int port) {

		XenqttUtil.validateGreaterThanOrEqualTo("messageResendIntervalSeconds", messageResendIntervalSeconds, 0);
		this.port = XenqttUtil.validateInRange("port", port, 0, 65535);

		try {
			this.manager = new ChannelManagerImpl(messageResendIntervalSeconds);
			server = ServerSocketChannel.open();
		} catch (IOException e) {
			throw new RuntimeException("Failed to create " + getClass().getSimpleName(), e);
		}
	}

	/**
	 * Starts the broker. Blocks until startup is complete.
	 * 
	 * @param messageHandler
	 *            Called when events happen
	 * @param serverThreadName
	 *            Name to give the server thread
	 */
	public final void init(MessageHandler messageHandler, String serverThreadName) {

		XenqttUtil.validateNotEmpty("serverThreadName", serverThreadName);
		XenqttUtil.validateNotNull("messageHandler", messageHandler);

		this.messageHandler = messageHandler;
		manager.init();

		serverThread.setName(serverThreadName);
		serverThread.start();

		try {
			readyLatch.await();
			if (ioException != null) {
				throw ioException;
			}
		} catch (Exception e) {
			throw new RuntimeException("Init failed", e);
		}
	}

	/**
	 * Shuts down the broker. Blocks until shutdown is complete.
	 * 
	 * @param millis
	 *            Milliseconds to wait for shutdown to complete. 0 means to wait forever.
	 * 
	 * @return true if shutdown is successful, otherwise false.
	 */
	public boolean shutdown(long millis) {

		XenqttUtil.validateGreaterThanOrEqualTo("millis", millis, 0);

		try {
			server.close();
			serverThread.join(millis);
			manager.shutdown();
			if (ioException != null) {
				throw ioException;
			}
			return !serverThread.isAlive();
		} catch (Exception e) {
			throw new RuntimeException("Shutdown failed", e);
		}
	}

	/**
	 * @return The URI to this broker
	 */
	public final String getURI() {

		try {
			String addr = InetAddress.getLocalHost().getHostAddress();
			int port = getPort();
			return String.format("tcp://%s:%d", addr, port);
		} catch (Exception e) {
			throw new MqttException("Unable to get the broker's URI", e);
		}
	}

	/**
	 * @return The port the broker is running on. Not valid until after {@link #init()} is called.
	 */
	public final int getPort() {
		return port;
	}

	private void doIo() {

		try {
			server.socket().bind(new InetSocketAddress(port));
			port = server.socket().getLocalPort();

			try {
				readyLatch.countDown();
				for (;;) {
					SocketChannel client = server.accept();
					manager.newBrokerChannel(client, messageHandler);
				}
			} finally {
				server.close();
			}
		} catch (ClosedChannelException ignore) {
		} catch (Exception e) {
			Log.error(e, getClass().getSimpleName() + " IO error");
			ioException = new RuntimeException(getClass().getSimpleName() + " IO thread failed", e);
		}
	}

	private final class ServerThread extends Thread {

		@Override
		public void run() {

			doIo();
		}
	}
}
