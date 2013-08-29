package net.sf.xenqtt.message;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;

import org.junit.Test;

public class UnsubscribeMessageTest {

	static final byte[] PAYLOAD = new byte[] { -94, 29, 0, 1, 0, 5, 97, 108, 112, 104, 97, 0, 4, 98, 101, 116, 97, 0, 5, 100, 101, 108, 116, 97, 0, 5, 103, 97,
			109, 109, 97 };

	@Test
	public void testOutboundCtor() {
		String[] topics = new String[] { "alpha", "beta", "delta", "gamma" };
		UnsubscribeMessage message = new UnsubscribeMessage(1, topics);

		assertSame(MessageType.UNSUBSCRIBE, message.getMessageType());
		assertSame(QoS.AT_LEAST_ONCE, message.getQoS());
		assertEquals(1, message.getQoSLevel());
		assertFalse(message.isDuplicate());
		assertFalse(message.isRetain());

		assertEquals(1, message.getMessageId());
		assertArrayEquals(topics, message.getTopics());
	}

	@Test(expected = NullPointerException.class)
	public void testOutboundCtor_MissingTopics() {
		new UnsubscribeMessage(1, null);
	}

	@Test
	public void testInboundCtor() {
		String[] topics = new String[] { "alpha", "beta", "delta", "gamma" };
		UnsubscribeMessage message = new UnsubscribeMessage(ByteBuffer.wrap(PAYLOAD), 29);

		assertSame(MessageType.UNSUBSCRIBE, message.getMessageType());
		assertSame(QoS.AT_LEAST_ONCE, message.getQoS());
		assertEquals(1, message.getQoSLevel());
		assertFalse(message.isDuplicate());
		assertFalse(message.isRetain());

		assertEquals(1, message.getMessageId());
		assertArrayEquals(topics, message.getTopics());

		assertArrayEquals(PAYLOAD, message.buffer.array());
	}

	@Test
	public void testSetMessageId() {
		String[] topics = new String[] { "alpha", "beta", "delta", "gamma" };
		UnsubscribeMessage message = new UnsubscribeMessage(ByteBuffer.wrap(PAYLOAD), 29);

		assertSame(MessageType.UNSUBSCRIBE, message.getMessageType());
		assertSame(QoS.AT_LEAST_ONCE, message.getQoS());
		assertEquals(1, message.getQoSLevel());
		assertFalse(message.isDuplicate());
		assertFalse(message.isRetain());

		assertEquals(1, message.getMessageId());
		assertArrayEquals(topics, message.getTopics());

		assertArrayEquals(PAYLOAD, message.buffer.array());

		message.setMessageId(2);
		assertEquals(2, message.getMessageId());
	}

}
