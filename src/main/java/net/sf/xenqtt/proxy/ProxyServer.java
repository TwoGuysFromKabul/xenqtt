package net.sf.xenqtt.proxy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import net.sf.xenqtt.message.ConnAckMessage;
import net.sf.xenqtt.message.ConnectMessage;
import net.sf.xenqtt.message.DisconnectMessage;
import net.sf.xenqtt.message.MessageHandler;
import net.sf.xenqtt.message.MqttChannel;
import net.sf.xenqtt.message.PingReqMessage;
import net.sf.xenqtt.message.PingRespMessage;
import net.sf.xenqtt.message.PubAckMessage;
import net.sf.xenqtt.message.PubCompMessage;
import net.sf.xenqtt.message.PubRecMessage;
import net.sf.xenqtt.message.PubRelMessage;
import net.sf.xenqtt.message.PublishMessage;
import net.sf.xenqtt.message.SubAckMessage;
import net.sf.xenqtt.message.SubscribeMessage;
import net.sf.xenqtt.message.UnsubAckMessage;
import net.sf.xenqtt.message.UnsubscribeMessage;

public class ProxyServer {

	// FIXME [jim] - maybe keep track of connections that don't send a connect message within some time period and close them?

	private final Map<String, ProxySession> sessionsByClientId = new HashMap<String, ProxySession>();

	private final String brokerHost;
	private final int brokerPort;
	private final int sessionCleanupIntervalMillis;

	private final Selector selector;
	private final CountDownLatch exitLatch = new CountDownLatch(1);
	private final ServerSocketChannel ssc;
	private final MessageHandler handler = new Handler();

	private volatile boolean running = true;

	public static void main(String[] args) throws IOException {

		if (args.length != 2) {
			usage(null);
		}

		String brokerHost = null;
		int brokerPort = 0;
		String brokerUri = args[0];
		try {
			if (brokerUri.isEmpty()) {
				usage("Broker URI is required");
			}

			URI uri = new URI(brokerUri);
			brokerHost = uri.getHost();
			brokerPort = uri.getPort();
			if (brokerHost == null || brokerPort <= 0) {
				usage("The broker URI requires both a valid host and a port > 0");
			}
		} catch (URISyntaxException e) {
			usage("Invalid broker URI: " + brokerUri);
		}

		int clientPort = 0;
		try {
			clientPort = Integer.parseInt(args[1]);
		} catch (NumberFormatException e) {
			usage("Invalid port");
		}

		final ProxyServer server = new ProxyServer(brokerHost, brokerPort, 60000);

		Thread hook = new Thread() {
			@Override
			public void run() {
				server.stop();
			};
		};

		Runtime.getRuntime().addShutdownHook(hook);

		server.run(clientPort);
	}

	public ProxyServer(String brokerHost, int brokerPort, int sessionCleanupIntervalMillis) throws IOException {
		this.brokerHost = brokerHost;
		this.brokerPort = brokerPort;
		this.sessionCleanupIntervalMillis = sessionCleanupIntervalMillis;
		this.selector = Selector.open();
		this.ssc = ServerSocketChannel.open();
	}

	/**
	 * Runs the server on the specified port. The calling thread will not return until {@link #stop()} is called.
	 */
	public final void run(int port) throws IOException {

		ssc.configureBlocking(false);
		ssc.socket().bind(new InetSocketAddress(port));
		SelectionKey serverKey = ssc.register(selector, SelectionKey.OP_ACCEPT);

		long now = System.currentTimeMillis();
		long nextSessionCleanupTime = now + sessionCleanupIntervalMillis;

		try {
			while (running) {

				long sleepMillis = nextSessionCleanupTime - now;
				if (sleepMillis > 0) {
					selector.select(sleepMillis);
				}

				Set<SelectionKey> keys = selector.selectedKeys();
				for (SelectionKey key : keys) {
					processKey(key);
				}
				keys.clear();

				now = System.currentTimeMillis();
				if (now >= nextSessionCleanupTime) {
					cleanupSessions();
					nextSessionCleanupTime = now + sessionCleanupIntervalMillis;
				}
			}
		} finally {
			serverKey.cancel();
			ssc.close();
			abortSessions();
			exitLatch.countDown();
		}
	}

