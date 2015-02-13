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

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import net.xenqtt.message.MessageType;
import net.xenqtt.message.PubMessage;
import net.xenqtt.message.QoS;

import org.junit.Test;

public class PubMessageTest {

	final byte[] paylaod = "To alcohol - the cause of, and solution to, all of life's problems".getBytes(Charset.forName("UTF-8"));
	final byte[] emptyPayloadBytes = new byte[] { 50, 24, 0, 20, 110, 101, 116, 46, 115, 102, 47, 109, 101, 115, 115, 97, 103, 101, 47, 116, 111, 112, 105, 99,
			0, 1 };
	final byte[] qos1Bytes = new byte[] { 59, 90, 0, 20, 110, 101, 116, 46, 115, 102, 47, 109, 101, 115, 115, 97, 103, 101, 47, 116, 111, 112, 105, 99, 0, 1,
			84, 111, 32, 97, 108, 99, 111, 104, 111, 108, 32, 45, 32, 116, 104, 101, 32, 99, 97, 117, 115, 101, 32, 111, 102, 44, 32, 97, 110, 100, 32, 115,
			111, 108, 117, 116, 105, 111, 110, 32, 116, 111, 44, 32, 97, 108, 108, 32, 111, 102, 32, 108, 105, 102, 101, 39, 115, 32, 112, 114, 111, 98, 108,
			101, 109, 115 };
	final byte[] qos0Bytes = new byte[] { 57, 88, 0, 20, 110, 101, 116, 46, 115, 102, 47, 109, 101, 115, 115, 97, 103, 101, 47, 116, 111, 112, 105, 99, 84,
			111, 32, 97, 108, 99, 111, 104, 111, 108, 32, 45, 32, 116, 104, 101, 32, 99, 97, 117, 115, 101, 32, 111, 102, 44, 32, 97, 110, 100, 32, 115, 111,
			108, 117, 116, 105, 111, 110, 32, 116, 111, 44, 32, 97, 108, 108, 32, 111, 102, 32, 108, 105, 102, 101, 39, 115, 32, 112, 114, 111, 98, 108, 101,
			109, 115 };

	@Test
	public void testOutboundCtor_EmptyPayload() {
		PubMessage message = new PubMessage(QoS.AT_LEAST_ONCE, false, "net.sf/message/topic", 1, new byte[] {});

		assertSame(MessageType.PUBLISH, message.getMessageType());

		assertEquals(1, message.getMessageId());
		assertEquals("net.sf/message/topic", message.getTopicName());
		assertEquals(QoS.AT_LEAST_ONCE, message.getQoS());
		assertEquals(1, message.getQoSLevel());
		assertArrayEquals(new byte[0], message.getPayload());
		assertFalse(message.isDuplicate());
		assertFalse(message.isRetain());
		assertArrayEquals(emptyPayloadBytes, message.buffer.array());
	}

	@Test
	public void testOutboundCtor_Qos1_NotDuplicateNotRetain() {
		PubMessage message = new PubMessage(QoS.AT_LEAST_ONCE, false, "net.sf/message/topic", 1, paylaod);

		assertSame(MessageType.PUBLISH, message.getMessageType());

		assertEquals(1, message.getMessageId());
		assertEquals("net.sf/message/topic", message.getTopicName());
		assertEquals(QoS.AT_LEAST_ONCE, message.getQoS());
		assertEquals(1, message.getQoSLevel());
		assertArrayEquals(paylaod, message.getPayload());
		assertFalse(message.isDuplicate());
		assertFalse(message.isRetain());
		qos1Bytes[0] -= 9;
		assertArrayEquals(qos1Bytes, message.buffer.array());
	}

	@Test
	public void testOutboundCtor_Qos1_DuplicateNotRetain() {
		PubMessage message = new PubMessage(QoS.AT_LEAST_ONCE, false, "net.sf/message/topic", 1, paylaod);
		message.setDuplicateFlag();

		assertSame(MessageType.PUBLISH, message.getMessageType());

		assertEquals(1, message.getMessageId());
		assertEquals("net.sf/message/topic", message.getTopicName());
		assertEquals(QoS.AT_LEAST_ONCE, message.getQoS());
		assertEquals(1, message.getQoSLevel());
		assertArrayEquals(paylaod, message.getPayload());
		assertTrue(message.isDuplicate());
		assertFalse(message.isRetain());
		qos1Bytes[0] -= 1;
		byte[] bytes = new byte[qos1Bytes.length];
		message.buffer.get(bytes);
		assertArrayEquals(qos1Bytes, bytes);
	}

