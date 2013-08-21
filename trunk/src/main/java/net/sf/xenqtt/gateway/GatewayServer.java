package net.sf.xenqtt.gateway;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import net.sf.xenqtt.message.ConnAckMessage;
import net.sf.xenqtt.message.ConnectMessage;
import net.sf.xenqtt.message.DisconnectMessage;
import net.sf.xenqtt.message.MessageHandler;
import net.sf.xenqtt.message.MqttChannel;
import net.sf.xenqtt.message.MqttChannelImpl;
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

public class GatewayServer {

	// FIXME [jim] - maybe keep track of connections that don't send a connect message within some time period and close them?

	// FIXME [jim] - how do sessions get deleted? going to need some kind of thread safe callback from the session I guess

	private final Map<String, GatewaySession> sessionsByClientId = new HashMap<String, GatewaySession>();

	private final String brokerUrl;

	private final Selector selector;
	private final CountDownLatch exitLatch = new CountDownLatch(1);
	private final ServerSocketChannel ssc;
	private final MessageHandler handler = new Handler();

	private volatile boolean running = true;

	public static void main(String[] args) throws IOException {

		if (args.length != 2) {
			usage();
		}

		String brokerUrl = args[0];
		int port = Integer.parseInt(args[1]);

		final GatewayServer server = new GatewayServer(brokerUrl);

		Thread hook = new Thread() {
			@Override
			public void run() {
				server.stop();
			};
		};

		Runtime.getRuntime().addShutdownHook(hook);

		server.run(port);
	}

	public GatewayServer(String brokerUrl) throws IOException {
		this.brokerUrl = brokerUrl;
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

		try {
			while (running) {
				selector.select();
				Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();
				while (keyIter.hasNext()) {
					process(keyIter.next());
					keyIter.remove();
				}
			}
		} finally {
			serverKey.cancel();
			ssc.close();
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
	GatewaySession createSession(MqttChannel channel, ConnectMessage message) throws IOException {

		return new GatewaySessionImpl(brokerUrl, channel, message);
	}

	private static void usage() {

		System.out.println("java -jar xenqtt.jar brokerUrl port");
		System.out.println("brokerUrl: URL to the broker");
		System.out.println("port: Port to listen for clustered clients on");
	}

	private void process(SelectionKey key) throws IOException {

		if (!key.isValid()) {
			return;
		}

		if (key.isAcceptable()) {
			SocketChannel socketChannel = ssc.accept();
			if (socketChannel != null) {
				new MqttChannelImpl(socketChannel, handler, selector);
			}

		} else if (key.isReadable()) {
			MqttChannel channel = (MqttChannel) key.attachment();
			if (!channel.read()) {
				channel.close();
			}
		}
	}

	private class Handler implements MessageHandler {

		@Override
		public void handle(MqttChannel channel, ConnectMessage message) throws Exception {

			try {
				channel.deregister();
				String clientId = message.getClientId();
				GatewaySession session = sessionsByClientId.get(clientId);
				if (session == null) {
					session = createSession(channel, message);
					sessionsByClientId.put(clientId, session);
				} else {
					session.addClient(channel, message);
				}
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
	}
}
