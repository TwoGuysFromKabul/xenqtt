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
package net.sf.xenqtt.mockbroker;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import net.sf.xenqtt.Log;
import net.sf.xenqtt.MqttException;
import net.sf.xenqtt.XenqttUtil;
import net.sf.xenqtt.message.ChannelManager;
import net.sf.xenqtt.message.ChannelManagerImpl;
import net.sf.xenqtt.message.ConnectMessage;
import net.sf.xenqtt.message.MessageHandler;
import net.sf.xenqtt.message.MqttMessage;

// TODO [jim] - update javadoc
/**
 * Mock MQTT broker used to test MQTT clients and applications. If debug level logging is enabled all broker events will be logged.
 */
public final class MockBroker {

	private final ConcurrentHashMap<String, String> credentials = new ConcurrentHashMap<String, String>();

	private final BrokerEvents events;

	private final CountDownLatch readyLatch = new CountDownLatch(1);
	private final ChannelManager manager;
	private final ServerSocketChannel server;
	private final Thread serverThread = new ServerThread();
	private final MessageHandler messageHandler;

	private volatile Exception ioException;
	private volatile int port;

	/**
	 * Creates a broker with the following config:
	 * <ul>
	 * <li>no {@link MockBrokerHandler}</li>
	 * <li>15 second message resend interval</li>
	 * <li>Any available port. Use {@link #getPort()} to get the port.</li>
	 * <li>allows anonymous access</li>
	 * <li>captures broker events</li>
	 * <li>allows 50 in-flight messages</li>
	 * </ul>
	 */
	public MockBroker() {
		this(null, 15, 0, true, true, 50);
	}

	/**
	 * Creates a broker with the specified {@link MockBrokerHandler handler} and the following config:
	 * <ul>
	 * <li>15 second message resend interval</li>
	 * <li>Any available port. Use {@link #getPort()} to get the port.</li>
	 * <li>allows anonymous access</li>
	 * <li>captures broker events</li>
	 * <li>allows 50 in-flight messages</li>
	 * </ul>
	 */
	public MockBroker(MockBrokerHandler brokerHandler) {
		this(brokerHandler, 15, 0, true, true, 50);
	}

	/**
	 * @param brokerHandler
	 *            Called when events happen. Can be {@code null} if you don't need to do any custom message handling.
	 * @param messageResendIntervalSeconds
	 *            Seconds between attempts to resend a message that is {@link MqttMessage#isAckable()}. 0 to disable message resends.
	 * @param port
	 *            The port for the server to listen on. 0 will choose an arbitrary available port which you can get from {@link MockBroker#getPort()} after
	 *            calling {@link #init()}.
	 * @param allowAnonymousAccess
	 *            If true then {@link ConnectMessage} with no username/password will be accepted. Otherwise only valid credentials will be accepted.
	 * @param captureBrokerEvents
	 *            If {@code true} then capture all events within the broker; otherwise, do not capture any events
	 * @param maxInFlightMessages
	 *            Maximum number of concurrent publish messages the broker will have in-flight to the client. This is an approximation. The actual maximum
	 *            number of in-flight messages may vary slightly.
	 */
	public MockBroker(MockBrokerHandler brokerHandler, long messageResendIntervalSeconds, int port, boolean allowAnonymousAccess, boolean captureBrokerEvents,
			int maxInFlightMessages) {
		XenqttUtil.validateGreaterThanOrEqualTo("messageResendIntervalSeconds", messageResendIntervalSeconds, 0);
		XenqttUtil.validateGreaterThan("maxInFlightMessages", maxInFlightMessages, 0);

		this.events = captureBrokerEvents ? new BrokerEventsImpl() : new NullBrokerEvents();
		this.messageHandler = new BrokerMessageHandler(brokerHandler, events, credentials, allowAnonymousAccess, maxInFlightMessages);
		this.manager = new ChannelManagerImpl(messageResendIntervalSeconds);
		this.port = XenqttUtil.validateInRange("port", port, 0, 65535);

		try {
			server = ServerSocketChannel.open();
		} catch (IOException e) {
			throw new RuntimeException("Failed to create mock broker", e);
		}
	}

	/**
	 * Starts the mock broker
	 */
	public void init() {
		manager.init();
		serverThread.setName("MockBrokerServer");
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
	 * Shuts down the mock broker
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
	 * Adds the specified credentials for authentication by {@link ConnectMessage}s. If password is {@code null} any existing credentials for the user are
	 * removed.
	 */
	public void addCredentials(String userName, String password) {
		if (password == null) {
			credentials.remove(userName);
		} else {
			XenqttUtil.validateNotNull("userName", userName);

			credentials.put(userName, password);
		}
	}

	/**
	 * @return The URI to this broker
	 */
	public String getURI() {

		try {
			String addr = InetAddress.getLocalHost().getHostAddress();
			int port = getPort();
			return String.format("tcp://%s:%d", addr, port);
		} catch (Exception e) {
			throw new MqttException("Unable to get mock broker URI", e);
		}
	}

	/**
	 * @return The port the mock broker is running on. Not valid until after {@link #init()} is called.
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @return All broker events. This list is a copy.
	 */
	public List<BrokerEvent> getEvents() {
		return events.getEvents();
	}

	/**
	 * @return All broker events for the specified client ID. This list is a copy.
	 */
	public List<BrokerEvent> getEvents(String clientId) {
		return events.getEvents(clientId);
	}

	/**
	 * Removes all broker events
	 */
	public void clearEvents() {
		events.clearEvents();
	}

	/**
	 * Removes the specified broker events.
	 */
	public void removeEvents(Collection<BrokerEvent> eventsToRemove) {
		XenqttUtil.validateNotNull("eventsToRemove", eventsToRemove);

		events.removeEvents(eventsToRemove);
	}

	private void doIo() throws Exception {

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
	}

	private final class ServerThread extends Thread {

		@Override
		public void run() {

			try {
				doIo();
			} catch (ClosedChannelException ignore) {
			} catch (Exception e) {
				Log.error(e, "Mock broker IO error");
				ioException = new RuntimeException("Mock broker IO thread failed", e);
			}
		}
	}
}