	@Test
	public void testOutboundCtor_Qos1_NotDuplicateRetain() {
		PubMessage message = new PubMessage(QoS.AT_LEAST_ONCE, true, "net.sf/message/topic", 1, paylaod);

		assertSame(MessageType.PUBLISH, message.getMessageType());

		assertEquals(1, message.getMessageId());
		assertEquals("net.sf/message/topic", message.getTopicName());
		assertEquals(QoS.AT_LEAST_ONCE, message.getQoS());
		assertEquals(1, message.getQoSLevel());
		assertArrayEquals(paylaod, message.getPayload());
		assertFalse(message.isDuplicate());
		assertTrue(message.isRetain());
		qos1Bytes[0] -= 8;
		byte[] bytes = new byte[qos1Bytes.length];
		message.buffer.get(bytes);
		assertArrayEquals(qos1Bytes, bytes);
	}

	@Test
	public void testOutboundCtor_Qos1_DuplicateRetain() {
		PubMessage message = new PubMessage(QoS.AT_LEAST_ONCE, true, "net.sf/message/topic", 1, paylaod);
		message.setDuplicateFlag();

		assertSame(MessageType.PUBLISH, message.getMessageType());

		assertEquals(1, message.getMessageId());
		assertEquals("net.sf/message/topic", message.getTopicName());
		assertEquals(QoS.AT_LEAST_ONCE, message.getQoS());
		assertEquals(1, message.getQoSLevel());
		assertArrayEquals(paylaod, message.getPayload());
		assertTrue(message.isDuplicate());
		assertTrue(message.isRetain());
		byte[] bytes = new byte[qos1Bytes.length];
		message.buffer.get(bytes);
		assertArrayEquals(qos1Bytes, bytes);
	}

	@Test
	public void testInboundCtor_EmptyPayload() {

		PubMessage message = new PubMessage(ByteBuffer.wrap(emptyPayloadBytes), 90, 0);

		assertSame(MessageType.PUBLISH, message.getMessageType());

		assertEquals(1, message.getMessageId());
		assertEquals("net.sf/message/topic", message.getTopicName());
		assertEquals(QoS.AT_LEAST_ONCE, message.getQoS());
		assertEquals(1, message.getQoSLevel());
		assertArrayEquals(new byte[0], message.getPayload());
		assertFalse(message.isDuplicate());
		assertFalse(message.isRetain());
		assertArrayEquals(emptyPayloadBytes, message.buffer.array());
	}

	@Test
	public void testInboundCtor_Qos1() {
		PubMessage message = new PubMessage(ByteBuffer.wrap(qos1Bytes), 90, 0);

		assertSame(MessageType.PUBLISH, message.getMessageType());

		assertEquals(1, message.getMessageId());
		assertEquals("net.sf/message/topic", message.getTopicName());
		assertEquals(QoS.AT_LEAST_ONCE, message.getQoS());
		assertEquals(1, message.getQoSLevel());
		assertArrayEquals(paylaod, message.getPayload());
		assertTrue(message.isDuplicate());
		assertTrue(message.isRetain());
	}

	@Test
	public void testSetMessageId_Qos1() {
		PubMessage message = new PubMessage(ByteBuffer.wrap(qos1Bytes), 90, 0);

		assertSame(MessageType.PUBLISH, message.getMessageType());

		assertEquals(1, message.getMessageId());
		assertEquals("net.sf/message/topic", message.getTopicName());
		assertEquals(QoS.AT_LEAST_ONCE, message.getQoS());
		assertEquals(1, message.getQoSLevel());
		assertArrayEquals(paylaod, message.getPayload());
		assertTrue(message.isDuplicate());
		assertTrue(message.isRetain());

		message.setMessageId(7);
		assertEquals(7, message.getMessageId());
	}

