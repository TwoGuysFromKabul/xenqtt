package net.sf.xenqtt.message;

import static org.junit.Assert.*;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

public class MqttChannelImplTest {

	ServerSocketChannel ssc;
	Selector selector;
	int port;

	MqttChannel clientChannel;
	MqttChannel brokerChannel;

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

	@Test
	public void testCtors() throws Exception {

		establishConnection();

		assertFalse(clientChannel.isConnectionPending());
		assertTrue(clientChannel.isOpen());

		assertFalse(brokerChannel.isConnectionPending());
		assertTrue(brokerChannel.isOpen());

		closeConnection();
	}

	@Test
	public void testRegister() throws Exception {

		establishConnection();

		Selector newSelector = Selector.open();
		assertEquals(0, newSelector.keys().size());

		clientChannel.register(newSelector, new MockMessageHandler(false));
		assertEquals(1, newSelector.keys().size());

		closeConnection();
	}

	@Test
	public void testDeregister() throws Exception {

		establishConnection();

		int originalCancelledKeyCount = 0;
		for (SelectionKey key : selector.keys()) {
			if (!key.isValid()) {
				originalCancelledKeyCount++;
			}
		}

		clientChannel.deregister();

		int cancelledKeyCount = 0;
		for (SelectionKey key : selector.keys()) {
			if (!key.isValid()) {
				cancelledKeyCount++;
			}
		}

		assertEquals(originalCancelledKeyCount + 1, cancelledKeyCount);
	}

	@Test
	public void testHouseKeeping_ResendMessage_qos0() throws Exception {

		clientChannel = new MqttChannelImpl("localhost", port, clientHandler, selector, 10);

		establishConnection();

		PublishMessage msg = new PublishMessage(false, QoS.AT_MOST_ONCE, false, "foo", 12, new byte[] { 1, 2, 3 });

		clientChannel.send(now, msg);
		assertNull(readWrite(0, 1));
		assertEquals(1, brokerHandler.messagesReceived.size());
		assertNotSame(msg, brokerHandler.messagesReceived.get(0));
		assertEquals(msg, brokerHandler.messagesReceived.get(0));
		assertFalse(brokerHandler.messagesReceived.get(0).isDuplicate());
		assertEquals(0, clientChannel.inFlightMessageCount());
	}

	@Test
	public void testHouseKeeping_ResendMessage_qos1() throws Exception {

		clientChannel = new MqttChannelImpl("localhost", port, clientHandler, selector, 10);

		establishConnection();

		PublishMessage msg = new PublishMessage(false, QoS.AT_LEAST_ONCE, false, "foo", 12, new byte[] { 1, 2, 3 });

		clientChannel.send(now, msg);
		assertNull(readWrite(0, 1));
		assertEquals(1, brokerHandler.messagesReceived.size());
		assertNotSame(msg, brokerHandler.messagesReceived.get(0));
		assertEquals(msg, brokerHandler.messagesReceived.get(0));
		assertFalse(brokerHandler.messagesReceived.get(0).isDuplicate());
		assertEquals(1, clientChannel.inFlightMessageCount());

		clientChannel.houseKeeping(now + 100);
		assertNull(readWrite(0, 1));
		assertEquals(1, brokerHandler.messagesReceived.size());
		assertEquals(msg.getMessageId(), ((IdentifiableMqttMessage) brokerHandler.messagesReceived.get(0)).getMessageId());
		assertTrue(brokerHandler.messagesReceived.get(0).isDuplicate());
		assertEquals(1, clientChannel.inFlightMessageCount());

		brokerChannel.send(now, new PubAckMessage(12));
		assertNull(readWrite(1, 0));
		assertEquals(0, clientChannel.inFlightMessageCount());
		assertEquals(0, brokerChannel.inFlightMessageCount());
	}

