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

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import net.sf.xenqtt.message.BlockingCommand;
import net.sf.xenqtt.message.MessageType;
import net.sf.xenqtt.message.MqttChannel;
import net.sf.xenqtt.message.PubMessage;
import net.sf.xenqtt.message.QoS;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class SubscriptionTest {

	Subscription subscription = new Subscription("clientId");
	Map<String, Client> clientById = new HashMap<String, Client>();

	MqttChannel channel = mock(MqttChannel.class);
	BrokerEvents events = new BrokerEvents();
	Client client = new Client(channel, events);

	@Test
	public void testConnected_NoPendingMessage() {
		subscription.connected(client);
		verifyZeroInteractions(channel);
		assertEquals(0, events.getEvents().size());
	}

	@Test
	public void testConnected_PendingMessage() {
		PubMessage message = new PubMessage(QoS.AT_LEAST_ONCE, false, "grand/foo/bar", 7, new byte[] { 97, 98, 99 });
		subscription.subscribe("grand/foo/bar", QoS.AT_LEAST_ONCE);
		subscription.publish(message, clientById);
		verifyZeroInteractions(channel);

		subscription.connected(client);
		PubMessage expected = new PubMessage(QoS.AT_LEAST_ONCE, false, "grand/foo/bar", 1, new byte[] { 97, 98, 99 });
		verify(channel).send(expected, null);
		assertEquals(1, events.getEvents().size());
	}

	@Test
	public void testPubAcked_NoMessageInQueue() {
		assertFalse(subscription.pubAcked(7));
	}

	@Test
	public void testPubAcked_MessageInQueue() {
		PubMessage message = new PubMessage(QoS.AT_LEAST_ONCE, false, "grand/foo/bar", 7, new byte[] { 97, 98, 99 });
		subscription.subscribe("grand/foo/bar", QoS.AT_LEAST_ONCE);
		subscription.publish(message, clientById);

		assertTrue(subscription.pubAcked(7));
	}

	@Test
	public void testPublish_SubscribedQoSZero_MessageQoSZero_MessageNotAddedToQueue() {
		PubMessage message = new PubMessage(QoS.AT_MOST_ONCE, false, "grand/foo/bar", 7, new byte[] { 97, 98, 99 });
		clientById.put("clientId", client);
		subscription.publish(message, clientById);

		verify(channel).send(message, null);
		assertEquals(0, getMessageQueueSize());
	}

	@Test
	public void testPublish_SubscribedQoSZero_MessageQoSZero_MessageNotAddedToQueue_NoClient() {
		PubMessage message = new PubMessage(QoS.AT_MOST_ONCE, false, "grand/foo/bar", 7, new byte[] { 97, 98, 99 });
		subscription.publish(message, clientById);

		verifyZeroInteractions(channel);
		assertEquals(0, getMessageQueueSize());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testPublish_SubscribedQoSZero_MessageQoSOne_MessageNotAddedToQueue() {
		PubMessage message = new PubMessage(QoS.AT_LEAST_ONCE, false, "grand/foo/bar", 7, new byte[] { 97, 98, 99 });
		clientById.put("clientId", client);
		subscription.publish(message, clientById);

		ArgumentCaptor<PubMessage> messageCaptor = ArgumentCaptor.forClass(PubMessage.class);
		verify(channel).send(messageCaptor.capture(), any(BlockingCommand.class));
		assertEquals(0, getMessageQueueSize());
		assertEquals(0, messageCaptor.getValue().getMessageId());
		assertSame(MessageType.PUBLISH, messageCaptor.getValue().getMessageType());
		assertArrayEquals(new byte[] { 97, 98, 99 }, messageCaptor.getValue().getPayload());
		assertSame(QoS.AT_MOST_ONCE, messageCaptor.getValue().getQoS());
		assertEquals("grand/foo/bar", messageCaptor.getValue().getTopicName());
	}

	@Test
	public void testPublish_SubscribedQoSZero_MessageQoSOne_MessageNotAddedToQueue_NoClient() {
		PubMessage message = new PubMessage(QoS.AT_LEAST_ONCE, false, "grand/foo/bar", 7, new byte[] { 97, 98, 99 });
		subscription.publish(message, clientById);

		verifyZeroInteractions(channel);
	}

	@Test
	public void testPublish_SubscribedQoSOne_MessageQoSOne_MessageAddedToQueue() {
		PubMessage message = new PubMessage(QoS.AT_LEAST_ONCE, false, "grand/foo/bar", 7, new byte[] { 97, 98, 99 });
		clientById.put("clientId", client);
		subscription.subscribe("grand/foo/bar", QoS.AT_LEAST_ONCE);
		subscription.publish(message, clientById);

		PubMessage expected = new PubMessage(QoS.AT_LEAST_ONCE, false, "grand/foo/bar", 1, new byte[] { 97, 98, 99 });
		verify(channel).send(expected, null);
		assertEquals(1, getMessageQueueSize());
	}

	@Test
	public void testPublish_SubscribedQoSOne_MessageQoSOne_MessageAddedToQueue_NoClient() {
		PubMessage message = new PubMessage(QoS.AT_LEAST_ONCE, false, "grand/foo/bar", 7, new byte[] { 97, 98, 99 });
		subscription.subscribe("grand/foo/bar", QoS.AT_LEAST_ONCE);
		subscription.publish(message, clientById);

		verifyZeroInteractions(channel);
		assertEquals(1, getMessageQueueSize());
	}

	@Test
	public void testSubscribe_NewSubscription() {
		assertTrue(subscription.subscribe("grand/foo/bar", QoS.AT_MOST_ONCE));
		assertSubscriptionQoS(QoS.AT_MOST_ONCE);
	}

	@Test
	public void testSubscribe_ExtantSubscription_UpdatedQoS() {
		assertTrue(subscription.subscribe("grand/foo/bar", QoS.AT_MOST_ONCE));
		assertFalse(subscription.subscribe("grand/foo/bar", QoS.AT_LEAST_ONCE));
		assertSubscriptionQoS(QoS.AT_LEAST_ONCE);
	}

	@Test
	public void testSubscribe_ExtantSubscription_SameQoS() {
		assertTrue(subscription.subscribe("grand/foo/bar", QoS.AT_MOST_ONCE));
		assertFalse(subscription.subscribe("grand/foo/bar", QoS.AT_MOST_ONCE));
		assertSubscriptionQoS(QoS.AT_MOST_ONCE);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSubscribe_BlankTopicName() {
		subscription.subscribe("", QoS.AT_MOST_ONCE);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSubscribe_NullTopicName() {
		subscription.subscribe(null, QoS.AT_MOST_ONCE);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSubscribe_NullQoS() {
		subscription.subscribe("grand/foo/bar", null);
	}

	@Test
	public void testUnsubscribe_AtFirstTwoThenOneTopic_SameQoSForBoth() {
		subscription.subscribe("grand/foo/bar", QoS.AT_MOST_ONCE);
		subscription.subscribe("ultimate/warrior", QoS.AT_MOST_ONCE);

		assertEquals(1, subscription.unsubscribe("grand/foo/bar"));
		assertSubscriptionQoS(QoS.AT_MOST_ONCE);
	}

	@Test
	public void testUnsubscribe_AtFirstTwoThenOneTopic_RemainingSubscriptionHasLowerQoS() {
		subscription.subscribe("grand/foo/bar", QoS.AT_MOST_ONCE);
		subscription.subscribe("ultimate/warrior", QoS.AT_LEAST_ONCE);
		assertSubscriptionQoS(QoS.AT_LEAST_ONCE);

		assertEquals(1, subscription.unsubscribe("ultimate/warrior"));
		assertSubscriptionQoS(QoS.AT_MOST_ONCE);
	}

	@Test
	public void testUnsubscribe_AtFirstTwoThenOneTopic_RemainingSubscriptionHasHigherQoS() {
		subscription.subscribe("grand/foo/bar", QoS.AT_MOST_ONCE);
		subscription.subscribe("ultimate/warrior", QoS.AT_LEAST_ONCE);
		assertSubscriptionQoS(QoS.AT_LEAST_ONCE);

		assertEquals(1, subscription.unsubscribe("grand/foo/bar"));
		assertSubscriptionQoS(QoS.AT_LEAST_ONCE);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testUnsubscribe_BlankTopicName() {
		subscription.unsubscribe("");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testUnsubscribe_NullTopicName() {
		subscription.unsubscribe(null);
	}

	private int getMessageQueueSize() {
		try {
			Field field = Subscription.class.getDeclaredField("messageQueue");
			field.setAccessible(true);
			@SuppressWarnings("unchecked")
			Queue<PubMessage> messageQueue = (Queue<PubMessage>) field.get(subscription);

			return messageQueue.size();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private void assertSubscriptionQoS(QoS qos) {
		try {
			Field field = Subscription.class.getDeclaredField("subscribedQos");
			field.setAccessible(true);
			QoS subscribedQos = (QoS) field.get(subscription);

			assertSame(qos, subscribedQos);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

}
