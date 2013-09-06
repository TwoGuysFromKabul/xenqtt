package net.sf.xenqtt.message;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.xenqtt.mock.MockMessageHandler;

import org.junit.Test;

public class AbstractMqttChannelTest extends MqttChannelTestBase<MqttChannelTestBase<?, ?>.TestChannel, MqttChannelTestBase<?, ?>.TestChannel> {

	@Override
	TestChannel newClientChannel() throws Exception {
		return new TestChannel("localhost", port, clientHandler, selector, 10000);
	}

	@Override
	TestChannel newBrokerChannel(SocketChannel brokerSocketChannel) throws Exception {
		return new TestChannel(brokerSocketChannel, brokerHandler, selector, 10000);
	}

	@Test
	public void testGetUnsentMessages_NoUnsentMessages() throws Exception {

		clientChannel = newClientChannel();
		assertTrue(clientChannel.getUnsentMessages().isEmpty());
	}

	@Test
	public void testGetUnsentMessages_UnsentMessages() throws Exception {

		establishConnection();

		assertTrue(clientChannel.send(new UnsubscribeMessage(1, new String[] { "foo" })));
		assertTrue(clientChannel.send(new UnsubscribeMessage(2, new String[] { "foo" })));

		readWrite(0, 2);

		// put a value in sendMessageInProgress directly because if we call send(...) it will write directly
		Field field = AbstractMqttChannel.class.getDeclaredField("sendMessageInProgress");
		field.setAccessible(true);
		field.set(clientChannel, new UnsubscribeMessage(3, new String[] { "foo" }));

		assertTrue(clientChannel.send(new UnsubscribeMessage(4, new String[] { "foo" })));
		assertTrue(clientChannel.send(new UnsubscribeMessage(5, new String[] { "foo" })));

		List<MqttMessage> unsent = clientChannel.getUnsentMessages();

		Set<Integer> ids = new HashSet<Integer>();
		for (MqttMessage m : unsent) {
			ids.add(((IdentifiableMqttMessage) m).getMessageId());
		}

		assertEquals(5, unsent.size());
		assertEquals(5, ids.size());
		for (int i = 1; i <= 5; i++) {
			assertTrue(ids.contains(i));
		}
	}

	@Test
	public void testCtors() throws Exception {

		establishConnection();

		assertFalse(clientChannel.isConnectionPending());
		assertTrue(clientChannel.isOpen());
		clientHandler.assertChannelOpenedCount(1);

		assertFalse(brokerChannel.isConnectionPending());
		assertTrue(brokerChannel.isOpen());
		brokerHandler.assertChannelOpenedCount(1);

		closeConnection();
	}

	@Test
	public void testCtorInvalidHost() throws Exception {

		try {
			new TestChannel("foo", 123, clientHandler, selector, 10000);
			fail("Expected exception");
		} catch (UnresolvedAddressException e) {
			clientHandler.assertChannelOpenedCount(0);
			clientHandler.assertChannelClosedCount(1);
			clientHandler.assertLastChannelClosedCause(e);
		}
	}

	@Test
	public void testCtorInvalidConnection() throws Exception {

		try {
			new TestChannel(null, brokerHandler, null, 10000);
			fail("Expected exception");
		} catch (NullPointerException e) {
			brokerHandler.assertChannelOpenedCount(0);
			brokerHandler.assertChannelClosedCount(1);
			brokerHandler.assertLastChannelClosedCause(e);
		}
	}

	@Test
	public void testRegister_Fails() throws Exception {

		establishConnection();

		assertFalse(clientChannel.register(null, clientHandler));
	}

