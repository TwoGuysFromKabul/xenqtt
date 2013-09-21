package net.sf.xenqtt.mockbroker;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import net.sf.xenqtt.Log;
import net.sf.xenqtt.message.ChannelManager;
import net.sf.xenqtt.message.ChannelManagerImpl;
import net.sf.xenqtt.message.ConnectMessage;
import net.sf.xenqtt.message.MessageHandler;
import net.sf.xenqtt.message.MqttMessage;

// TODO [jim] - update javadoc
/**
 * Mock MQTT broker used to test MQTT clients and applications
 */
public final class MockBroker {

	private final ConcurrentHashMap<String, String> credentials = new ConcurrentHashMap<String, String>();

	private final BrokerEvents events;

	private final CountDownLatch readyLatch = new CountDownLatch(1);
	private final ChannelManager manager;
	private final ServerSocketChannel server;
	private final Thread serverThread = new ServerThread();;
	private final MessageHandler messageHandler;

	private volatile Exception ioException;
	private volatile int port;

	/**
	 * Creates a broker with no {@link MockBrokerHandler}, 15 second message resend interval, port 1883, allows anonymous access, and does not print events
	 */
	public MockBroker() {
		this(null, 15, 1883, true, false);
	}

	/**
	 * @param brokerHandler
	 *            Called when events happen. Can be null if you don't need to do any custom message handling.
	 * @param messageResendIntervalSeconds
	 *            Seconds between attempts to resend a message that is {@link MqttMessage#isAckable()}. 0 to disable message resends.
	 * @param port
	 *            The port for the server to listen on. 0 will choose an arbitrary available port which you can get from {@link MockBroker#getPort()} after
	 *            calling {@link #init()}.
	 * @param allowAnonymousAccess
	 *            If true then {@link ConnectMessage} with no username/password will be accepted. Otherwise only valid credentials will be accepted.
	 * @param logEvents
	 *            If true events will logged at info level as they occur
	 */
	public MockBroker(MockBrokerHandler brokerHandler, long messageResendIntervalSeconds, int port, boolean allowAnonymousAccess, boolean logEvents) {
		this.events = new BrokerEvents(logEvents);
		this.messageHandler = new BrokerMessageHandler(brokerHandler, events, credentials, allowAnonymousAccess);
		this.manager = new ChannelManagerImpl(messageResendIntervalSeconds);
		this.port = port;

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
	 * Adds the specified credentials for authentication by {@link ConnectMessage}s. If userName is null any existing credentials for that user are removed. If
	 * userName is not null the password may not be null.
	 */
	public void addCredentials(String userName, String password) {
		if (userName == null) {
			credentials.remove(userName);
		} else {
			credentials.put(userName, password);
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
