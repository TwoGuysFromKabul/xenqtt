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
package net.xenqtt.proxy;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import net.xenqtt.message.ChannelManager;
import net.xenqtt.message.ConnAckMessage;
import net.xenqtt.message.ConnectMessage;
import net.xenqtt.message.ConnectReturnCode;
import net.xenqtt.message.DisconnectMessage;
import net.xenqtt.message.MqttChannel;
import net.xenqtt.message.MqttMessage;
import net.xenqtt.message.PubAckMessage;
import net.xenqtt.message.PubMessage;
import net.xenqtt.message.QoS;
import net.xenqtt.message.SubAckMessage;
import net.xenqtt.message.SubscribeMessage;
import net.xenqtt.message.UnsubAckMessage;
import net.xenqtt.message.UnsubscribeMessage;
import net.xenqtt.proxy.ProxySession;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ProxySessionTest {

	String brokerUri = "tcp://localhost:1883";
	ConnectMessage connectMessage = new ConnectMessage("client1", false, 10000);
	@Mock MqttChannel channelToBroker;
	@Mock MqttChannel channelToClient1;
	@Mock MqttChannel channelToClient2;
	@Mock ChannelManager manager;
	@Captor ArgumentCaptor<MqttMessage> messageCaptor;

	ProxySession session;

	@Before
	public void before() {

		MockitoAnnotations.initMocks(this);
		session = new ProxySession(brokerUri, connectMessage, manager, 0xffff);
		when(manager.newClientChannel(brokerUri, session)).thenReturn(channelToBroker);
	}

	@Test
	public void testInit() {

		session.init();

		verify(manager).init();
		verify(manager).newClientChannel(brokerUri, session);
	}

	@Test
	public void testShutdown() throws Exception {

		assertFalse(session.isClosed());
		session.shutdown();
		assertTrue(session.isClosed());
		verify(manager).shutdown();
		verifyNoMoreInteractions(manager);
	}

	@Test
	public void testNewConnection_SessionClosed() throws Exception {

		session.shutdown();
		assertFalse(session.newConnection(channelToClient1, connectMessage));
	}

	@Test
	public void testNewConnection_SessionNotClosed() throws Exception {

		assertTrue(session.newConnection(channelToClient1, connectMessage));
		verify(manager).attachChannel(channelToClient1, session);
		verifyNoMoreInteractions(manager);
	}

	@Test
	public void testConnAck_NotBrokerConnection() throws Exception {

		attachClientAndBroker();

		ConnAckMessage message = new ConnAckMessage(ConnectReturnCode.ACCEPTED);
		session.connAck(channelToClient1, message);

		verify(manager, never()).send(any(MqttChannel.class), any(MqttMessage.class));
	}

	@Test
	public void testConnAck_BrokerConnection_ConnectionNotAccepted() throws Exception {

		attachClientAndBroker();

		ConnAckMessage message = new ConnAckMessage(ConnectReturnCode.BAD_CREDENTIALS);
		session.connAck(channelToBroker, message);

		verify(channelToClient1).send(messageCaptor.capture());
		assertEquals(ConnectReturnCode.BAD_CREDENTIALS, ((ConnAckMessage) messageCaptor.getValue()).getReturnCode());
	}

	@Test
	public void testConnAck_BrokerConnection_Accepted() throws Exception {

		attachClientAndBroker();

		ConnAckMessage message = new ConnAckMessage(ConnectReturnCode.ACCEPTED);
		session.connAck(channelToBroker, message);

		verify(channelToClient1).send(messageCaptor.capture());
		assertEquals(ConnectReturnCode.ACCEPTED, ((ConnAckMessage) messageCaptor.getValue()).getReturnCode());
	}

	@Test
	public void testPublish_FromBrokerConnection_Qos0_SingleClientInCluster() throws Exception {

		connectClientAndBroker();

		PubMessage message = new PubMessage(QoS.AT_MOST_ONCE, false, "foo", 0, new byte[1]);
		session.publish(channelToBroker, message);
		verify(channelToClient1).send(same(message));
		assertEquals(0, message.getMessageId());
	}

	@Test
	public void testPublish_FromBrokerConnection_Qos1_SingleClientInCluster() throws Exception {

		connectClientAndBroker();

		PubMessage message = new PubMessage(QoS.AT_LEAST_ONCE, false, "foo", 123, new byte[1]);
		session.publish(channelToBroker, message);
		verify(channelToClient1).send(same(message));
		assertEquals(123, message.getMessageId());
	}

	@Test
	public void testPublish_FromBrokerConnection_Qos0_MutipleClientsInCluster() throws Exception {

		connectClientAndBroker();
		assertTrue(session.newConnection(channelToClient2, connectMessage));
		session.channelAttached(channelToClient2);

		PubMessage message = new PubMessage(QoS.AT_MOST_ONCE, false, "foo", 0, new byte[1]);
		session.publish(channelToBroker, message);
		verify(channelToClient1).send(same(message));
		assertEquals(0, message.getMessageId());

		message = new PubMessage(QoS.AT_MOST_ONCE, false, "foo", 0, new byte[1]);
		session.publish(channelToBroker, message);
		verify(channelToClient2).send(same(message));
		assertEquals(0, message.getMessageId());

		message = new PubMessage(QoS.AT_MOST_ONCE, false, "foo", 0, new byte[1]);
		session.publish(channelToBroker, message);
		verify(channelToClient1).send(same(message));
		assertEquals(0, message.getMessageId());

		message = new PubMessage(QoS.AT_MOST_ONCE, false, "foo", 0, new byte[1]);
		session.publish(channelToBroker, message);
		verify(channelToClient2).send(same(message));
		assertEquals(0, message.getMessageId());
	}

	@Test
	public void testPublish_FromBrokerConnection_Qos1_MutipleClientsInCluster() throws Exception {

		connectClientAndBroker();
		assertTrue(session.newConnection(channelToClient2, connectMessage));
		session.channelAttached(channelToClient2);

		PubMessage message = new PubMessage(QoS.AT_LEAST_ONCE, false, "foo", 123, new byte[1]);
		session.publish(channelToBroker, message);
		verify(channelToClient1).send(same(message));
		assertEquals(123, message.getMessageId());

		message = new PubMessage(QoS.AT_LEAST_ONCE, false, "foo", 456, new byte[1]);
		session.publish(channelToBroker, message);
		verify(channelToClient2).send(same(message));
		assertEquals(456, message.getMessageId());

		message = new PubMessage(QoS.AT_LEAST_ONCE, false, "foo", 321, new byte[1]);
		session.publish(channelToBroker, message);
		verify(channelToClient1).send(same(message));
		assertEquals(321, message.getMessageId());

		message = new PubMessage(QoS.AT_LEAST_ONCE, false, "foo", 654, new byte[1]);
		session.publish(channelToBroker, message);
		verify(channelToClient2).send(same(message));
		assertEquals(654, message.getMessageId());
	}

	@Test
	public void testPublish_FromClientConnection_Qos0() throws Exception {

		connectClientAndBroker();

		PubMessage message = new PubMessage(QoS.AT_MOST_ONCE, false, "foo", 0, new byte[1]);
		session.publish(channelToClient1, message);
		verify(channelToBroker).send(same(message));
		assertEquals(0, message.getMessageId());
	}

	@Test
	public void testPublish_FromClientConnection_Qos1() throws Exception {

		connectClientAndBroker();

		PubMessage message = new PubMessage(QoS.AT_LEAST_ONCE, false, "foo", 123, new byte[1]);
		session.publish(channelToClient1, message);
		verify(channelToBroker).send(same(message));
		assertEquals(1, message.getMessageId());

		message = new PubMessage(QoS.AT_LEAST_ONCE, false, "foo", 124, new byte[1]);
		session.publish(channelToClient1, message);
		verify(channelToBroker).send(same(message));
		assertEquals(2, message.getMessageId());

		Field field = ProxySession.class.getDeclaredField("nextIdToBroker");
		field.setAccessible(true);
		field.set(session, 0xffff);

		message = new PubMessage(QoS.AT_LEAST_ONCE, false, "foo", 125, new byte[1]);
		session.publish(channelToClient1, message);
		verify(channelToBroker).send(same(message));
		assertEquals(0xffff, message.getMessageId());

		message = new PubMessage(QoS.AT_LEAST_ONCE, false, "foo", 126, new byte[1]);
		session.publish(channelToClient1, message);
		verify(channelToBroker).send(same(message));
		assertEquals(3, message.getMessageId());
	}

	@Test
	public void testPubAck_FromBrokerConnection_SendingClientFound() throws Exception {

		connectClientAndBroker();

		PubMessage message = new PubMessage(QoS.AT_LEAST_ONCE, false, "foo", 123, new byte[1]);
		session.publish(channelToClient1, message);
		verify(channelToBroker).send(same(message));
		assertEquals(1, message.getMessageId());

		PubAckMessage ack = new PubAckMessage(1);
		session.pubAck(channelToBroker, ack);
		verify(channelToClient1).send(same(ack));
		assertEquals(123, ack.getMessageId());
	}

	@Test
	public void testPubAck_FromBrokerConnection_SendingClientNotFound() throws Exception {

		connectClientAndBroker();

		PubMessage message = new PubMessage(QoS.AT_LEAST_ONCE, false, "foo", 123, new byte[1]);
		session.publish(channelToClient1, message);
		verify(channelToBroker).send(same(message));
		assertEquals(1, message.getMessageId());

		session.channelClosed(channelToClient1, null);
		reset(channelToClient1);

		PubAckMessage ack = new PubAckMessage(1);
		session.pubAck(channelToBroker, ack);
		verifyZeroInteractions(channelToClient1);
	}

	@Test
	public void testPubAck_FromClientConnection() throws Exception {

		connectClientAndBroker();

		PubMessage message = new PubMessage(QoS.AT_LEAST_ONCE, false, "foo", 123, new byte[1]);
		session.publish(channelToBroker, message);
		verify(channelToClient1).send(same(message));
		assertEquals(123, message.getMessageId());

		PubAckMessage ack = new PubAckMessage(123);
		session.pubAck(channelToClient1, ack);
		verify(channelToBroker).send(same(ack));
		assertEquals(123, ack.getMessageId());
	}

	@Test
	public void testSubscribe() throws Exception {

		connectClientAndBroker();

		SubscribeMessage message = new SubscribeMessage(123, new String[0], new QoS[0]);
		session.subscribe(channelToClient1, message);
		verify(channelToBroker).send(same(message));
		assertEquals(1, message.getMessageId());
	}

	@Test
	public void testSubAck_SendingClientFound() throws Exception {

		connectClientAndBroker();

		SubscribeMessage message = new SubscribeMessage(123, new String[0], new QoS[0]);
		session.subscribe(channelToClient1, message);
		verify(channelToBroker).send(same(message));
		assertEquals(1, message.getMessageId());

		SubAckMessage ack = new SubAckMessage(1, new QoS[0]);
		session.subAck(channelToBroker, ack);
		verify(channelToClient1).send(same(ack));
		assertEquals(123, ack.getMessageId());
	}

	@Test
	public void testSubAck_SendingClientNotFound() throws Exception {

		connectClientAndBroker();

		SubscribeMessage message = new SubscribeMessage(123, new String[0], new QoS[0]);
		session.subscribe(channelToClient1, message);
		verify(channelToBroker).send(same(message));
		assertEquals(1, message.getMessageId());

		session.channelClosed(channelToClient1, null);
		reset(channelToClient1);

		SubAckMessage ack = new SubAckMessage(1, new QoS[0]);
		session.subAck(channelToBroker, ack);
		verifyZeroInteractions(channelToClient1);
	}

	@Test
	public void testUnsubscribe() throws Exception {

		connectClientAndBroker();

		UnsubscribeMessage message = new UnsubscribeMessage(123, new String[0]);
		session.unsubscribe(channelToClient1, message);
		verify(channelToBroker).send(same(message));
		assertEquals(1, message.getMessageId());
	}

	@Test
	public void testUnsubAck_SendingClientFound() throws Exception {

		connectClientAndBroker();

		UnsubscribeMessage message = new UnsubscribeMessage(123, new String[0]);
		session.unsubscribe(channelToClient1, message);
		verify(channelToBroker).send(same(message));
		assertEquals(1, message.getMessageId());

		UnsubAckMessage ack = new UnsubAckMessage(1);
		session.unsubAck(channelToBroker, ack);
		verify(channelToClient1).send(same(ack));
		assertEquals(123, ack.getMessageId());
	}

	@Test
	public void testUnsubAck_SendingClientNotFound() throws Exception {

		connectClientAndBroker();

		UnsubscribeMessage message = new UnsubscribeMessage(123, new String[0]);
		session.unsubscribe(channelToClient1, message);
		verify(channelToBroker).send(same(message));
		assertEquals(1, message.getMessageId());

		session.channelClosed(channelToClient1, null);
		reset(channelToClient1);

		UnsubAckMessage ack = new UnsubAckMessage(1);
		session.unsubAck(channelToBroker, ack);
		verifyZeroInteractions(channelToClient1);
	}

	@Test
	public void testUnackedMessageIdNotReused() throws Exception {

		connectClientAndBroker();

		UnsubscribeMessage message = new UnsubscribeMessage(123, new String[0]);
		session.unsubscribe(channelToClient1, message);
		verify(channelToBroker).send(same(message));
		assertEquals(1, message.getMessageId());

		Field field = ProxySession.class.getDeclaredField("nextIdToBroker");
		field.setAccessible(true);
		field.set(session, 1);

		message = new UnsubscribeMessage(123, new String[0]);
		session.unsubscribe(channelToClient1, message);
		verify(channelToBroker).send(same(message));
		assertEquals(2, message.getMessageId());
	}

	@Test
	public void testClientPauseAndResumeRead() throws Exception {

		connectClientAndBroker();
		assertTrue(session.newConnection(channelToClient2, connectMessage));
		session.channelAttached(channelToClient2);
		// add a channel pending session attachment
		MqttChannel channelToClient3 = mock(MqttChannel.class);
		assertTrue(session.newConnection(channelToClient3, connectMessage));

		UnsubscribeMessage message = new UnsubscribeMessage(123, new String[0]);
		for (int i = 1; i <= 0xffff; i++) {
			verify(channelToClient1, never()).pauseRead();
			verify(channelToClient2, never()).pauseRead();
			verify(channelToClient3, never()).pauseRead();
			session.unsubscribe(channelToClient1, message);
		}

		verify(channelToClient1).pauseRead();
		verify(channelToClient2).pauseRead();
		verify(channelToClient3, never()).pauseRead();

		session.channelAttached(channelToClient3);
		verify(channelToClient3).pauseRead();

		UnsubAckMessage ack = new UnsubAckMessage(1234);
		session.unsubAck(channelToBroker, ack);

		verify(channelToClient1).resumeRead();
		verify(channelToClient2).resumeRead();
		verify(channelToClient3).resumeRead();

		session.unsubscribe(channelToClient1, message);
		assertEquals(1234, message.getMessageId());

		verify(channelToClient1, times(2)).pauseRead();
		verify(channelToClient2, times(2)).pauseRead();
		verify(channelToClient3, times(2)).pauseRead();
	}

	@Test
	public void testChannelOpened() throws Exception {

		session.channelOpened(channelToBroker);

		verify(channelToBroker).send(connectMessage);
	}

	@Test
	public void testChannelClosed_ChannelToBroker() throws Exception {

		connectClientAndBroker();
		assertTrue(session.newConnection(channelToClient2, connectMessage));
		session.channelAttached(channelToClient2);

		session.channelClosed(channelToBroker, null);

		verify(channelToClient1).close();
		verify(channelToClient2).close();
		assertTrue(session.isClosed());
	}

	@Test
	public void testChannelClosed_ChannelToClient_MaxInFlightMessagesReached() throws Exception {

		session = new ProxySession(brokerUri, connectMessage, manager, 1);

		connectClientAndBroker();
		assertTrue(session.newConnection(channelToClient2, connectMessage));
		session.channelAttached(channelToClient2);

		PubMessage message = new PubMessage(QoS.AT_LEAST_ONCE, false, "foo", 789, new byte[1]);
		session.publish(channelToClient1, message);

		verify(channelToClient1).pauseRead();
		verify(channelToClient2).pauseRead();

		session.channelClosed(channelToClient1, null);

		verify(channelToClient1).resumeRead();
		verify(channelToClient2).resumeRead();
	}

	@Test
	public void testChannelClosed_ChannelToClient_NotLastClient() throws Exception {

		connectClientAndBroker();
		assertTrue(session.newConnection(channelToClient2, connectMessage));
		session.channelAttached(channelToClient2);

		PubMessage message1 = new PubMessage(QoS.AT_LEAST_ONCE, false, "foo", 123, new byte[1]);
		session.publish(channelToBroker, message1);
		verify(channelToClient1).send(same(message1));
		assertEquals(123, message1.getMessageId());

		PubMessage message2 = new PubMessage(QoS.AT_LEAST_ONCE, false, "foo", 456, new byte[1]);
		session.publish(channelToBroker, message2);
		verify(channelToClient2).send(same(message2));
		assertEquals(456, message2.getMessageId());

		PubMessage message3 = new PubMessage(QoS.AT_LEAST_ONCE, false, "foo", 789, new byte[1]);
		session.publish(channelToBroker, message3);
		verify(channelToClient1).send(same(message3));
		assertEquals(789, message3.getMessageId());

		List<MqttMessage> unsentMessages = new ArrayList<MqttMessage>();
		unsentMessages.add(message1);
		unsentMessages.add(message3);
		when(channelToClient1.getUnsentMessages()).thenReturn(unsentMessages);
		reset(channelToBroker);
		session.channelClosed(channelToClient1, null);

		verify(channelToClient2).send(same(message1));
		assertEquals(123, message1.getMessageId());
		verify(channelToClient2).send(same(message3));
		assertEquals(789, message3.getMessageId());

		assertFalse(session.isClosed());

		verifyZeroInteractions(channelToBroker);
	}

	@Test
	public void testChannelClosed_ChannelToClient_LastClient() throws Exception {

		connectClientAndBroker();

		reset(channelToBroker);
		session.channelClosed(channelToClient1, null);

		verify(channelToBroker).send(messageCaptor.capture());
		assertTrue(messageCaptor.getValue() instanceof DisconnectMessage);

		verifyZeroInteractions(channelToBroker);
	}

	@Test
	public void testChannelAttached_NoPendingConnectForChannel() throws Exception {

		session.channelAttached(channelToClient1);

		verify(channelToClient1).send(messageCaptor.capture());
		assertEquals(ConnectReturnCode.OTHER, ((ConnAckMessage) messageCaptor.getValue()).getReturnCode());
	}

	@Test
	public void testChannelAttached_CleanSessionIsTrue() throws Exception {

		connectMessage = new ConnectMessage("foo", true, 10000);
		assertTrue(session.newConnection(channelToClient1, connectMessage));
		session.channelAttached(channelToClient1);

		verify(channelToClient1).send(messageCaptor.capture());
		assertEquals(ConnectReturnCode.OTHER, ((ConnAckMessage) messageCaptor.getValue()).getReturnCode());
	}

	@Test
	public void testChannelAttached_ProtocolVersionDoesNotMatch() throws Exception {

		byte[] messageBytes = new byte[] { 0x10, 0x15, 0x00, 0x06, 0x4d, 0x51, 0x49, 0x73, 0x64, 0x70, 0x03, 0x00, 0x27, 0x10, 0x00, 0x07, 0x63, 0x6c, 0x69,
				0x65, 0x6e, 0x74, 0x31 };

		messageBytes[10] = 4;
		connectMessage = new ConnectMessage(ByteBuffer.wrap(messageBytes), 0x15, 0);
		assertTrue(session.newConnection(channelToClient1, connectMessage));
		session.channelAttached(channelToClient1);

		verify(channelToClient1).send(messageCaptor.capture());
		assertEquals(ConnectReturnCode.UNACCEPTABLE_PROTOCOL_VERSION, ((ConnAckMessage) messageCaptor.getValue()).getReturnCode());
	}

	@Test
	public void testChannelAttached_ProtocolNameDoesNotMatch() throws Exception {

		byte[] messageBytes = new byte[] { 0x10, 0x15, 0x00, 0x06, 0x4d, 0x51, 0x49, 0x73, 0x64, 0x70, 0x03, 0x00, 0x27, 0x10, 0x00, 0x07, 0x63, 0x6c, 0x69,
				0x65, 0x6e, 0x74, 0x31 };

		messageBytes[9] = 0x71;
		connectMessage = new ConnectMessage(ByteBuffer.wrap(messageBytes), 0x15, 0);
		assertTrue(session.newConnection(channelToClient1, connectMessage));
		session.channelAttached(channelToClient1);

		verify(channelToClient1).send(messageCaptor.capture());
		assertEquals(ConnectReturnCode.OTHER, ((ConnAckMessage) messageCaptor.getValue()).getReturnCode());
	}

	@Test
	public void testChannelAttached_UserNameFlagDoesNotMatch() throws Exception {

		connectMessage = new ConnectMessage("foo", false, 10000, "user", null);
		assertTrue(session.newConnection(channelToClient1, connectMessage));
		session.channelAttached(channelToClient1);

		verify(channelToClient1).send(messageCaptor.capture());
		assertEquals(ConnectReturnCode.BAD_CREDENTIALS, ((ConnAckMessage) messageCaptor.getValue()).getReturnCode());
	}

	@Test
	public void testChannelAttached_PasswordFlagDoesNotMatch() throws Exception {

		connectMessage = new ConnectMessage("foo", false, 10000, "user", null);
		session = new ProxySession(brokerUri, connectMessage, manager, 0xffff);
		when(manager.newClientChannel(brokerUri, session)).thenReturn(channelToBroker);

		connectMessage = new ConnectMessage("foo", false, 10000, "user", "pass");
		assertTrue(session.newConnection(channelToClient1, connectMessage));
		session.channelAttached(channelToClient1);

		verify(channelToClient1).send(messageCaptor.capture());
		assertEquals(ConnectReturnCode.BAD_CREDENTIALS, ((ConnAckMessage) messageCaptor.getValue()).getReturnCode());
	}

	@Test
	public void testChannelAttached_UserNameDoesNotMatch() throws Exception {

		connectMessage = new ConnectMessage("foo", false, 10000, "user", "pass");
		session = new ProxySession(brokerUri, connectMessage, manager, 0xffff);
		when(manager.newClientChannel(brokerUri, session)).thenReturn(channelToBroker);

		connectMessage = new ConnectMessage("foo", false, 10000, "otheruser", "pass");
		assertTrue(session.newConnection(channelToClient1, connectMessage));
		session.channelAttached(channelToClient1);

		verify(channelToClient1).send(messageCaptor.capture());
		assertEquals(ConnectReturnCode.BAD_CREDENTIALS, ((ConnAckMessage) messageCaptor.getValue()).getReturnCode());
	}

	@Test
	public void testChannelAttached_PasswordDoesNotMatch() throws Exception {

		connectMessage = new ConnectMessage("foo", false, 10000, "user", "otherpass");
		session = new ProxySession(brokerUri, connectMessage, manager, 0xffff);
		when(manager.newClientChannel(brokerUri, session)).thenReturn(channelToBroker);

		connectMessage = new ConnectMessage("foo", false, 10000, "user", "pass");
		assertTrue(session.newConnection(channelToClient1, connectMessage));
		session.channelAttached(channelToClient1);

		verify(channelToClient1).send(messageCaptor.capture());
		assertEquals(ConnectReturnCode.BAD_CREDENTIALS, ((ConnAckMessage) messageCaptor.getValue()).getReturnCode());
	}

	@Test
	public void testChannelAttached_WillMessageFlagDoesNotMatch() throws Exception {

		connectMessage = new ConnectMessage("foo", false, 10000, "topic", "msg", QoS.AT_LEAST_ONCE, false);
		assertTrue(session.newConnection(channelToClient1, connectMessage));
		session.channelAttached(channelToClient1);

		verify(channelToClient1).send(messageCaptor.capture());
		assertEquals(ConnectReturnCode.OTHER, ((ConnAckMessage) messageCaptor.getValue()).getReturnCode());
	}

	@Test
	public void testChannelAttached_WillRetainFlagDoesNotMatch() throws Exception {

		connectMessage = new ConnectMessage("foo", false, 10000, "topic", "msg", QoS.AT_LEAST_ONCE, false);
		session = new ProxySession(brokerUri, connectMessage, manager, 0xffff);
		when(manager.newClientChannel(brokerUri, session)).thenReturn(channelToBroker);

		connectMessage = new ConnectMessage("foo", false, 10000, "topic", "msg", QoS.AT_LEAST_ONCE, true);
		assertTrue(session.newConnection(channelToClient1, connectMessage));
		session.channelAttached(channelToClient1);

		verify(channelToClient1).send(messageCaptor.capture());
		assertEquals(ConnectReturnCode.OTHER, ((ConnAckMessage) messageCaptor.getValue()).getReturnCode());
	}

	@Test
	public void testChannelAttached_WillTopicDoesNotMatch() throws Exception {

		connectMessage = new ConnectMessage("foo", false, 10000, "topic", "msg", QoS.AT_LEAST_ONCE, false);
		session = new ProxySession(brokerUri, connectMessage, manager, 0xffff);
		when(manager.newClientChannel(brokerUri, session)).thenReturn(channelToBroker);

		connectMessage = new ConnectMessage("foo", false, 10000, "othertopic", "msg", QoS.AT_LEAST_ONCE, false);
		assertTrue(session.newConnection(channelToClient1, connectMessage));
		session.channelAttached(channelToClient1);

		verify(channelToClient1).send(messageCaptor.capture());
		assertEquals(ConnectReturnCode.OTHER, ((ConnAckMessage) messageCaptor.getValue()).getReturnCode());
	}

	@Test
	public void testChannelAttached_WillMessageDoesNotMatch() throws Exception {

		connectMessage = new ConnectMessage("foo", false, 10000, "topic", "msg", QoS.AT_LEAST_ONCE, false);
		session = new ProxySession(brokerUri, connectMessage, manager, 0xffff);
		when(manager.newClientChannel(brokerUri, session)).thenReturn(channelToBroker);

		connectMessage = new ConnectMessage("foo", false, 10000, "topic", "othermsg", QoS.AT_LEAST_ONCE, false);
		assertTrue(session.newConnection(channelToClient1, connectMessage));
		session.channelAttached(channelToClient1);

		verify(channelToClient1).send(messageCaptor.capture());
		assertEquals(ConnectReturnCode.OTHER, ((ConnAckMessage) messageCaptor.getValue()).getReturnCode());
	}

	@Test
	public void testChannelAttached_WillQosDoesNotMatch() throws Exception {

		connectMessage = new ConnectMessage("foo", false, 10000, "topic", "msg", QoS.AT_LEAST_ONCE, false);
		session = new ProxySession(brokerUri, connectMessage, manager, 0xffff);
		when(manager.newClientChannel(brokerUri, session)).thenReturn(channelToBroker);

		connectMessage = new ConnectMessage("foo", false, 10000, "topic", "msg", QoS.AT_MOST_ONCE, false);
		assertTrue(session.newConnection(channelToClient1, connectMessage));
		session.channelAttached(channelToClient1);

		verify(channelToClient1).send(messageCaptor.capture());
		assertEquals(ConnectReturnCode.OTHER, ((ConnAckMessage) messageCaptor.getValue()).getReturnCode());
	}

	@Test
	public void testChannelAttached_ConnectionToBrokerPending() throws Exception {

		assertTrue(session.newConnection(channelToClient1, connectMessage));
		session.channelAttached(channelToClient1);

		verifyZeroInteractions(channelToClient1);
	}

	@Test
	public void testChannelAttached_ConnectionToBrokerPending_MaxInFlightMessagesReached() throws Exception {

		session = new ProxySession(brokerUri, connectMessage, manager, 0);

		assertTrue(session.newConnection(channelToClient1, connectMessage));
		session.channelAttached(channelToClient1);

		verifyZeroInteractions(channelToClient1);
	}

	@Test
	public void testChannelAttached_ConnectionToBrokerEstablished() throws Exception {

		session.channelOpened(channelToBroker);
		ConnAckMessage message = new ConnAckMessage(ConnectReturnCode.ACCEPTED);
		session.connAck(channelToBroker, message);

		assertTrue(session.newConnection(channelToClient1, connectMessage));
		session.channelAttached(channelToClient1);

		verify(channelToClient1).send(messageCaptor.capture());
		assertEquals(message, messageCaptor.getValue());
	}

	@Test
	public void testChannelAttached_ConnectionToBrokerEstablished_MaxInFlightMessagesReached() throws Exception {

		session = new ProxySession(brokerUri, connectMessage, manager, 0);

		session.channelOpened(channelToBroker);
		ConnAckMessage message = new ConnAckMessage(ConnectReturnCode.ACCEPTED);
		session.connAck(channelToBroker, message);

		assertTrue(session.newConnection(channelToClient1, connectMessage));
		session.channelAttached(channelToClient1);

		verify(channelToClient1).send(messageCaptor.capture());
		assertEquals(message, messageCaptor.getValue());

		verify(channelToClient1).pauseRead();
	}

	@Test
	public void testChannelAttached_ConnectionToBrokerDisconnected() throws Exception {

		session.channelOpened(channelToBroker);
		ConnAckMessage message = new ConnAckMessage(ConnectReturnCode.ACCEPTED);
		session.connAck(channelToBroker, message);

		assertTrue(session.newConnection(channelToClient1, connectMessage));

		session.channelClosed(channelToBroker, null);

		session.channelAttached(channelToClient1);

		verify(channelToClient1).send(messageCaptor.capture());
		assertEquals(ConnectReturnCode.SERVER_UNAVAILABLE, ((ConnAckMessage) messageCaptor.getValue()).getReturnCode());
	}

	@Test
	public void testChannelAttached_ConnectionToBrokerConnectionNotAccepted() throws Exception {

		session.channelOpened(channelToBroker);
		ConnAckMessage message = new ConnAckMessage(ConnectReturnCode.IDENTIFIER_REJECTED);
		session.connAck(channelToBroker, message);

		assertTrue(session.newConnection(channelToClient1, connectMessage));

		session.channelClosed(channelToBroker, null);

		session.channelAttached(channelToClient1);

		verify(channelToClient1).send(messageCaptor.capture());
		assertEquals(message, messageCaptor.getValue());
	}

	private void connectClientAndBroker() throws Exception {

		attachClientAndBroker();
		ConnAckMessage message = new ConnAckMessage(ConnectReturnCode.ACCEPTED);
		session.connAck(channelToBroker, message);
	}

	private void attachClientAndBroker() {

		assertTrue(session.newConnection(channelToClient1, connectMessage));
		session.channelAttached(channelToClient1);
		session.channelOpened(channelToBroker);
	}
}