	@Test
	public void testOutboundCtor_Qos0_NotDuplicateNotRetain() {
		PubMessage message = new PubMessage(QoS.AT_MOST_ONCE, false, "net.sf/message/topic", 1, paylaod);

		assertSame(MessageType.PUBLISH, message.getMessageType());

		assertEquals(0, message.getMessageId());
		assertEquals("net.sf/message/topic", message.getTopicName());
		assertEquals(QoS.AT_MOST_ONCE, message.getQoS());
		assertEquals(0, message.getQoSLevel());
		assertArrayEquals(paylaod, message.getPayload());
		assertFalse(message.isDuplicate());
		assertFalse(message.isRetain());
		qos0Bytes[0] -= 9;
		byte[] bytes = new byte[qos0Bytes.length];
		message.buffer.get(bytes);
		assertArrayEquals(qos0Bytes, bytes);
	}

	@Test
	public void testOutboundCtor_Qos0_DuplicateNotRetain() {
		PubMessage message = new PubMessage(QoS.AT_MOST_ONCE, false, "net.sf/message/topic", 1, paylaod);
		message.setDuplicateFlag();

		assertSame(MessageType.PUBLISH, message.getMessageType());

		assertEquals(0, message.getMessageId());
		assertEquals("net.sf/message/topic", message.getTopicName());
		assertEquals(QoS.AT_MOST_ONCE, message.getQoS());
		assertEquals(0, message.getQoSLevel());
		assertArrayEquals(paylaod, message.getPayload());
		assertTrue(message.isDuplicate());
		assertFalse(message.isRetain());
		qos0Bytes[0] -= 1;
		byte[] bytes = new byte[qos0Bytes.length];
		message.buffer.get(bytes);
		assertArrayEquals(qos0Bytes, bytes);
	}

	@Test
	public void testOutboundCtor_Qos0_NotDuplicateRetain() {
		PubMessage message = new PubMessage(QoS.AT_MOST_ONCE, true, "net.sf/message/topic", 1, paylaod);

		assertSame(MessageType.PUBLISH, message.getMessageType());

		assertEquals(0, message.getMessageId());
		assertEquals("net.sf/message/topic", message.getTopicName());
		assertEquals(QoS.AT_MOST_ONCE, message.getQoS());
		assertEquals(0, message.getQoSLevel());
		assertArrayEquals(paylaod, message.getPayload());
		assertFalse(message.isDuplicate());
		assertTrue(message.isRetain());
		qos0Bytes[0] -= 8;
		byte[] bytes = new byte[qos0Bytes.length];
		message.buffer.get(bytes);
		assertArrayEquals(qos0Bytes, bytes);
	}

	@Test
	public void testOutboundCtor_Qos0_DuplicateRetain() {
		PubMessage message = new PubMessage(QoS.AT_MOST_ONCE, true, "net.sf/message/topic", 1, paylaod);
		message.setDuplicateFlag();

		assertSame(MessageType.PUBLISH, message.getMessageType());

		assertEquals(0, message.getMessageId());
		assertEquals("net.sf/message/topic", message.getTopicName());
		assertEquals(QoS.AT_MOST_ONCE, message.getQoS());
		assertEquals(0, message.getQoSLevel());
		assertArrayEquals(paylaod, message.getPayload());
		assertTrue(message.isDuplicate());
		assertTrue(message.isRetain());
		byte[] bytes = new byte[qos0Bytes.length];
		message.buffer.get(bytes);
		assertArrayEquals(qos0Bytes, bytes);
	}

	@Test
	public void testInboundCtor_Qos0() {
		PubMessage message = new PubMessage(ByteBuffer.wrap(qos0Bytes), 90, 0);

		assertSame(MessageType.PUBLISH, message.getMessageType());

		assertEquals(0, message.getMessageId());
		assertEquals("net.sf/message/topic", message.getTopicName());
		assertEquals(QoS.AT_MOST_ONCE, message.getQoS());
		assertEquals(0, message.getQoSLevel());
		assertArrayEquals(paylaod, message.getPayload());
		assertTrue(message.isDuplicate());
		assertTrue(message.isRetain());
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testSetMessageId_Qos0() {

		PubMessage message = new PubMessage(ByteBuffer.wrap(qos0Bytes), 90, 0);
		message.setMessageId(7);
	}
}