	@Test
	public void testHouseKeeping_ResendMessage_qos2() throws Exception {

		clientChannel = new MqttChannelImpl("localhost", port, clientHandler, selector, 10);

		establishConnection();

		PublishMessage msg = new PublishMessage(false, QoS.EXACTLY_ONCE, false, "foo", 12, new byte[] { 1, 2, 3 });

		clientChannel.send(now, msg);
		assertNull(readWrite(0, 1));
		assertEquals(1, brokerHandler.messagesReceived.size());
		assertNotSame(msg, brokerHandler.messagesReceived.get(0));
		assertEquals(msg, brokerHandler.messagesReceived.get(0));
		assertFalse(brokerHandler.messagesReceived.get(0).isDuplicate());
		assertEquals(1, clientChannel.inFlightMessageCount());

		clientChannel.houseKeeping(now + 100);
		assertNull(readWrite(0, 1));
		assertEquals(1, brokerHandler.messagesReceived.size());
		assertEquals(msg.getMessageId(), ((IdentifiableMqttMessage) brokerHandler.messagesReceived.get(0)).getMessageId());
		assertTrue(brokerHandler.messagesReceived.get(0).isDuplicate());
		assertEquals(1, clientChannel.inFlightMessageCount());

		brokerChannel.send(now, new PubRecMessage(12));
		assertNull(readWrite(1, 0));
		assertEquals(0, clientChannel.inFlightMessageCount());
		assertEquals(0, brokerChannel.inFlightMessageCount());
	}

	@Test
	public void testReadWriteSend_ClientClosesConnection() throws Exception {

		establishConnection();

		clientChannel.close();

		assertEquals(brokerChannel, readWrite(1, 1));

		brokerChannel.close();
	}

	@Test
	public void testReadWriteSend_BrokerClosesConnection() throws Exception {

		establishConnection();

		brokerChannel.close();

		assertEquals(clientChannel, readWrite(1, 1));

		clientChannel.close();
	}

	@Test
	public void testReadWriteSend_DisconnectClosesConnection() throws Exception {

		establishConnection();

		DisconnectMessage msg = new DisconnectMessage();

		clientChannel.send(now, msg);
		assertNull(readWrite(0, 1));
		assertEquals(1, brokerHandler.messagesReceived.size());
		assertNotSame(msg, brokerHandler.messagesReceived.get(0));
		assertEquals(msg, brokerHandler.messagesReceived.get(0));

		assertFalse(clientChannel.isOpen());
		assertEquals(brokerChannel, readWrite(1, 1));
		brokerChannel.close();
	}

	@Test
	public void testReadWriteSend_ConnAckWithoutAcceptClosesConnection() throws Exception {

		establishConnection();

		ConnAckMessage msg = new ConnAckMessage(ConnectReturnCode.BAD_CREDENTIALS);

		clientChannel.send(now, msg);
		assertNull(readWrite(0, 1));
		assertEquals(1, brokerHandler.messagesReceived.size());
		assertNotSame(msg, brokerHandler.messagesReceived.get(0));
		assertEquals(msg, brokerHandler.messagesReceived.get(0));

		assertFalse(clientChannel.isOpen());
		assertEquals(brokerChannel, readWrite(1, 1));
		brokerChannel.close();
	}

	@Test
	public void testReadWriteSend_ConnAckWithAccept() throws Exception {

		establishConnection();

		ConnAckMessage msg = new ConnAckMessage(ConnectReturnCode.ACCEPTED);

		clientChannel.send(now, msg);
		assertNull(readWrite(0, 1));
		assertEquals(1, brokerHandler.messagesReceived.size());
		assertNotSame(msg, brokerHandler.messagesReceived.get(0));
		assertEquals(msg, brokerHandler.messagesReceived.get(0));

		assertTrue(clientChannel.isOpen());

		closeConnection();
	}

