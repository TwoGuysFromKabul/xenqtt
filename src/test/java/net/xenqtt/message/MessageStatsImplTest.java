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
package net.xenqtt.message;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.HashSet;
import java.util.Set;

import net.xenqtt.message.MessageStatsImpl;
import net.xenqtt.message.MqttChannel;

import org.junit.Before;
import org.junit.Test;

public class MessageStatsImplTest {

	Set<MqttChannel> openChannels;
	MessageStatsImpl stats;

	@Before
	public void setup() {
		openChannels = new HashSet<MqttChannel>();
		for (int i = 1; i <= 100; i++) {
			MqttChannel channel = mock(MqttChannel.class);
			when(channel.sendQueueDepth()).thenReturn(i);
			when(channel.inFlightMessageCount()).thenReturn(1);
			openChannels.add(channel);
		}
		stats = new MessageStatsImpl(openChannels);
	}

	@Test
	public void testGetMessagesQueuedToSend() {
		assertEquals(0, stats.getMessagesQueuedToSend());
		assertEquals(5050, stats.clone().getMessagesQueuedToSend());
	}

	@Test
	public void testGetMessagesInFlight() {
		assertEquals(0, stats.getMessagesInFlight());
		assertEquals(100, stats.clone().getMessagesInFlight());
	}

	@Test
	public void testGetMessagesSent() {
		stats.messageSent(false);
		stats.messageSent(false);
		stats.messageSent(true);
		stats.messageSent(true);
		stats.messageSent(false);
		stats.messageSent(false);
		stats.messageSent(false);

		assertEquals(7, stats.getMessagesSent());
		assertEquals(7, stats.clone().getMessagesSent());
	}

	@Test
	public void testGetMessagesResent() {
		stats.messageSent(false);
		stats.messageSent(false);
		stats.messageSent(true);
		stats.messageSent(true);
		stats.messageSent(false);
		stats.messageSent(false);
		stats.messageSent(false);

		assertEquals(2, stats.getMessagesResent());
		assertEquals(2, stats.clone().getMessagesResent());
	}

	@Test
	public void testGetMessagesReceived() {
		stats.messageReceived(true);
		stats.messageReceived(false);
		stats.messageReceived(true);
		stats.messageReceived(false);
		stats.messageReceived(false);
		stats.messageReceived(false);
		stats.messageReceived(true);

		assertEquals(7, stats.getMessagesReceived());
		assertEquals(7, stats.clone().getMessagesReceived());
	}

	@Test
	public void testGetDuplicateMessagesReceived() {
		stats.messageReceived(true);
		stats.messageReceived(false);
		stats.messageReceived(true);
		stats.messageReceived(false);
		stats.messageReceived(false);
		stats.messageReceived(false);
		stats.messageReceived(true);

		assertEquals(3, stats.getDuplicateMessagesReceived());
		assertEquals(3, stats.clone().getDuplicateMessagesReceived());
	}

	@Test
	public void testGetMinAckLatencyMillis() {
		stats.messageAcked(10);
		stats.messageAcked(100);
		stats.messageAcked(1000);
		stats.messageAcked(10000);
		stats.messageAcked(100000);
		stats.messageAcked(1000000);
		stats.messageAcked(10000000);

		assertEquals(10, stats.getMinAckLatencyMillis());
		assertEquals(10, stats.clone().getMinAckLatencyMillis());
	}

	@Test
	public void testGetMaxAckLatencyMillis() {
		stats.messageAcked(10);
		stats.messageAcked(100);
		stats.messageAcked(1000);
		stats.messageAcked(10000);
		stats.messageAcked(100000);
		stats.messageAcked(1000000);
		stats.messageAcked(10000000);

		assertEquals(10000000, stats.getMaxAckLatencyMillis());
		assertEquals(10000000, stats.clone().getMaxAckLatencyMillis());
	}

	@Test
	public void testGetAverageAckLatencyMillis() {
		stats.messageAcked(10);
		stats.messageAcked(100);
		stats.messageAcked(1000);
		stats.messageAcked(10000);
		stats.messageAcked(100000);
		stats.messageAcked(1000000);
		stats.messageAcked(10000000);

		double expected = (10 + 100 + 1000 + 10000 + 100000 + 1000000 + 10000000) / 7.0;
		assertEquals(expected, stats.getAverageAckLatencyMillis(), 0.0);
		assertEquals(expected, stats.clone().getAverageAckLatencyMillis(), 0.0);
	}

	@Test
	public void testReset() {
		stats.messageAcked(10);
		stats.messageAcked(100);
		stats.messageAcked(1000);
		stats.messageReceived(false);
		stats.messageReceived(true);
		stats.messageReceived(false);
		stats.messageSent(true);
		stats.messageSent(true);
		stats.messageSent(false);

		assertEquals(5050, stats.clone().getMessagesQueuedToSend());
		assertEquals(100, stats.clone().getMessagesInFlight());
		assertEquals(3, stats.getMessagesSent());
		assertEquals(2, stats.getMessagesResent());
		assertEquals(3, stats.getMessagesReceived());
		assertEquals(1, stats.getDuplicateMessagesReceived());
		assertEquals(10, stats.getMinAckLatencyMillis());
		assertEquals(1000, stats.getMaxAckLatencyMillis());
		assertEquals(370.0, stats.getAverageAckLatencyMillis(), 0.0);

		stats.reset();
		assertEquals(5050, stats.clone().getMessagesQueuedToSend());
		assertEquals(100, stats.clone().getMessagesInFlight());
		assertEquals(0, stats.getMessagesSent());
		assertEquals(0, stats.getMessagesResent());
		assertEquals(0, stats.getMessagesReceived());
		assertEquals(0, stats.getDuplicateMessagesReceived());
		assertEquals(0, stats.getMinAckLatencyMillis());
		assertEquals(0, stats.getMaxAckLatencyMillis());
		assertEquals(0.0, stats.getAverageAckLatencyMillis(), 0.0);
	}

	@Test
	public void testClone() {
		stats.messageAcked(10);
		stats.messageAcked(100);
		stats.messageAcked(1000);
		stats.messageReceived(false);
		stats.messageReceived(true);
		stats.messageReceived(false);
		stats.messageSent(true);
		stats.messageSent(true);
		stats.messageSent(false);

		assertEquals(0, stats.getMessagesQueuedToSend());
		assertEquals(5050, stats.clone().getMessagesQueuedToSend());
		assertEquals(0, stats.getMessagesInFlight());
		assertEquals(100, stats.clone().getMessagesInFlight());
		assertEquals(3, stats.getMessagesSent());
		assertEquals(2, stats.getMessagesResent());
		assertEquals(3, stats.getMessagesReceived());
		assertEquals(1, stats.getDuplicateMessagesReceived());
		assertEquals(10, stats.getMinAckLatencyMillis());
		assertEquals(1000, stats.getMaxAckLatencyMillis());
		assertEquals(370.0, stats.getAverageAckLatencyMillis(), 0.0);

		MessageStatsImpl clone = stats.clone();
		stats.reset();
		assertEquals(5050, clone.getMessagesQueuedToSend());
		assertEquals(100, clone.getMessagesInFlight());
		assertEquals(3, clone.getMessagesSent());
		assertEquals(2, clone.getMessagesResent());
		assertEquals(3, clone.getMessagesReceived());
		assertEquals(1, clone.getDuplicateMessagesReceived());
		assertEquals(10, clone.getMinAckLatencyMillis());
		assertEquals(1000, clone.getMaxAckLatencyMillis());
		assertEquals(370.0, clone.getAverageAckLatencyMillis(), 0.0);
	}

}
