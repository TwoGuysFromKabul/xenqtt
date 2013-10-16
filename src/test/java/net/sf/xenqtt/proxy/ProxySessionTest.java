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
package net.sf.xenqtt.proxy;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;

import net.sf.xenqtt.message.ChannelManager;
import net.sf.xenqtt.message.ConnAckMessage;
import net.sf.xenqtt.message.ConnectMessage;
import net.sf.xenqtt.message.ConnectReturnCode;
import net.sf.xenqtt.message.DisconnectMessage;
import net.sf.xenqtt.message.MqttChannel;
import net.sf.xenqtt.message.MqttMessage;
import net.sf.xenqtt.message.PubAckMessage;
import net.sf.xenqtt.message.PubMessage;
import net.sf.xenqtt.message.QoS;
import net.sf.xenqtt.message.SubAckMessage;
import net.sf.xenqtt.message.SubscribeMessage;
import net.sf.xenqtt.message.UnsubAckMessage;
import net.sf.xenqtt.message.UnsubscribeMessage;

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
		session = new ProxySession(brokerUri, connectMessage, manager);
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
		assertEquals(1, message.getMessageId());
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
	public void testChannelAttached() throws Exception {

		fail("not implemented");
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
