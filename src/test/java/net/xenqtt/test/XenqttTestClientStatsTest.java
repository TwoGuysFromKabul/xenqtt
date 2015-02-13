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
package net.xenqtt.test;

import static org.junit.Assert.*;
import net.xenqtt.client.PublishMessage;
import net.xenqtt.message.QoS;
import net.xenqtt.test.XenqttTestClientStats;
import net.xenqtt.test.XenqttTestClient.ClientType;
import net.xenqtt.test.XenqttTestClientStats.Gap;

import org.junit.Test;

public class XenqttTestClientStatsTest {

	XenqttTestClientStats stats = new XenqttTestClientStats(ClientType.ASYNC);

	@Test
	public void testGetClientType() {
		assertEquals("async", stats.getClientType());
	}

	@Test
	public void testGetTestDurationSeconds() throws Exception {
		stats.testStarted();
		Thread.sleep(1500);
		stats.testEnded();

		double duration = stats.getTestDurationSeconds();
		assertEquals(1.5D, duration, 0.2D);
	}

	@Test
	public void testGetPublishMessageGaps_NoGaps() {
		for (int i = 0; i < 10; i++) {
			PublishMessage message = new PublishMessage("a/b/c", QoS.AT_MOST_ONCE, createPayload(i));
			stats.publishComplete(message);
		}

		assertTrue(stats.getPublishMessageGaps().isEmpty());
	}

	@Test
	public void testGetPublishMessageGaps_NoGaps_OutOfOrder() {
		for (int i = 9; i >= 0; i--) {
			PublishMessage message = new PublishMessage("a/b/c", QoS.AT_MOST_ONCE, createPayload(i));
			stats.publishComplete(message);
		}

		assertTrue(stats.getPublishMessageGaps().isEmpty());
	}

	@Test
	public void testGetPublishMessageGaps_Gaps_NoRange() {
		for (int i = 0; i < 10; i++) {
			if (i == 4) {
				continue;
			}

			PublishMessage message = new PublishMessage("a/b/c", QoS.AT_MOST_ONCE, createPayload(i));
			stats.publishComplete(message);
		}

		Gap gap = stats.getPublishMessageGaps().get(0);
		assertEquals(4, gap.start);
		assertEquals(4, gap.end);
	}

	@Test
	public void testGetPublishMessageGaps_Gaps_Range() {
		for (int i = 0; i < 10; i++) {
			if (i >= 2 && i <= 4) {
				continue;
			}

			PublishMessage message = new PublishMessage("a/b/c", QoS.AT_MOST_ONCE, createPayload(i));
			stats.publishComplete(message);
		}

		Gap gap = stats.getPublishMessageGaps().get(0);
		assertEquals(2, gap.start);
		assertEquals(4, gap.end);
	}

	@Test
	public void testGetReceiveMessageGaps_NoGaps() {
		for (int i = 0; i < 10; i++) {
			PublishMessage message = new PublishMessage("a/b/c", QoS.AT_MOST_ONCE, createPayload(i));
			stats.messageReceived(message);
		}

		assertTrue(stats.getReceivedMessageGaps().isEmpty());
	}

	@Test
	public void testGetReceivedMessageGaps_NoGaps_OutOfOrder() {
		for (int i = 9; i >= 0; i--) {
			PublishMessage message = new PublishMessage("a/b/c", QoS.AT_MOST_ONCE, createPayload(i));
			stats.messageReceived(message);
		}

		assertTrue(stats.getReceivedMessageGaps().isEmpty());
	}

	@Test
	public void testGetReceivedMessageGaps_Gaps_NoRange() {
		for (int i = 0; i < 10; i++) {
			if (i == 4) {
				continue;
			}

			PublishMessage message = new PublishMessage("a/b/c", QoS.AT_MOST_ONCE, createPayload(i));
			stats.messageReceived(message);
		}

		Gap gap = stats.getReceivedMessageGaps().get(0);
		assertEquals(4, gap.start);
		assertEquals(4, gap.end);
	}

	@Test
	public void testGetReceivedMessageGaps_Gaps_Range() {
		for (int i = 0; i < 10; i++) {
			if (i >= 2 && i <= 4) {
				continue;
			}

			PublishMessage message = new PublishMessage("a/b/c", QoS.AT_MOST_ONCE, createPayload(i));
			stats.messageReceived(message);
		}

		Gap gap = stats.getReceivedMessageGaps().get(0);
		assertEquals(2, gap.start);
		assertEquals(4, gap.end);
	}

	private byte[] createPayload(int id) {
		byte[] payload = new byte[12];

		long now = System.currentTimeMillis();
		payload[0] = (byte) ((now & 0xff00000000000000L) >> 56);
		payload[1] = (byte) ((now & 0x00ff000000000000L) >> 48);
		payload[2] = (byte) ((now & 0x0000ff0000000000L) >> 40);
		payload[3] = (byte) ((now & 0x000000ff00000000L) >> 32);
		payload[4] = (byte) ((now & 0x00000000ff000000L) >> 24);
		payload[5] = (byte) ((now & 0x0000000000ff0000L) >> 16);
		payload[6] = (byte) ((now & 0x000000000000ff00L) >> 8);
		payload[7] = (byte) (now & 0x00000000000000ffL);

		payload[8] = (byte) ((id & 0xff000000) >> 24);
		payload[9] = (byte) ((id & 0x00ff0000) >> 16);
		payload[10] = (byte) ((id & 0x0000ff00) >> 8);
		payload[11] = (byte) (id & 0x000000ff);

		return payload;
	}

}