	@Test
	public void testRegister_Succeeds() throws Exception {

		establishConnection();

		Selector newSelector = Selector.open();
		assertEquals(0, newSelector.keys().size());

		clientHandler = new MockMessageHandler();
		assertTrue(clientChannel.register(newSelector, clientHandler));
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
	public void testHouseKeeping_KeepAlive_ThrowsException() throws Exception {

		clientChannel = new TestChannel("localhost", port, clientHandler, selector, 10);

		clientChannel.exceptionToThrow = new RuntimeException("crap");

		clientChannel.houseKeeping(now + 100);
	}

	@Test
	public void testHouseKeeping_KeepAlive() throws Exception {

		clientChannel = new TestChannel("localhost", port, clientHandler, selector, 10);

		establishConnection();

		PublishMessage msg = new PublishMessage(QoS.AT_MOST_ONCE, false, "foo", 12, new byte[] { 1, 2, 3 });

		assertTrue(clientChannel.send(msg));
		readWrite(0, 1);

		assertEquals(25000, clientChannel.houseKeeping(now + 1000));
		assertEquals(25000, brokerChannel.houseKeeping(now + 1000));

		assertTrue(brokerChannel.lastReceived >= now && brokerChannel.lastReceived < now + 100);
		assertEquals(0, clientChannel.lastReceived);
	}

	@Test
	public void testSend_qos0() throws Exception {

		clientChannel = new TestChannel("localhost", port, clientHandler, selector, 15000);

		establishConnection();

		PublishMessage msg = new PublishMessage(QoS.AT_MOST_ONCE, false, "foo", 12, new byte[] { 1, 2, 3 });

		assertTrue(clientChannel.send(msg));
		readWrite(0, 1);
		brokerHandler.assertMessages(msg);
		assertFalse(brokerHandler.message(0).isDuplicate());
		assertEquals(0, clientChannel.inFlightMessageCount());
	}

	@Test
	public void testHouseKeeping_ResendMessage_qos1() throws Exception {

		clientChannel = new TestChannel("localhost", port, clientHandler, selector, 15000);

		establishConnection();

		PublishMessage msg = new PublishMessage(QoS.AT_LEAST_ONCE, false, "foo", 12, new byte[] { 1, 2, 3 });

		assertTrue(clientChannel.send(msg));
		readWrite(0, 1);
		brokerHandler.assertMessages(msg);
		assertFalse(brokerHandler.message(0).isDuplicate());
		assertEquals(1, clientChannel.inFlightMessageCount());

		// the time hasn't elapsed yet so we should get the time until next resend of the message
		assertEquals(15000, clientChannel.houseKeeping(now));
		assertEquals(1, clientChannel.inFlightMessageCount());

		// now the time has elapsed so we resend and get the time until the keep alive
		assertEquals(25000, clientChannel.houseKeeping(now + 15000));
		readWrite(0, 1);
		brokerHandler.assertMessageCount(1);
		assertEquals(msg.getMessageId(), ((IdentifiableMqttMessage) brokerHandler.message(0)).getMessageId());
		assertTrue(brokerHandler.message(0).isDuplicate());
		assertEquals(1, clientChannel.inFlightMessageCount());

		assertTrue(brokerChannel.send(new PubAckMessage(12)));
		readWrite(1, 0);
		assertEquals(0, clientChannel.inFlightMessageCount());
		assertEquals(0, brokerChannel.inFlightMessageCount());
	}

	@Test
	public void testHouseKeeping_ResendMessage_qos2() throws Exception {

		clientChannel = new TestChannel("localhost", port, clientHandler, selector, 15000);

		establishConnection();

		PublishMessage msg = new PublishMessage(QoS.EXACTLY_ONCE, false, "foo", 12, new byte[] { 1, 2, 3 });

		assertTrue(clientChannel.send(msg));
		readWrite(0, 1);
		brokerHandler.assertMessages(msg);
		assertFalse(brokerHandler.message(0).isDuplicate());
		assertEquals(1, clientChannel.inFlightMessageCount());

		// the time hasn't elapsed yet so we should get the time until next resend of the message
		assertEquals(15000, clientChannel.houseKeeping(now));
		assertEquals(1, clientChannel.inFlightMessageCount());

		// now the time has elapsed so we resend and get the time until the keep alive
		assertEquals(25000, clientChannel.houseKeeping(now + 15000));
		readWrite(0, 1);
		brokerHandler.assertMessageCount(1);
		assertEquals(msg.getMessageId(), ((IdentifiableMqttMessage) brokerHandler.message(0)).getMessageId());
		assertTrue(brokerHandler.message(0).isDuplicate());
		assertEquals(1, clientChannel.inFlightMessageCount());

		assertTrue(brokerChannel.send(new PubRecMessage(12)));
		readWrite(1, 0);
		assertEquals(0, clientChannel.inFlightMessageCount());
		assertEquals(0, brokerChannel.inFlightMessageCount());
	}

	@Test
	public void testReadFromClosedConnection() throws Exception {

		establishConnection();
		clientChannel.close();

		assertFalse(clientChannel.read(now));
	}

	@Test
	public void testWriteToClosedConnection() throws Exception {

		establishConnection();
		clientChannel.close();

		// put a value in sendMessageInProgress directly because if we call send(...) it will write directly
		Field field = AbstractMqttChannel.class.getDeclaredField("sendMessageInProgress");
		field.setAccessible(true);
		field.set(clientChannel, new PingReqMessage());

		assertFalse(clientChannel.write(now));
	}

	@Test
	public void testSendToClosedConnection() throws Exception {

		establishConnection();
		clientChannel.close();

		assertFalse(clientChannel.send(new PingReqMessage()));
	}

	@Test
	public void testReadWriteSend_Disconnect() throws Exception {

		establishConnection();

		DisconnectMessage discMsg = new DisconnectMessage();

		assertTrue(clientChannel.send(discMsg));
		readWrite(0, 1);
		brokerHandler.assertMessages(discMsg);

		assertFalse(clientChannel.isConnected());
		assertFalse(clientChannel.disconnectedCalled);
		assertFalse(brokerChannel.isConnected());
		assertFalse(brokerChannel.disconnectedCalled);
		assertFalse(clientChannel.isOpen());
		assertFalse(brokerChannel.isOpen());
	}

	@Test
	public void testReadWriteSend_ConnAckWithoutAccept() throws Exception {

		establishConnection();

		ConnectMessage connMsg = new ConnectMessage("abc", false, 123);
		ConnAckMessage ackMsg = new ConnAckMessage(ConnectReturnCode.BAD_CREDENTIALS);

		assertTrue(clientChannel.send(connMsg));
		readWrite(0, 1);
		brokerHandler.assertMessages(connMsg);

		assertTrue(brokerChannel.send(ackMsg));
		readWrite(1, 0);
		clientHandler.assertMessages(ackMsg);

		assertFalse(clientChannel.isConnected());
		assertFalse(clientChannel.connectedCalled);
		assertFalse(clientChannel.disconnectedCalled);
		assertFalse(clientChannel.isOpen());

		assertFalse(brokerChannel.isConnected());
		assertFalse(brokerChannel.connectedCalled);
		assertFalse(brokerChannel.disconnectedCalled);
		assertFalse(brokerChannel.isOpen());
	}

	@Test
	public void testReadWriteSend_ConnAckWithAccept() throws Exception {

		establishConnection();

		ConnectMessage connMsg = new ConnectMessage("abc", false, 123);
		ConnAckMessage ackMsg = new ConnAckMessage(ConnectReturnCode.ACCEPTED);

		assertTrue(clientChannel.send(connMsg));
		readWrite(0, 1);
		brokerHandler.assertMessages(connMsg);

		assertTrue(brokerChannel.send(ackMsg));
		readWrite(1, 0);
		clientHandler.assertMessages(ackMsg);

		assertTrue(clientChannel.isOpen());
		assertTrue(clientChannel.isConnected());
		assertTrue(clientChannel.connectedCalled);
		assertEquals(123000, clientChannel.pingIntervalMillis);
		assertTrue(brokerChannel.isConnected());
		assertTrue(brokerChannel.connectedCalled);
		assertEquals(123000, brokerChannel.pingIntervalMillis);

		assertFalse(clientChannel.disconnectedCalled);
		assertFalse(brokerChannel.disconnectedCalled);

		closeConnection();

		assertTrue(clientChannel.disconnectedCalled);
		assertTrue(brokerChannel.disconnectedCalled);
	}

	@Test
	public void testReadWriteSend_PingReqResp() throws Exception {

		establishConnection();

		PingReqMessage pingReqMsg = new PingReqMessage();
		PingRespMessage pingRespMsg = new PingRespMessage();

		assertTrue(clientChannel.send(pingReqMsg));
		readWrite(0, 1);
		brokerHandler.assertMessages(pingReqMsg);

		assertTrue(brokerChannel.send(pingRespMsg));
		readWrite(1, 0);
		clientHandler.assertMessages(pingRespMsg);

		clientChannel.close();
		brokerChannel.close();
	}

	@Test
	public void testReadWriteSend_PingReq_ThrowsException() throws Exception {

		establishConnection();

		brokerHandler.setException(new RuntimeException("crap"));

		PingReqMessage pingReqMsg = new PingReqMessage();
		PubAckMessage msg2 = new PubAckMessage(1);

		assertTrue(clientChannel.send(pingReqMsg));
		assertTrue(clientChannel.send(msg2));
		readWrite(0, 1);
		brokerHandler.assertMessages(msg2);

		closeConnection();
	}

	@Test
	public void testReadWriteSend_PingResp_ThrowsException() throws Exception {

		establishConnection();

		brokerHandler.setException(new RuntimeException("crap"));

		PingRespMessage pingRespMsg = new PingRespMessage();
		PubAckMessage msg2 = new PubAckMessage(1);

		assertTrue(clientChannel.send(pingRespMsg));
		assertTrue(clientChannel.send(msg2));
		readWrite(0, 1);
		brokerHandler.assertMessages(msg2);

		closeConnection();
	}

	@Test
	public void testSend_NotConnectedYet() throws Exception {

		PingReqMessage msg = new PingReqMessage();

		clientChannel = new TestChannel("localhost", port, clientHandler, selector, 10000);
		assertFalse(clientChannel.send(msg));

		establishConnection();

		readWrite(0, 1);

		closeConnection();
	}

	@Test
	public void testReadWriteSend_HandlerThrowsException() throws Exception {

		establishConnection();

		brokerHandler.setException(new RuntimeException());

		UnsubAckMessage msg1 = new UnsubAckMessage(1);
		PingReqMessage msg2 = new PingReqMessage();

		assertTrue(clientChannel.send(msg1));
		assertTrue(clientChannel.send(msg2));

		readWrite(0, 1);
		brokerHandler.assertMessages(msg2);

		closeConnection();
	}

	@Test
	public void testReadWriteSend_ClientClosesConnection() throws Exception {

		establishConnection();

		clientChannel.close();

		for (int i = 0; i < 100; i++) {
			if (!brokerChannel.read(now)) {
				return;
			}
		}

		fail("Expected the channel to close");
	}

	@Test
	public void testReadWriteSend_BrokerClosesConnection() throws Exception {

		establishConnection();

		brokerChannel.close();

		for (int i = 0; i < 100; i++) {
			if (!clientChannel.read(now)) {
				return;
			}
		}

		fail("Expected the channel to close");
	}

	@Test
	public void testReadWriteSend_IOException() throws Exception {

		establishConnection();

		brokerChannel.close();

		for (int i = 0; i < 1000; i++) {
			clientChannel.send(new PingReqMessage());
		}

		assertFalse(clientChannel.write(now));
		assertFalse(clientChannel.isOpen());
		clientHandler.assertChannelClosedCount(1);

		closeConnection();
	}

	@Test
	public void testReadWriteSend_RemainingLengthZero() throws Exception {

		establishConnection();

		PingReqMessage msg = new PingReqMessage();

		assertTrue(clientChannel.send(msg));
		readWrite(0, 1);

		closeConnection();
	}

	@Test
	public void testReadWriteSend_RemainingLength2() throws Exception {

		establishConnection();

		PubAckMessage msg = new PubAckMessage(123);

		assertTrue(clientChannel.send(msg));
		readWrite(0, 1);

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

			PublishMessage msg = new PublishMessage(QoS.AT_LEAST_ONCE, false, "abc", 123, payload);

			assertTrue(clientChannel.send(msg));
			messagesSent.add(msg);
		}

		readWrite(0, messageCount);

		brokerHandler.assertMessages(messagesSent);

		closeConnection();
	}
}
