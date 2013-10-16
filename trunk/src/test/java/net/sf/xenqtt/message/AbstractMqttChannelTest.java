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
package net.sf.xenqtt.message;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.sf.xenqtt.mock.MockMessageHandler;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AbstractMqttChannelTest extends MqttChannelTestBase<MqttChannelTestBase<?, ?>.TestChannel, MqttChannelTestBase<?, ?>.TestChannel> {

	@Mock BlockingCommand<MqttMessage> blockingCommand;

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
	}

	/**
	 * @see net.sf.xenqtt.message.MqttChannelTestBase#newClientChannel(net.sf.xenqtt.message.BlockingCommand)
	 */
	@Override
	TestChannel newClientChannel(BlockingCommand<?> connectionCompleteCommand) throws Exception {
		return new TestChannel("localhost", port, clientHandler, selector, 10000, connectionCompleteCommand);
	}

	@Override
	TestChannel newBrokerChannel(SocketChannel brokerSocketChannel) throws Exception {
		return new TestChannel(brokerSocketChannel, brokerHandler, selector, 10000);
	}

	@Test
	public void testClose_WithCause() throws Exception {

		RuntimeException e = new RuntimeException();
		establishConnection();
		clientChannel.close(e);
		assertSame(e, clientHandler.lastChannelClosedCause());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testCancelBlockingCommands() throws Exception {
		clientChannel = newClientChannel(blockingCommand);

		BlockingCommand<MqttMessage> connAckReceivedCommand = mock(BlockingCommand.class);
		setField(clientChannel, "connAckReceivedCommand", connAckReceivedCommand);

		MqttMessage sendMessageInProgress = new MqttMessage(MessageType.PUBLISH, 0);
		sendMessageInProgress.blockingCommand = mock(BlockingCommand.class);
		setField(clientChannel, "sendMessageInProgress", sendMessageInProgress);

		List<MqttMessage> writesPendingMessages = new ArrayList<MqttMessage>();
		writesPendingMessages.add(new MqttMessage(MessageType.PUBLISH, 0));
		writesPendingMessages.add(new MqttMessage(MessageType.PUBLISH, 0));
		writesPendingMessages.add(new MqttMessage(MessageType.PUBLISH, 0));
		writesPendingMessages.get(0).blockingCommand = mock(BlockingCommand.class);
		writesPendingMessages.get(1).blockingCommand = mock(BlockingCommand.class);
		writesPendingMessages.get(2).blockingCommand = mock(BlockingCommand.class);
		addMessages(clientChannel, "writesPending", writesPendingMessages);

		List<MqttMessage> messagesToResend = new ArrayList<MqttMessage>();
		messagesToResend.add(new IdentifiableMqttMessage(MessageType.PUBLISH, 0) {
		});
		messagesToResend.add(new IdentifiableMqttMessage(MessageType.PUBLISH, 0) {
		});
		messagesToResend.add(new IdentifiableMqttMessage(MessageType.PUBLISH, 0) {
		});
		messagesToResend.get(0).blockingCommand = mock(BlockingCommand.class);
		messagesToResend.get(1).blockingCommand = mock(BlockingCommand.class);
		messagesToResend.get(2).blockingCommand = mock(BlockingCommand.class);
		addMessages(clientChannel, "messagesToResend", messagesToResend);

		Map<Integer, IdentifiableMqttMessage> inFlightMessages = new HashMap<Integer, IdentifiableMqttMessage>();
		PubMessage pubMessage = new PubMessage(QoS.AT_LEAST_ONCE, false, "a/b/c", 0, new byte[] { 97, 98, 99 });
		pubMessage.blockingCommand = mock(BlockingCommand.class);
		inFlightMessages.put(Integer.valueOf(0), pubMessage);
		pubMessage = new PubMessage(QoS.AT_LEAST_ONCE, false, "a/b/c", 1, new byte[] { 97, 98, 99 });
		pubMessage.blockingCommand = mock(BlockingCommand.class);
		inFlightMessages.put(Integer.valueOf(1), pubMessage);
		pubMessage = new PubMessage(QoS.AT_LEAST_ONCE, false, "a/b/c", 2, new byte[] { 97, 98, 99 });
		pubMessage.blockingCommand = mock(BlockingCommand.class);
		inFlightMessages.put(Integer.valueOf(2), pubMessage);
		setInFlightMessages(clientChannel, inFlightMessages);

		clientChannel.cancelBlockingCommands();
		verify(blockingCommand).cancel();
		verify(connAckReceivedCommand).cancel();
		verify(sendMessageInProgress.blockingCommand).cancel();
		for (MqttMessage writePendingMessage : writesPendingMessages) {
			verify(writePendingMessage.blockingCommand).cancel();
		}
		for (MqttMessage messageToResend : messagesToResend) {
			verify(messageToResend.blockingCommand).cancel();
		}
		for (IdentifiableMqttMessage inFlightMessage : inFlightMessages.values()) {
			verify(inFlightMessage.blockingCommand).cancel();
		}
		verifyNoMoreInteractions(allOf(writesPendingMessages, messagesToResend, inFlightMessages, blockingCommand, connAckReceivedCommand,
				sendMessageInProgress.blockingCommand));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testCancelBlockingCommands_SomeMessagesDoNotHaveABlockingCommand() throws Exception {
		clientChannel = newClientChannel(null);
		setField(clientChannel, "connAckReceivedCommand", null);

		MqttMessage sendMessageInProgress = new MqttMessage(MessageType.PUBLISH, 0);
		sendMessageInProgress.blockingCommand = null;
		setField(clientChannel, "sendMessageInProgress", sendMessageInProgress);

		List<MqttMessage> writesPendingMessages = new ArrayList<MqttMessage>();
		writesPendingMessages.add(new MqttMessage(MessageType.PUBLISH, 0));
		writesPendingMessages.add(new MqttMessage(MessageType.PUBLISH, 0));
		writesPendingMessages.add(new MqttMessage(MessageType.PUBLISH, 0));
		writesPendingMessages.get(0).blockingCommand = mock(BlockingCommand.class);
		writesPendingMessages.get(1).blockingCommand = null;
		writesPendingMessages.get(2).blockingCommand = mock(BlockingCommand.class);
		addMessages(clientChannel, "writesPending", writesPendingMessages);

		List<MqttMessage> messagesToResend = new ArrayList<MqttMessage>();
		messagesToResend.add(new IdentifiableMqttMessage(MessageType.PUBLISH, 0) {
		});
		messagesToResend.add(new IdentifiableMqttMessage(MessageType.PUBLISH, 0) {
		});
		messagesToResend.add(new IdentifiableMqttMessage(MessageType.PUBLISH, 0) {
		});
		messagesToResend.get(0).blockingCommand = mock(BlockingCommand.class);
		messagesToResend.get(1).blockingCommand = mock(BlockingCommand.class);
		messagesToResend.get(2).blockingCommand = null;
		addMessages(clientChannel, "messagesToResend", messagesToResend);

		Map<Integer, IdentifiableMqttMessage> inFlightMessages = new HashMap<Integer, IdentifiableMqttMessage>();
		PubMessage pubMessage = new PubMessage(QoS.AT_LEAST_ONCE, false, "a/b/c", 0, new byte[] { 97, 98, 99 });
		pubMessage.blockingCommand = null;
		inFlightMessages.put(Integer.valueOf(0), pubMessage);
		pubMessage = new PubMessage(QoS.AT_LEAST_ONCE, false, "a/b/c", 1, new byte[] { 97, 98, 99 });
		pubMessage.blockingCommand = mock(BlockingCommand.class);
		inFlightMessages.put(Integer.valueOf(1), pubMessage);
		pubMessage = new PubMessage(QoS.AT_LEAST_ONCE, false, "a/b/c", 2, new byte[] { 97, 98, 99 });
		pubMessage.blockingCommand = mock(BlockingCommand.class);
		inFlightMessages.put(Integer.valueOf(2), pubMessage);
		setInFlightMessages(clientChannel, inFlightMessages);

		clientChannel.cancelBlockingCommands();
		for (MqttMessage writePendingMessage : writesPendingMessages) {
			if (writePendingMessage.blockingCommand != null) {
				verify(writePendingMessage.blockingCommand).cancel();
			}
		}
		for (MqttMessage messageToResend : messagesToResend) {
			if (messageToResend.blockingCommand != null) {
				verify(messageToResend.blockingCommand).cancel();
			}
		}
		for (IdentifiableMqttMessage inFlightMessage : inFlightMessages.values()) {
			if (inFlightMessage.blockingCommand != null) {
				verify(inFlightMessage.blockingCommand).cancel();
			}
		}

		verifyZeroInteractions(blockingCommand);
		verifyNoMoreInteractions(allOf(writesPendingMessages, messagesToResend, inFlightMessages));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testCancelBlockingCommands_CancelsInFlightMessagesBeforeAcked() throws Exception {
		establishConnection();

		BlockingCommand<MqttMessage> command1 = mock(BlockingCommand.class);
		BlockingCommand<MqttMessage> command2 = mock(BlockingCommand.class);
		assertTrue(clientChannel.send(new UnsubscribeMessage(1, new String[] { "foo" }), command1));
		assertTrue(clientChannel.send(new UnsubscribeMessage(2, new String[] { "foo" }), command2));

		readWrite(0, 2);
		assertEquals(2, clientChannel.getUnsentMessages().size());

		clientChannel.cancelBlockingCommands();

		verify(command1, timeout(1000)).cancel();
		verify(command2, timeout(1000)).cancel();
		verifyNoMoreInteractions(command1, command2);
	}

	@SuppressWarnings("rawtypes")
	private void setField(net.sf.xenqtt.message.MqttChannelTestBase.TestChannel clientChannel, String fieldName, Object value) throws Exception {
		Field field = AbstractMqttChannel.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(clientChannel, value);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void addMessages(net.sf.xenqtt.message.MqttChannelTestBase.TestChannel clientChannel, String messagesField, List<MqttMessage> messages)
			throws Exception {
		Field field = AbstractMqttChannel.class.getDeclaredField(messagesField);
		field.setAccessible(true);
		Collection<MqttMessage> writesPending = (Collection<MqttMessage>) field.get(clientChannel);
		for (MqttMessage message : messages) {
			assertTrue(writesPending.add(message));
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void setInFlightMessages(net.sf.xenqtt.message.MqttChannelTestBase.TestChannel clientChannel, Map<Integer, IdentifiableMqttMessage> messages)
			throws Exception {
		Field field = AbstractMqttChannel.class.getDeclaredField("inFlightMessages");
		field.setAccessible(true);
		Map<Integer, IdentifiableMqttMessage> inFlightMessages = (Map<Integer, IdentifiableMqttMessage>) field.get(clientChannel);
		inFlightMessages.clear();
		for (Entry<Integer, IdentifiableMqttMessage> entry : messages.entrySet()) {
			inFlightMessages.put(entry.getKey(), entry.getValue());
		}
	}

	private Object[] allOf(List<MqttMessage> writesPendingMessages, List<MqttMessage> messagesToResend, Map<Integer, IdentifiableMqttMessage> inFlightMessages,
			BlockingCommand<?>... others) {
		List<BlockingCommand<?>> blockingCommands = new ArrayList<BlockingCommand<?>>();
		for (MqttMessage message : writesPendingMessages) {
			if (message.blockingCommand != null) {
				blockingCommands.add(message.blockingCommand);
			}
		}
		for (MqttMessage message : messagesToResend) {
			if (message.blockingCommand != null) {
				blockingCommands.add(message.blockingCommand);
			}
		}
		for (MqttMessage message : inFlightMessages.values()) {
			if (message.blockingCommand != null) {
				blockingCommands.add(message.blockingCommand);
			}
		}
		for (BlockingCommand<?> other : others) {
			if (other != null) {
				blockingCommands.add(other);
			}
		}

		return blockingCommands.toArray(new BlockingCommand<?>[0]);
	}

	@Test
	public void testGetUnsentMessages_NoUnsentMessages() throws Exception {

		clientChannel = newClientChannel(null);
		assertTrue(clientChannel.getUnsentMessages().isEmpty());
	}

	@Test
	public void testGetUnsentMessages_UnsentMessages() throws Exception {

		establishConnection();

		assertTrue(clientChannel.send(new UnsubscribeMessage(1, new String[] { "foo" }), null));
		assertTrue(clientChannel.send(new UnsubscribeMessage(2, new String[] { "foo" }), null));

		readWrite(0, 2);

		// put a value in sendMessageInProgress directly because if we call send(...) it will write directly
		Field field = AbstractMqttChannel.class.getDeclaredField("sendMessageInProgress");
		field.setAccessible(true);
		field.set(clientChannel, new UnsubscribeMessage(3, new String[] { "foo" }));

		assertTrue(clientChannel.send(new UnsubscribeMessage(4, new String[] { "foo" }), null));
		assertTrue(clientChannel.send(new UnsubscribeMessage(5, new String[] { "foo" }), null));

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

	@Test(expected = IllegalStateException.class)
	public void testRegister_ChannelAlreadyRegistered() throws Exception {

		establishConnection();

		clientChannel.register(null, clientHandler);
	}

	@Test
	public void testRegister_Fails() throws Exception {

		establishConnection();

		clientChannel.deregister();

		assertFalse(clientChannel.register(null, clientHandler));
		clientHandler.assertChannelAttachedCount(0);
	}

	@Test
	public void testRegister_Succeeds() throws Exception {

		establishConnection();

		Selector newSelector = Selector.open();
		assertEquals(0, newSelector.keys().size());

		clientHandler = new MockMessageHandler();
		clientChannel.deregister();

		assertTrue(clientChannel.register(newSelector, clientHandler));
		assertEquals(1, newSelector.keys().size());
		clientHandler.assertChannelAttachedCount(1);

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
		clientHandler.assertChannelDetachedCount(1);

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

		PubMessage msg = new PubMessage(QoS.AT_MOST_ONCE, false, "foo", 12, new byte[] { 1, 2, 3 });

		assertTrue(clientChannel.send(msg, null));
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

		PubMessage msg = new PubMessage(QoS.AT_MOST_ONCE, false, "foo", 12, new byte[] { 1, 2, 3 });

		assertTrue(clientChannel.send(msg, null));
		readWrite(0, 1);
		brokerHandler.assertMessages(msg);
		assertFalse(brokerHandler.message(0).isDuplicate());
		assertEquals(0, clientChannel.inFlightMessageCount());
	}

	@Test
	public void testHouseKeeping_ResendMessage_qos1() throws Exception {

		clientChannel = new TestChannel("localhost", port, clientHandler, selector, 15000);

		establishConnection();

		PubMessage msg = new PubMessage(QoS.AT_LEAST_ONCE, false, "foo", 12, new byte[] { 1, 2, 3 });

		assertTrue(clientChannel.send(msg, null));
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

		assertTrue(brokerChannel.send(new PubAckMessage(12), null));
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
		clientHandler.assertChannelClosedCount(1);
	}

	@Test
	public void testSendToClosedConnection() throws Exception {

		establishConnection();
		clientChannel.close();

		assertFalse(clientChannel.send(new PingReqMessage(), null));
	}

	@Test
	public void testEstabishConnection_NonBlocking() throws Exception {

		establishConnection();

		assertTrue(clientChannel.isOpen());
		assertFalse(clientChannel.isConnected());
		assertFalse(clientChannel.connectedCalled);
		assertEquals(0, clientChannel.pingIntervalMillis);
		assertFalse(brokerChannel.isConnected());
		assertFalse(brokerChannel.connectedCalled);
		assertEquals(0, brokerChannel.pingIntervalMillis);

		assertFalse(clientChannel.disconnectedCalled);
		assertFalse(brokerChannel.disconnectedCalled);

		closeConnection();

		assertFalse(clientChannel.disconnectedCalled);
		assertFalse(brokerChannel.disconnectedCalled);
	}

	@Test
	public void testEstabishConnection_Blocking() throws Exception {

		establishConnection(blockingCommand);

		verify(blockingCommand).complete();

		assertTrue(clientChannel.isOpen());
		assertFalse(clientChannel.isConnected());
		assertFalse(clientChannel.connectedCalled);
		assertEquals(0, clientChannel.pingIntervalMillis);
		assertFalse(brokerChannel.isConnected());
		assertFalse(brokerChannel.connectedCalled);
		assertEquals(0, brokerChannel.pingIntervalMillis);

		assertFalse(clientChannel.disconnectedCalled);
		assertFalse(brokerChannel.disconnectedCalled);

		closeConnection();

		assertFalse(clientChannel.disconnectedCalled);
		assertFalse(brokerChannel.disconnectedCalled);
	}

	@Test
	public void testReadWriteSend_Disconnect() throws Exception {

		establishConnection();

		DisconnectMessage discMsg = new DisconnectMessage();

		assertTrue(clientChannel.send(discMsg, null));
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
	public void testReadWriteSend_ConnAckWithoutAccept_NonBlocking() throws Exception {

		establishConnection();

		ConnectMessage connMsg = new ConnectMessage("abc", false, 123);
		ConnAckMessage ackMsg = new ConnAckMessage(ConnectReturnCode.BAD_CREDENTIALS);

		assertTrue(clientChannel.send(connMsg, null));
		readWrite(0, 1);
		brokerHandler.assertMessages(connMsg);

		assertTrue(brokerChannel.send(ackMsg, null));
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
	public void testReadWriteSend_ConnAckWithoutAccept_Blocking() throws Exception {

		establishConnection();

		ConnectMessage connMsg = new ConnectMessage("abc", false, 123);
		ConnAckMessage ackMsg = new ConnAckMessage(ConnectReturnCode.BAD_CREDENTIALS);

		assertTrue(clientChannel.send(connMsg, blockingCommand));
		readWrite(0, 1);
		brokerHandler.assertMessages(connMsg);

		verifyZeroInteractions(blockingCommand);

		assertTrue(brokerChannel.send(ackMsg, null));
		readWrite(1, 0);
		clientHandler.assertMessages(ackMsg);

		verify(blockingCommand).complete();
		verify(blockingCommand).setResult(ackMsg);

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
	public void testReadWriteSend_ConnAckWithAccept_NonBlocking() throws Exception {

		establishConnection();

		ConnectMessage connMsg = new ConnectMessage("abc", false, 123);
		ConnAckMessage ackMsg = new ConnAckMessage(ConnectReturnCode.ACCEPTED);

		assertTrue(clientChannel.send(connMsg, null));
		readWrite(0, 1);
		brokerHandler.assertMessages(connMsg);

		assertTrue(brokerChannel.send(ackMsg, null));
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
	public void testReadWriteSend_ConnAckWithAccept_Blocking() throws Exception {

		establishConnection();

		ConnectMessage connMsg = new ConnectMessage("abc", false, 123);
		ConnAckMessage ackMsg = new ConnAckMessage(ConnectReturnCode.ACCEPTED);

		assertTrue(clientChannel.send(connMsg, blockingCommand));
		readWrite(0, 1);
		brokerHandler.assertMessages(connMsg);

		verifyZeroInteractions(blockingCommand);

		assertTrue(brokerChannel.send(ackMsg, null));
		readWrite(1, 0);
		clientHandler.assertMessages(ackMsg);

		verify(blockingCommand).complete();
		verify(blockingCommand).setResult(ackMsg);

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

		assertTrue(clientChannel.send(pingReqMsg, null));
		readWrite(0, 1);
		brokerHandler.assertMessages(pingReqMsg);

		assertTrue(brokerChannel.send(pingRespMsg, null));
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

		assertTrue(clientChannel.send(pingReqMsg, null));
		assertTrue(clientChannel.send(msg2, null));
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

		assertTrue(clientChannel.send(pingRespMsg, null));
		assertTrue(clientChannel.send(msg2, null));
		readWrite(0, 1);
		brokerHandler.assertMessages(msg2);

		closeConnection();
	}

	@Test
	public void testSend_NotConnectedYet() throws Exception {

		PingReqMessage msg = new PingReqMessage();

		clientChannel = new TestChannel("localhost", port, clientHandler, selector, 10000);
		assertFalse(clientChannel.send(msg, null));

		establishConnection();

		readWrite(0, 1);

		closeConnection();
	}

	@Test
	public void testReadWriteSend_PublishAndAck_Qos1_NonBlocking() throws Exception {

		PubMessage msg = new PubMessage(QoS.AT_LEAST_ONCE, false, "foo", 1, new byte[] {});
		PubAckMessage ack = new PubAckMessage(1);
		doTestReadWriteSend_NonBlocking(msg, ack);
	}

	@Test
	public void testReadWriteSend_PublishAndAck_Qos1_Blocking() throws Exception {

		PubMessage msg = new PubMessage(QoS.AT_LEAST_ONCE, false, "foo", 1, new byte[] {});
		PubAckMessage ack = new PubAckMessage(1);
		doTestReadWriteSend_Blocking(msg, ack);
	}

	@Test
	public void testReadWriteSend_Publish_Qos0_NonBlocking() throws Exception {

		PubMessage msg = new PubMessage(QoS.AT_MOST_ONCE, false, "foo", 0, new byte[] {});
		doTestReadWriteSend_NonBlocking(msg, null);
	}

	@Test
	public void testReadWriteSend_Publish_Qos0_Blocking() throws Exception {

		PubMessage msg = new PubMessage(QoS.AT_MOST_ONCE, false, "foo", 0, new byte[] {});
		doTestReadWriteSend_Blocking(msg, null);
	}

	@Test
	public void testReadWriteSend_PublishAndPubRec_NonBlocking() throws Exception {

		PubMessage msg = new PubMessage(QoS.AT_LEAST_ONCE, false, "foo", 1, new byte[] {});
		PubRecMessage ack = new PubRecMessage(1);
		doTestReadWriteSend_NonBlocking(msg, ack);
	}

	@Test
	public void testReadWriteSend_PublishAndPubRec_Blocking() throws Exception {

		PubMessage msg = new PubMessage(QoS.AT_LEAST_ONCE, false, "foo", 1, new byte[] {});
		PubRecMessage ack = new PubRecMessage(1);
		doTestReadWriteSend_Blocking(msg, ack);
	}

	@Test
	public void testReadWriteSend_PubRelAndPubComp_NonBlocking() throws Exception {

		PubRelMessage msg = new PubRelMessage(1);
		PubCompMessage ack = new PubCompMessage(1);
		doTestReadWriteSend_NonBlocking(msg, ack);
	}

	@Test
	public void testReadWriteSend_PubRelAndPubComp_Blocking() throws Exception {

		PubRelMessage msg = new PubRelMessage(1);
		PubCompMessage ack = new PubCompMessage(1);
		doTestReadWriteSend_Blocking(msg, ack);
	}

	@Test
	public void testReadWriteSend_SubscribeAndAck_NonBlocking() throws Exception {

		SubscribeMessage msg = new SubscribeMessage(1, new String[] {}, new QoS[] {});
		SubAckMessage ack = new SubAckMessage(1, new QoS[] {});
		doTestReadWriteSend_NonBlocking(msg, ack);
	}

	@Test
	public void testReadWriteSend_SubscribeAndAck_Blocking() throws Exception {

		SubscribeMessage msg = new SubscribeMessage(1, new String[] {}, new QoS[] {});
		SubAckMessage ack = new SubAckMessage(1, new QoS[] {});
		doTestReadWriteSend_Blocking(msg, ack);
	}

	@Test
	public void testReadWriteSend_UnsubscribeAndAck_NonBlocking() throws Exception {

		UnsubscribeMessage msg = new UnsubscribeMessage(1, new String[] {});
		UnsubAckMessage ack = new UnsubAckMessage(1);
		doTestReadWriteSend_NonBlocking(msg, ack);
	}

	@Test
	public void testReadWriteSend_UnsubscribeAndAck_Blocking() throws Exception {

		UnsubscribeMessage msg = new UnsubscribeMessage(1, new String[] {});
		UnsubAckMessage ack = new UnsubAckMessage(1);
		doTestReadWriteSend_Blocking(msg, ack);
	}

	@Test
	public void testReadWriteSend_PingReq_NonBlocking() throws Exception {

		PingReqMessage msg = new PingReqMessage();
		doTestReadWriteSend_NonBlocking(msg, null);
	}

	@Test
	public void testReadWriteSend_PingReq_Blocking() throws Exception {

		PingReqMessage msg = new PingReqMessage();
		doTestReadWriteSend_Blocking(msg, null);
	}

	@Test
	public void testReadWriteSend_PingResp_NonBlocking() throws Exception {

		PingRespMessage msg = new PingRespMessage();
		doTestReadWriteSend_NonBlocking(msg, null);
	}

	@Test
	public void testReadWriteSend_PingResp_Blocking() throws Exception {

		PingRespMessage msg = new PingRespMessage();
		doTestReadWriteSend_Blocking(msg, null);
	}

	@Test
	public void testReadWriteSend_Disconnect_NonBlocking() throws Exception {

		DisconnectMessage msg = new DisconnectMessage();
		doTestReadWriteSend_NonBlocking(msg, null);
	}

	@Test
	public void testReadWriteSend_Disconnect_Blocking() throws Exception {

		DisconnectMessage msg = new DisconnectMessage();
		doTestReadWriteSend_Blocking(msg, null);
	}

	@Test
	public void testReadWriteSend_HandlerThrowsException() throws Exception {

		establishConnection();

		brokerHandler.setException(new RuntimeException());

		UnsubAckMessage msg1 = new UnsubAckMessage(1);
		PingReqMessage msg2 = new PingReqMessage();

		assertTrue(clientChannel.send(msg1, null));
		assertTrue(clientChannel.send(msg2, null));

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
			clientChannel.send(new PingReqMessage(), null);
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

		assertTrue(clientChannel.send(msg, null));
		readWrite(0, 1);

		assertEquals(1, stats.getMessagesSent());

		closeConnection();
	}

	@Test
	public void testReadWriteSend_RemainingLength2() throws Exception {

		establishConnection();

		PubAckMessage msg = new PubAckMessage(123);

		assertTrue(clientChannel.send(msg, null));
		readWrite(0, 1);

		assertEquals(1, stats.getMessagesSent());

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

		List<PubMessage> messagesSent = new ArrayList<PubMessage>();

		establishConnection();

		for (int remainingLength = firstRemainingLength; remainingLength < firstRemainingLength + messageCount; remainingLength++) {
			int payloadLength = remainingLength - 7;
			byte[] payload = new byte[payloadLength];
			Arrays.fill(payload, (byte) messageCount);

			PubMessage msg = new PubMessage(QoS.AT_LEAST_ONCE, false, "abc", 123, payload);

			assertTrue(clientChannel.send(msg, null));
			messagesSent.add(msg);
		}

		readWrite(0, messageCount);

		brokerHandler.assertMessages(messagesSent);

		assertEquals(messagesSent.size(), stats.getMessagesSent());

		closeConnection();
	}

	private void doTestReadWriteSend_NonBlocking(MqttMessage msg, MqttMessage ack) throws Exception {
		establishConnection();

		int messagesSent = 1;
		clientChannel.send(msg, null);
		readWrite(0, 1);
		brokerHandler.assertMessages(msg);

		if (ack != null) {
			messagesSent++;
			brokerChannel.send(ack, null);
			readWrite(1, 0);
			clientHandler.assertMessages(ack);
			if (ack instanceof PubAckMessage) {
				assertTrue(stats.getMinAckLatencyMillis() > 0);
				assertTrue(stats.getMaxAckLatencyMillis() > 0);
				assertTrue(stats.getAverageAckLatencyMillis() > 0.0);
				assertTrue(stats.getMinAckLatencyMillis() <= stats.getAverageAckLatencyMillis());
				assertTrue(stats.getAverageAckLatencyMillis() <= stats.getMaxAckLatencyMillis());
			}
		}

		assertEquals(messagesSent, stats.getMessagesSent());

		closeConnection();
	}

	private void doTestReadWriteSend_Blocking(MqttMessage msg, MqttMessage ack) throws Exception {

		establishConnection();

		int messagesSent = 1;
		clientChannel.send(msg, blockingCommand);
		readWrite(0, 1);
		brokerHandler.assertMessages(msg);

		if (ack != null) {
			messagesSent++;
			verifyZeroInteractions(blockingCommand);

			brokerChannel.send(ack, null);
			readWrite(1, 0);
			clientHandler.assertMessages(ack);
			verify(blockingCommand).setResult(ack);
			if (ack instanceof PubAckMessage) {
				assertTrue(stats.getMinAckLatencyMillis() > 0);
				assertTrue(stats.getMaxAckLatencyMillis() > 0);
				assertTrue(stats.getAverageAckLatencyMillis() > 0.0);
				assertTrue(stats.getMinAckLatencyMillis() <= stats.getAverageAckLatencyMillis());
				assertTrue(stats.getAverageAckLatencyMillis() <= stats.getMaxAckLatencyMillis());
			}
		}

		verify(blockingCommand).complete();

		assertEquals(messagesSent, stats.getMessagesSent());

		closeConnection();
	}
}
