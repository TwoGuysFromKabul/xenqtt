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

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import net.sf.xenqtt.message.BlockingCommand;
import net.sf.xenqtt.message.MqttChannel;
import net.sf.xenqtt.message.PubAckMessage;
import net.sf.xenqtt.message.PubMessage;
import net.sf.xenqtt.message.QoS;

import org.junit.Before;
import org.junit.Test;

public class ClientTest {

	MqttChannel channel = mock(MqttChannel.class);
	BrokerEventsImpl events = new BrokerEventsImpl();
	Client client = new Client(channel, events, 2);

	@Before
	public void setup() {
		client.clientId = "clientId";
		client.cleanSession = false;
	}

	@Test
	public void testGetClientId() {
		assertEquals("clientId", client.getClientId());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSend_NonPublishMessage() {

		PubAckMessage message = new PubAckMessage(1);
		assertTrue(client.send(message));
		assertEquals(1, message.getMessageId());
		verify(channel).send(same(message), isNull(BlockingCommand.class), anyLong());

		message = new PubAckMessage(2);
		assertTrue(client.send(message));
		assertEquals(2, message.getMessageId());
		verify(channel).send(same(message), isNull(BlockingCommand.class), anyLong());

		message = new PubAckMessage(3);
		assertTrue(client.send(message));
		assertEquals(3, message.getMessageId());
		verify(channel).send(same(message), isNull(BlockingCommand.class), anyLong());

		List<BrokerEvent> events = this.events.getEvents("clientId");
		assertEquals(3, events.size());
		assertSame(BrokerEventType.MESSAGE_SENT, events.get(0).getEventType());
		assertSame(BrokerEventType.MESSAGE_SENT, events.get(1).getEventType());
		assertSame(BrokerEventType.MESSAGE_SENT, events.get(2).getEventType());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSend_PublishMessage_Qos0() {

		PubMessage message = new PubMessage(QoS.AT_MOST_ONCE, false, "foo", 0, new byte[] { 1, 2, 3 });
		assertTrue(client.send(message));
		assertEquals(0, message.getMessageId());
		verify(channel).send(same(message), isNull(BlockingCommand.class), anyLong());

		message = new PubMessage(QoS.AT_MOST_ONCE, false, "foo", 0, new byte[] { 1, 2, 3 });
		assertTrue(client.send(message));
		assertEquals(0, message.getMessageId());
		verify(channel).send(same(message), isNull(BlockingCommand.class), anyLong());

		message = new PubMessage(QoS.AT_MOST_ONCE, false, "foo", 0, new byte[] { 1, 2, 3 });
		assertTrue(client.send(message));
		assertEquals(0, message.getMessageId());
		verify(channel).send(same(message), isNull(BlockingCommand.class), anyLong());

		List<BrokerEvent> events = this.events.getEvents("clientId");
		assertEquals(3, events.size());
		assertSame(BrokerEventType.MESSAGE_SENT, events.get(0).getEventType());
		assertSame(BrokerEventType.MESSAGE_SENT, events.get(1).getEventType());
		assertSame(BrokerEventType.MESSAGE_SENT, events.get(2).getEventType());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSend_PublishMessage_NotQos0() {

		PubMessage message1 = new PubMessage(QoS.AT_LEAST_ONCE, false, "foo", 0, new byte[] { 1, 2, 3 });
		assertTrue(client.send(message1));
		assertEquals(1, message1.getMessageId());
		verify(channel).send(same(message1), isNull(BlockingCommand.class), anyLong());

		PubMessage message2 = new PubMessage(QoS.AT_LEAST_ONCE, false, "foo", 0, new byte[] { 1, 2, 3 });
		assertTrue(client.send(message2));
		assertEquals(2, message2.getMessageId());
		verify(channel).send(same(message2), isNull(BlockingCommand.class), anyLong());

		PubMessage message3 = new PubMessage(QoS.AT_LEAST_ONCE, false, "foo", 0, new byte[] { 1, 2, 3 });
		assertFalse(client.send(message3));
		assertEquals(0, message3.getMessageId());
		verify(channel, never()).send(same(message3), isNull(BlockingCommand.class), anyLong());

		List<BrokerEvent> events = this.events.getEvents("clientId");
		assertEquals(2, events.size());
		assertSame(BrokerEventType.MESSAGE_SENT, events.get(0).getEventType());
		assertSame(BrokerEventType.MESSAGE_SENT, events.get(1).getEventType());

		this.events.clearEvents();

		client.messageReceived(new PubAckMessage(5));
		verify(channel, never()).send(same(message3), isNull(BlockingCommand.class), anyLong());
		PubMessage message4 = new PubMessage(QoS.AT_LEAST_ONCE, false, "foo", 0, new byte[] { 1, 2, 3 });
		assertFalse(client.send(message4));
		assertEquals(0, message3.getMessageId());
		verify(channel, never()).send(same(message4), isNull(BlockingCommand.class), anyLong());

		client.messageReceived(new PubAckMessage(2));
		assertEquals(3, message3.getMessageId());
		verify(channel).send(same(message3), isNull(BlockingCommand.class), anyLong());
		verify(channel, never()).send(same(message4), isNull(BlockingCommand.class), anyLong());

		client.messageReceived(new PubAckMessage(1));
		assertEquals(4, message4.getMessageId());
		verify(channel).send(same(message4), isNull(BlockingCommand.class), anyLong());

		client.messageReceived(new PubAckMessage(1));

		events = this.events.getEvents("clientId");
		assertEquals(6, events.size());
		assertSame(BrokerEventType.MESSAGE_RECEIVED, events.get(0).getEventType());
		assertSame(BrokerEventType.MESSAGE_RECEIVED, events.get(1).getEventType());
		assertSame(BrokerEventType.MESSAGE_SENT, events.get(2).getEventType());
		assertSame(BrokerEventType.MESSAGE_RECEIVED, events.get(3).getEventType());
		assertSame(BrokerEventType.MESSAGE_SENT, events.get(4).getEventType());
		assertSame(BrokerEventType.MESSAGE_RECEIVED, events.get(5).getEventType());
	}

	@Test
	public void testMessageReceived() throws Exception {

		client.messageReceived(new PubAckMessage(1));

		List<BrokerEvent> events = this.events.getEvents("clientId");
		assertEquals(1, events.size());
		assertSame(BrokerEventType.MESSAGE_RECEIVED, events.get(0).getEventType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSend_NullMessage() {
		client.send(null);
	}

}
