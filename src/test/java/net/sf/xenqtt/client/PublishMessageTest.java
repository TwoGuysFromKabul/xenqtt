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
package net.sf.xenqtt.client;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import net.sf.xenqtt.message.ChannelManager;
import net.sf.xenqtt.message.MqttChannelRef;
import net.sf.xenqtt.message.PubAckMessage;
import net.sf.xenqtt.message.PubMessage;
import net.sf.xenqtt.message.QoS;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PublishMessageTest {

	@Mock ChannelManager channelManager;
	@Mock MqttChannelRef channel;
	PubMessage pubMessage = new PubMessage(QoS.AT_LEAST_ONCE, false, "my topic", 123, new byte[] { 97, 98, 99 });
	PublishMessage message;

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		message = new PublishMessage(channelManager, channel, pubMessage);
	}

	@Test
	public void testCtor_TopicQosPayload() {
		PublishMessage message = new PublishMessage("grand/foo/bar", QoS.AT_LEAST_ONCE, new byte[] { 97, 98, 99 });
		assertEquals("grand/foo/bar", message.getTopic());
		assertSame(QoS.AT_LEAST_ONCE, message.getQoS());
		assertArrayEquals(new byte[] { 97, 98, 99 }, message.getPayload());
		assertEquals("abc", message.getPayloadString());
		assertFalse(message.isDuplicate());
		assertFalse(message.isRetain());
	}

	@Test
	public void testCtor_TopicQosPayloadString() {
		PublishMessage message = new PublishMessage("grand/foo/bar", QoS.AT_LEAST_ONCE, "abc");
		assertEquals("grand/foo/bar", message.getTopic());
		assertSame(QoS.AT_LEAST_ONCE, message.getQoS());
		assertArrayEquals(new byte[] { 97, 98, 99 }, message.getPayload());
		assertEquals("abc", message.getPayloadString());
		assertFalse(message.isDuplicate());
		assertFalse(message.isRetain());
	}

	@Test
	public void testCtor_TopicQosPayloadStringRetain() {
		PublishMessage message = new PublishMessage("grand/foo/bar", QoS.AT_LEAST_ONCE, "abc", true);
		assertEquals("grand/foo/bar", message.getTopic());
		assertSame(QoS.AT_LEAST_ONCE, message.getQoS());
		assertArrayEquals(new byte[] { 97, 98, 99 }, message.getPayload());
		assertEquals("abc", message.getPayloadString());
		assertFalse(message.isDuplicate());
		assertTrue(message.isRetain());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_BlankTopic() {
		new PublishMessage("", QoS.AT_MOST_ONCE, new byte[] { 97, 98, 99 }, true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_NullTopic() {
		new PublishMessage(null, QoS.AT_MOST_ONCE, new byte[] { 97, 98, 99 }, true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_NullQoS() {
		new PublishMessage("grand/foo/bar", null, new byte[] { 97, 98, 99 }, true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_NullPayload() {
		byte[] payload = null;
		new PublishMessage("grand/foo/bar", QoS.AT_MOST_ONCE, payload, true);
	}

	@Test
	public void testGetTopic() throws Exception {
		assertEquals("my topic", message.getTopic());
	}

	@Test
	public void testGetPayload() throws Exception {

		assertArrayEquals(new byte[] { 97, 98, 99 }, message.getPayload());
	}

	@Test
	public void testGetPayloadString() throws Exception {

		assertEquals("abc", message.getPayloadString());
	}

	@Test
	public void testIsRetain() throws Exception {

		assertFalse(message.isRetain());

		pubMessage = new PubMessage(QoS.AT_LEAST_ONCE, true, "my topic", 123, new byte[] { 97, 98, 99 });
		message = new PublishMessage(channelManager, channel, pubMessage);

		assertTrue(message.isRetain());
	}

	@Test
	public void testIsDuplicate() throws Exception {

		assertFalse(message.isDuplicate());

		pubMessage.setDuplicateFlag();

		assertTrue(message.isDuplicate());
	}

	@Test
	public void testGetQos() throws Exception {

		assertEquals(QoS.AT_LEAST_ONCE, message.getQoS());

		pubMessage = new PubMessage(QoS.AT_MOST_ONCE, false, "my topic", 123, new byte[] { 97, 98, 99 });
		message = new PublishMessage(channelManager, channel, pubMessage);

		assertEquals(QoS.AT_MOST_ONCE, message.getQoS());
	}

	@Test
	public void testAck_Qos0() throws Exception {

		pubMessage = new PubMessage(QoS.AT_MOST_ONCE, false, "my topic", 123, new byte[] { 97, 98, 99 });
		message = new PublishMessage(channelManager, channel, pubMessage);

		message.ack();
		verifyZeroInteractions(channelManager);
		assertFalse(Thread.interrupted());
	}

	@Test
	public void testAck_Qos1() throws Exception {

		message.ack();
		verify(channelManager).send(channel, new PubAckMessage(123));
		assertFalse(Thread.interrupted());
	}
}
