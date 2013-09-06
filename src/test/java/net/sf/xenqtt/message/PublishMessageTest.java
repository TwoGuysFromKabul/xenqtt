package net.sf.xenqtt.message;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import org.junit.Test;

public class PublishMessageTest {

	static final byte[] PAYLOAD = "To alcohol - the cause of, and solution to, all of life's problems".getBytes(Charset.forName("UTF-8"));
	static final byte[] RECEIVED = new byte[] { 61, 90, 0, 20, 110, 101, 116, 46, 115, 102, 47, 109, 101, 115, 115, 97, 103, 101, 47, 116, 111, 112, 105, 99,
			0, 1, 84, 111, 32, 97, 108, 99, 111, 104, 111, 108, 32, 45, 32, 116, 104, 101, 32, 99, 97, 117, 115, 101, 32, 111, 102, 44, 32, 97, 110, 100, 32,
			115, 111, 108, 117, 116, 105, 111, 110, 32, 116, 111, 44, 32, 97, 108, 108, 32, 111, 102, 32, 108, 105, 102, 101, 39, 115, 32, 112, 114, 111, 98,
			108, 101, 109, 115 };

	@Test
	public void testInboundCtor_NotDuplicateNotRetain() {
		PubMessage message = new PubMessage(QoS.AT_LEAST_ONCE, false, "net.sf/message/topic", 1, PAYLOAD);

		assertSame(MessageType.PUBLISH, message.getMessageType());

		assertEquals(1, message.getMessageId());
		assertEquals("net.sf/message/topic", message.getTopicName());
		assertEquals(QoS.AT_LEAST_ONCE, message.getQoS());
		assertEquals(2, message.getQoSLevel());
		assertArrayEquals(PAYLOAD, message.getPayload());
		assertFalse(message.isDuplicate());
		assertFalse(message.isRetain());
	}

	@Test
	public void testInboundCtor_DuplicateNotRetain() {
		PubMessage message = new PubMessage(QoS.AT_LEAST_ONCE, false, "net.sf/message/topic", 1, PAYLOAD);
		message.setDuplicateFlag();

		assertSame(MessageType.PUBLISH, message.getMessageType());

		assertEquals(1, message.getMessageId());
		assertEquals("net.sf/message/topic", message.getTopicName());
		assertEquals(QoS.AT_LEAST_ONCE, message.getQoS());
		assertEquals(2, message.getQoSLevel());
		assertArrayEquals(PAYLOAD, message.getPayload());
		assertTrue(message.isDuplicate());
		assertFalse(message.isRetain());
	}

	@Test
	public void testInboundCtor_NotDuplicateRetain() {
		PubMessage message = new PubMessage(QoS.AT_LEAST_ONCE, true, "net.sf/message/topic", 1, PAYLOAD);

		assertSame(MessageType.PUBLISH, message.getMessageType());

		assertEquals(1, message.getMessageId());
		assertEquals("net.sf/message/topic", message.getTopicName());
		assertEquals(QoS.AT_LEAST_ONCE, message.getQoS());
		assertEquals(2, message.getQoSLevel());
		assertArrayEquals(PAYLOAD, message.getPayload());
		assertFalse(message.isDuplicate());
		assertTrue(message.isRetain());
	}

	@Test
	public void testInboundCtor_DuplicateRetain() {
		PubMessage message = new PubMessage(QoS.AT_LEAST_ONCE, true, "net.sf/message/topic", 1, PAYLOAD);
		message.setDuplicateFlag();

		assertSame(MessageType.PUBLISH, message.getMessageType());

		assertEquals(1, message.getMessageId());
		assertEquals("net.sf/message/topic", message.getTopicName());
		assertEquals(QoS.AT_LEAST_ONCE, message.getQoS());
		assertEquals(2, message.getQoSLevel());
		assertArrayEquals(PAYLOAD, message.getPayload());
		assertTrue(message.isDuplicate());
		assertTrue(message.isRetain());
	}

	@Test
	public void testOutboundCtor() {
		PubMessage message = new PubMessage(ByteBuffer.wrap(RECEIVED), 90);

		assertSame(MessageType.PUBLISH, message.getMessageType());

		assertEquals(1, message.getMessageId());
		assertEquals("net.sf/message/topic", message.getTopicName());
		assertEquals(QoS.AT_LEAST_ONCE, message.getQoS());
		assertEquals(2, message.getQoSLevel());
		assertArrayEquals(PAYLOAD, message.getPayload());
		assertTrue(message.isDuplicate());
		assertTrue(message.isRetain());
	}

	@Test
	public void testSetMessageId() {
		PubMessage message = new PubMessage(ByteBuffer.wrap(RECEIVED), 90);

		assertSame(MessageType.PUBLISH, message.getMessageType());

		assertEquals(1, message.getMessageId());
		assertEquals("net.sf/message/topic", message.getTopicName());
		assertEquals(QoS.AT_LEAST_ONCE, message.getQoS());
		assertEquals(2, message.getQoSLevel());
		assertArrayEquals(PAYLOAD, message.getPayload());
		assertTrue(message.isDuplicate());
		assertTrue(message.isRetain());

		message.setMessageId(7);
		assertEquals(7, message.getMessageId());
	}

}