	@Test
	public void testReadWriteSend_PingReqResp() throws Exception {

		establishConnection();

		PingReqMessage pingReqMsg = new PingReqMessage();
		PingRespMessage pingRespMsg = new PingRespMessage();

		clientChannel.send(now, pingReqMsg);
		assertNull(readWrite(0, 1));
		assertEquals(1, brokerHandler.messagesReceived.size());
		assertNotSame(pingReqMsg, brokerHandler.messagesReceived.get(0));
		assertEquals(pingReqMsg, brokerHandler.messagesReceived.get(0));

		brokerChannel.send(now, pingRespMsg);
		assertNull(readWrite(1, 0));
		assertEquals(1, clientHandler.messagesReceived.size());
		assertNotSame(pingRespMsg, clientHandler.messagesReceived.get(0));
		assertEquals(pingRespMsg, clientHandler.messagesReceived.get(0));

		clientChannel.close();

		assertEquals(brokerChannel, readWrite(1, 1));

		brokerChannel.close();
	}

	@Test
	public void testSend_NotConnectedYet() throws Exception {

		PingReqMessage msg = new PingReqMessage();

		clientChannel = new MqttChannelImpl("localhost", port, clientHandler, selector, 10000);
		clientChannel.send(now, msg);

		establishConnection();

		assertNull(readWrite(0, 1));

		closeConnection();
	}

	@Test
	public void testReadWriteSend_HanlderThrowsException() throws Exception {

		establishConnection();

		UnsubAckMessage msg1 = new UnsubAckMessage(1);
		PingReqMessage msg2 = new PingReqMessage();

		clientChannel.send(now, msg1);
		clientChannel.send(now, msg2);

		assertNull(readWrite(0, 1));

		assertEquals(1, brokerHandler.messagesReceived.size());
		assertEquals(msg2, brokerHandler.messagesReceived.get(0));

		closeConnection();
	}

	@Test
	public void testReadWriteSend_RemainingLengthZero() throws Exception {

		establishConnection();

		PingReqMessage msg = new PingReqMessage();

		clientChannel.send(now, msg);
		assertNull(readWrite(0, 1));

		closeConnection();
	}

	@Test
	public void testReadWriteSend_RemainingLength2() throws Exception {

		establishConnection();

		PubAckMessage msg = new PubAckMessage(123);

		clientChannel.send(now, msg);
		assertNull(readWrite(0, 1));

		closeConnection();
	}

	@Test
	public void testReadWriteSend_RemainingLength126to129() throws Exception {

		doTestReadWriteSend(126, 4);
	}

	@Test
	public void testReadWriteSend_RemainingLength16382to16385() throws Exception {
		doTestReadWriteSend(16382, 4);
	}

	@Test
	public void testReadWriteSend_RemainingLength2097150to2097155() throws Exception {
		doTestReadWriteSend(2097150, 6);
	}

	@Test
	public void testReadWriteSend_LotsOfMessages() throws Exception {
		doTestReadWriteSend(1000, 5000);
	}

	private void doTestReadWriteSend(int firstRemainingLength, int messageCount) throws Exception {

		List<PublishMessage> messagesSent = new ArrayList<PublishMessage>();

		establishConnection();

		for (int remainingLength = firstRemainingLength; remainingLength < firstRemainingLength + messageCount; remainingLength++) {
			int payloadLength = remainingLength - 7;
			byte[] payload = new byte[payloadLength];
			Arrays.fill(payload, (byte) messageCount);

			PublishMessage msg = new PublishMessage(false, QoS.AT_LEAST_ONCE, false, "abc", 123, payload);

			clientChannel.send(now, msg);
			messagesSent.add(msg);
		}

		assertNull(readWrite(0, messageCount));

		assertEquals(brokerHandler.messagesReceived, messagesSent);

		closeConnection();
	}

