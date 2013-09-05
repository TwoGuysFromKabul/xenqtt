package net.sf.xenqtt.message;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.mockito.MockitoAnnotations;

public abstract class MqttChannelTestBase<C extends AbstractMqttChannel, B extends AbstractMqttChannel> {

	ServerSocketChannel ssc;
	Selector selector;
	int port;

	C clientChannel;
	B brokerChannel;

	MockMessageHandler clientHandler = new MockMessageHandler(false);
	MockMessageHandler brokerHandler = new MockMessageHandler(true);

	long now = System.currentTimeMillis();

	@Before
	public void setup() throws Exception {

		MockitoAnnotations.initMocks(this);

		ssc = ServerSocketChannel.open();
		ssc.configureBlocking(false);

		ServerSocket serverSocket = ssc.socket();
		serverSocket.bind(new InetSocketAddress(0));
		port = serverSocket.getLocalPort();

		selector = Selector.open();

		ssc.register(selector, SelectionKey.OP_ACCEPT);
	}

	/**
	 * reads and writes until the specified number of client and broker messages are received or until a channel is closed
	 * 
	 * @return null if the requested message counts were received. Otherwise, the connection that was closed (read op hit end of stream)
	 */
	MqttChannel readWrite(int clientMessageCount, int brokerMessageCount) throws Exception {

		clientHandler.messagesReceived.clear();
		brokerHandler.messagesReceived.clear();

		while (brokerHandler.messagesReceived.size() < brokerMessageCount || clientHandler.messagesReceived.size() < clientMessageCount) {

			selector.select();
			Iterator<SelectionKey> iter = selector.selectedKeys().iterator();

			while (iter.hasNext()) {
				SelectionKey key = iter.next();
				MqttChannel channel = (MqttChannel) key.attachment();
				if (key.isReadable()) {
					if (!channel.read(now)) {
						return channel;
					}
				}
				if (key.isWritable()) {
					if (!channel.write(now)) {
						return channel;
					}
				}
				iter.remove();
			}
		}

		return null;
	}

	/**
	 * Closes both client and broker connections
	 */
	void closeConnection() {

		clientChannel.close();
		assertTrue(clientHandler.closed);
		assertFalse(clientChannel.isConnected());
		assertFalse(clientChannel.isOpen());
		brokerChannel.close();
		assertTrue(brokerHandler.closed);
		assertFalse(brokerChannel.isConnected());
		assertFalse(brokerChannel.isOpen());
	}

	/**
	 * Establishes a socket connection. Creates the client and/or broker channels if they do not exist
	 */
	void establishConnection() throws Exception {

		if (clientChannel == null) {
			clientChannel = newClientChannel();
		}

		assertTrue(clientChannel.isOpen());
		assertTrue(clientChannel.isConnectionPending());

		assertFalse(clientHandler.opened);
		assertFalse(brokerHandler.opened);

		assertEquals(2, selector.keys().size());

		boolean clientConnected = false;
		while (brokerChannel == null || !clientConnected) {
			selector.select();

			Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
			while (iter.hasNext()) {
				SelectionKey key = iter.next();
				if (key.isAcceptable()) {
					assertSame(ssc, key.channel());
					assertTrue(key.isAcceptable());
					SocketChannel brokerSocketChannel = ssc.accept();
					brokerChannel = newBrokerChannel(brokerSocketChannel);
					assertFalse(brokerChannel.isConnectionPending());
					assertTrue(brokerChannel.isOpen());
					assertTrue(brokerHandler.opened);
					key.cancel();
					ssc.close();
				} else if (key.isConnectable()) {
					assertSame(clientChannel, key.attachment());
					assertFalse(key.channel().isBlocking());
					assertTrue(key.channel().isRegistered());
					assertFalse(clientHandler.opened);
					assertTrue(clientChannel.isConnectionPending());
					clientChannel.finishConnect();
					assertFalse(clientChannel.isConnectionPending());
					assertTrue(clientChannel.isOpen());
					assertTrue(clientHandler.opened);
					clientConnected = true;
				}
				iter.remove();
			}
		}

		assertFalse(clientChannel.isConnected());
		assertFalse(brokerChannel.isConnected());

		assertTrue(clientHandler.opened);
		assertTrue(brokerHandler.opened);

		assertFalse(brokerHandler.closed);
		assertFalse(clientHandler.closed);
	}

	/**
	 * Sends a {@link ConnectMessage} from the client and a {@link ConnAckMessage} reply and waits for both to complete
	 */
	void connect() throws Exception {

		ConnectMessage connMsg = new ConnectMessage("abc", false, 10000);
		ConnAckMessage ackMsg = new ConnAckMessage(ConnectReturnCode.ACCEPTED);

		clientChannel.send(now, connMsg);
		assertNull(readWrite(0, 1));
		assertEquals(1, brokerHandler.messagesReceived.size());
		assertNotSame(connMsg, brokerHandler.messagesReceived.get(0));
		assertEquals(connMsg, brokerHandler.messagesReceived.get(0));

		brokerChannel.send(now, ackMsg);
		assertNull(readWrite(1, 0));
		assertEquals(1, clientHandler.messagesReceived.size());
		assertNotSame(ackMsg, clientHandler.messagesReceived.get(0));
		assertEquals(ackMsg, clientHandler.messagesReceived.get(0));

		assertTrue(clientChannel.isConnected());
		assertTrue(brokerChannel.isConnected());
	}