	/**
	 * Stops the server
	 */
	public final void stop() {

		running = false;
		selector.wakeup();
		try {
			exitLatch.await();
		} catch (InterruptedException ignore) {
		}
	}

	/**
	 * Intended to be overridden to inject a mock sessions during unit testing
	 */
	ProxySession createSession(MqttChannel channel, ConnectMessage message) throws IOException {

		// FIXME [jim] - implement proxy
		// return new ProxySessionImpl(brokerHost, brokerPort, channel, message);
		return null;
	}

	private static void usage(String errorMessage) {

		if (errorMessage != null) {
			System.out.println();
			System.out.println(errorMessage);
		}

		System.out.println("\njava -jar xenqtt.jar brokerUri port");
		System.out.println("\tbrokerUri: URI to the broker. i.e. tcp://my.host.com:1883");
		System.out.println("\tport: Port to listen for clustered clients on");
		System.out.println();
	}

	private void abortSessions() {

		for (ProxySession session : sessionsByClientId.values()) {
			try {
				session.close();
			} catch (Exception ignore) {
			}
		}
	}

	/**
	 * We clean up session in this garbage collection manner to reduce the complexity that would be involved with having the session do some kind of callback to
	 * this server.
	 */
	private void cleanupSessions() {
		Iterator<ProxySession> sessionIter = sessionsByClientId.values().iterator();
		while (sessionIter.hasNext()) {
			ProxySession session = sessionIter.next();
			if (!session.isOpen()) {
				sessionIter.remove();
			}
		}
	}

	private void processKey(SelectionKey key) throws IOException {

		if (!key.isValid()) {
			return;
		}

		if (key.isAcceptable()) {
			SocketChannel socketChannel = ssc.accept();
			if (socketChannel != null) {
				// FIXME [jim] - implement proxy
				// new MqttChannelImpl(socketChannel, handler, selector);
			}

		} else if (key.isReadable()) {
			MqttChannel channel = (MqttChannel) key.attachment();
			// FIXME [jim] - implement proxy
			// if (!channel.read()) {
			// channel.close();
			// }
		}
	}

	private class Handler implements MessageHandler {

		@Override
		public void handle(MqttChannel channel, ConnectMessage message) throws Exception {

			try {
				channel.deregister();

				String clientId = message.getClientId();

				ProxySession session = sessionsByClientId.get(clientId);
				if (session != null && session.addClient(channel, message)) {
					return;
				}

				session = createSession(channel, message);
				sessionsByClientId.put(clientId, session);

			} catch (Exception e) {
				// FIXME [jim] - log or something??
				e.printStackTrace();
				channel.close();
			}
		}

		@Override
		public void handle(MqttChannel channel, ConnAckMessage message) {
		}

		@Override
		public void handle(MqttChannel channel, PublishMessage message) {
		}

		@Override
		public void handle(MqttChannel channel, PubAckMessage message) {
		}

		@Override
		public void handle(MqttChannel channel, PubRecMessage message) {
		}

		@Override
		public void handle(MqttChannel channel, PubRelMessage message) {
		}

		@Override
		public void handle(MqttChannel channel, PubCompMessage message) {
		}

		@Override
		public void handle(MqttChannel channel, SubscribeMessage message) {
		}

		@Override
		public void handle(MqttChannel channel, SubAckMessage message) {
		}

		@Override
		public void handle(MqttChannel channel, UnsubscribeMessage message) {
		}

		@Override
		public void handle(MqttChannel channel, UnsubAckMessage message) {
		}

		@Override
		public void handle(MqttChannel channel, PingReqMessage message) {
		}

		@Override
		public void handle(MqttChannel channel, PingRespMessage message) {
		}

		@Override
		public void handle(MqttChannel channel, DisconnectMessage message) {
		}

		@Override
		public void channelClosed(MqttChannel channel) {
		}
	}
}