	/**
	 * reads and writes until the specified number of client and broker messages are received or until a channel is closed
	 * 
	 * @return null if the requested message counts were received. Otherwise, the connection that was closed (read op hit end of stream)
	 */
	private MqttChannel readWrite(int clientMessageCount, int brokerMessageCount) throws Exception {

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
					channel.write(now);
				}
				iter.remove();
			}
		}

		return null;
	}

	private void closeConnection() {

		clientChannel.close();
		assertFalse(clientChannel.isOpen());
		brokerChannel.close();
		assertFalse(brokerChannel.isOpen());
	}

	private void establishConnection() throws Exception {

		if (clientChannel == null) {
			clientChannel = new MqttChannelImpl("localhost", port, clientHandler, selector, 10000);
		}

		assertTrue(clientChannel.isOpen());
		assertTrue(clientChannel.isConnectionPending());

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
					brokerChannel = new MqttChannelImpl(brokerSocketChannel, brokerHandler, selector, 10000);
					assertFalse(brokerChannel.isConnectionPending());
					assertTrue(brokerChannel.isOpen());
					key.cancel();
					ssc.close();
				} else if (key.isConnectable()) {
					assertSame(clientChannel, key.attachment());
					assertFalse(key.channel().isBlocking());
					assertTrue(key.channel().isRegistered());
					assertTrue(clientChannel.isConnectionPending());
					clientChannel.finishConnect();
					assertFalse(clientChannel.isConnectionPending());
					assertTrue(clientChannel.isOpen());
					clientConnected = true;
				}
				iter.remove();
			}
		}
	}

	private class MockMessageHandler implements MessageHandler {

		private final boolean isBrokerChannel;
		List<MqttMessage> messagesReceived = new ArrayList<MqttMessage>();

		public MockMessageHandler(boolean isBrokerChannel) {
			this.isBrokerChannel = isBrokerChannel;
		}

		@Override
		public void handle(MqttChannel channel, ConnectMessage message) {
			messagesReceived.add(message);
			assertSame(isBrokerChannel ? brokerChannel : clientChannel, channel);
		}

		@Override
		public void handle(MqttChannel channel, ConnAckMessage message) {
			messagesReceived.add(message);
			assertSame(isBrokerChannel ? brokerChannel : clientChannel, channel);
		}

		@Override
		public void handle(MqttChannel channel, PublishMessage message) {
			messagesReceived.add(message);
			assertSame(isBrokerChannel ? brokerChannel : clientChannel, channel);
		}

		@Override
		public void handle(MqttChannel channel, PubAckMessage message) {
			messagesReceived.add(message);
			assertSame(isBrokerChannel ? brokerChannel : clientChannel, channel);
		}

		@Override
		public void handle(MqttChannel channel, PubRecMessage message) {
			messagesReceived.add(message);
			assertSame(isBrokerChannel ? brokerChannel : clientChannel, channel);
		}

		@Override
		public void handle(MqttChannel channel, PubRelMessage message) {
			messagesReceived.add(message);
			assertSame(isBrokerChannel ? brokerChannel : clientChannel, channel);
		}

		@Override
		public void handle(MqttChannel channel, PubCompMessage message) {
			messagesReceived.add(message);
			assertSame(isBrokerChannel ? brokerChannel : clientChannel, channel);
		}

		@Override
		public void handle(MqttChannel channel, SubscribeMessage message) {
			messagesReceived.add(message);
			assertSame(isBrokerChannel ? brokerChannel : clientChannel, channel);
		}

		@Override
		public void handle(MqttChannel channel, SubAckMessage message) {
			messagesReceived.add(message);
			assertSame(isBrokerChannel ? brokerChannel : clientChannel, channel);
		}

		@Override
		public void handle(MqttChannel channel, UnsubscribeMessage message) {
			messagesReceived.add(message);
			assertSame(isBrokerChannel ? brokerChannel : clientChannel, channel);
		}

		@Override
		public void handle(MqttChannel channel, UnsubAckMessage message) {
			throw new RuntimeException();
		}

		@Override
		public void handle(MqttChannel channel, PingReqMessage message) {
			messagesReceived.add(message);
			assertSame(isBrokerChannel ? brokerChannel : clientChannel, channel);
		}

		@Override
		public void handle(MqttChannel channel, PingRespMessage message) {
			messagesReceived.add(message);
			assertSame(isBrokerChannel ? brokerChannel : clientChannel, channel);
		}

		@Override
		public void handle(MqttChannel channel, DisconnectMessage message) {
			messagesReceived.add(message);
			assertSame(isBrokerChannel ? brokerChannel : clientChannel, channel);
		}

		@Override
		public void channelClosed(MqttChannel channel) {
		}
	}
}