	/**
	 * Sends a {@link DisconnectMessage} from the client to the broker
	 */
	void disconnect() throws Exception {

		clientChannel.send(now, new DisconnectMessage());
		readWrite(0, 1);

		assertFalse(clientChannel.isConnected());
		assertFalse(brokerChannel.isConnected());
		assertFalse(clientChannel.isOpen());
		assertFalse(brokerChannel.isOpen());
	}

	/**
	 * @return a new broker channel
	 */
	abstract B newBrokerChannel(SocketChannel brokerSocketChannel) throws Exception;

	/**
	 * @return A new client channel
	 */
	abstract C newClientChannel() throws Exception;

	class MockMessageHandler implements MessageHandler {

		private final boolean isBrokerChannel;
		List<MqttMessage> messagesReceived = new ArrayList<MqttMessage>();
		RuntimeException exceptionToThrow;
		boolean opened;
		boolean closed;
		Throwable closeCause;

		public MockMessageHandler(boolean isBrokerChannel) {
			this.isBrokerChannel = isBrokerChannel;
		}

		@Override
		public void connect(MqttChannel channel, ConnectMessage message) throws Exception {
			doHandleInvocation(channel, message);
		}

		@Override
		public void connAck(MqttChannel channel, ConnAckMessage message) throws Exception {
			doHandleInvocation(channel, message);
		}

		@Override
		public void publish(MqttChannel channel, PublishMessage message) throws Exception {
			doHandleInvocation(channel, message);
		}

		@Override
		public void pubAck(MqttChannel channel, PubAckMessage message) throws Exception {
			doHandleInvocation(channel, message);
		}

		@Override
		public void pubRec(MqttChannel channel, PubRecMessage message) throws Exception {
			doHandleInvocation(channel, message);
		}

		@Override
		public void pubRel(MqttChannel channel, PubRelMessage message) throws Exception {
			doHandleInvocation(channel, message);
		}

		@Override
		public void pubComp(MqttChannel channel, PubCompMessage message) throws Exception {
			doHandleInvocation(channel, message);
		}

		@Override
		public void subscribe(MqttChannel channel, SubscribeMessage message) throws Exception {
			doHandleInvocation(channel, message);
		}

		@Override
		public void subAck(MqttChannel channel, SubAckMessage message) throws Exception {
			doHandleInvocation(channel, message);
		}

		@Override
		public void unsubscribe(MqttChannel channel, UnsubscribeMessage message) throws Exception {
			doHandleInvocation(channel, message);
		}

		@Override
		public void unsubAck(MqttChannel channel, UnsubAckMessage message) throws Exception {
			doHandleInvocation(channel, message);
		}

		@Override
		public void disconnect(MqttChannel channel, DisconnectMessage message) throws Exception {
			doHandleInvocation(channel, message);
		}

		@Override
		public void channelOpened(MqttChannel channel) {
			opened = true;
			doHandleInvocation(channel, null);
		}

		@Override
		public void channelClosed(MqttChannel channel, Throwable cause) {
			closed = true;
			closeCause = cause;
			doHandleInvocation(channel, null);
		}

		private void doHandleInvocation(MqttChannel channel, MqttMessage message) {

			MqttChannel expectedChannel = isBrokerChannel ? brokerChannel : clientChannel;
			// need to check for null because this can be invoked during channel construction which is before the expected channel reference is assigned
			if (expectedChannel != null) {
				assertSame(expectedChannel, channel);
			}
			if (exceptionToThrow != null) {
				RuntimeException e = exceptionToThrow;
				exceptionToThrow = null;
				throw e;
			}

			if (message != null) {
				messagesReceived.add(message);
			}
		}
	}

	final class TestChannel extends AbstractMqttChannel {

		private final MockMessageHandler messageHandler;
		Exception exceptionToThrow;
		long lastReceived;
		boolean connectedCalled;
		boolean disconnectedCalled;
		long pingIntervalMillis;

		public TestChannel(SocketChannel channel, MockMessageHandler handler, Selector selector, long messageResendIntervalMillis) throws IOException {
			super(channel, handler, selector, messageResendIntervalMillis);
			this.messageHandler = handler;
		}

		public TestChannel(String host, int port, MockMessageHandler handler, Selector selector, long messageResendIntervalMillis) throws IOException {
			super(host, port, handler, selector, messageResendIntervalMillis);
			this.messageHandler = handler;
		}

		@Override
		void connected(long pingIntervalMillis) {
			connectedCalled = true;
			this.pingIntervalMillis = pingIntervalMillis;
		}

		@Override
		void disconnected() {
			disconnectedCalled = true;
		}

		@Override
		void pingReq(long now, PingReqMessage message) throws Exception {
			if (exceptionToThrow != null) {
				throw exceptionToThrow;
			}
			messageHandler.messagesReceived.add(message);
		}

		@Override
		void pingResp(long now, PingRespMessage message) throws Exception {
			if (exceptionToThrow != null) {
				throw exceptionToThrow;
			}
			messageHandler.messagesReceived.add(message);
		}

		@Override
		long keepAlive(long now, long lastMessageReceived) throws Exception {

			if (exceptionToThrow != null) {
				throw exceptionToThrow;
			}
			lastReceived = lastMessageReceived;

			return 25000;
		}
	}
}
