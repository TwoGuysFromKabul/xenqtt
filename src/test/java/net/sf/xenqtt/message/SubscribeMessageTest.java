package net.sf.xenqtt.message;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;

import org.junit.Test;

public class SubscribeMessageTest {

	static final byte[] PAYLOAD = new byte[] { -126, 33, 0, 1, 0, 5, 97, 108, 112, 104, 97, 1, 0, 4, 98, 101, 116, 97, 1, 0, 5, 100, 101, 108, 116, 97, 0, 0,
			5, 103, 97, 109, 109, 97, 1 };

	@Test
	public void testOutboundCtor() {
		String[] topics = new String[] { "alpha", "beta", "delta", "gamma" };
		QoS[] requestedQoses = new QoS[] { QoS.AT_LEAST_ONCE, QoS.AT_LEAST_ONCE, QoS.AT_MOST_ONCE, QoS.AT_LEAST_ONCE };
		SubscribeMessage message = new SubscribeMessage(1, topics, requestedQoses);

		assertSame(MessageType.SUBSCRIBE, message.getMessageType());
		assertSame(QoS.AT_LEAST_ONCE, message.getQoS());
		assertEquals(1, message.getQoSLevel());
		assertFalse(message.isDuplicate());
		assertFalse(message.isRetain());

		assertEquals(1, message.getMessageId());
		assertArrayEquals(topics, message.getTopics());
		assertArrayEquals(requestedQoses, message.getRequestedQoSes());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testOutboundCtor_TooManyTopics() {
		String[] topics = new String[] { "alpha", "beta", "delta", "gamma", "sigma" };
		QoS[] requestedQoses = new QoS[] { QoS.AT_LEAST_ONCE, QoS.AT_LEAST_ONCE, QoS.AT_MOST_ONCE, QoS.AT_LEAST_ONCE };
		new SubscribeMessage(1, topics, requestedQoses);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testOutboundCtor_TooManyQoses() {
		String[] topics = new String[] { "alpha", "beta", "delta", "gamma" };
		QoS[] requestedQoses = new QoS[] { QoS.AT_LEAST_ONCE, QoS.AT_LEAST_ONCE, QoS.AT_MOST_ONCE, QoS.AT_LEAST_ONCE, QoS.AT_LEAST_ONCE };
		new SubscribeMessage(1, topics, requestedQoses);
	}

	@Test(expected = NullPointerException.class)
	public void testOutboundCtor_MissingTopics() {
		QoS[] requestedQoses = new QoS[] { QoS.AT_LEAST_ONCE, QoS.AT_LEAST_ONCE, QoS.AT_MOST_ONCE, QoS.AT_LEAST_ONCE, QoS.AT_LEAST_ONCE };
		new SubscribeMessage(1, null, requestedQoses);
	}

	@Test(expected = NullPointerException.class)
	public void testOutboundCtor_MissingQoses() {
		String[] topics = new String[] { "alpha", "beta", "delta", "gamma" };
		new SubscribeMessage(1, topics, null);
	}

	@Test(expected = NullPointerException.class)
	public void testOutboundCtor_MissingBoth() {
		new SubscribeMessage(1, null, null);
	}

	@Test
	public void testInboundCtor() {
		String[] topics = new String[] { "alpha", "beta", "delta", "gamma" };
		QoS[] requestedQoses = new QoS[] { QoS.AT_LEAST_ONCE, QoS.AT_LEAST_ONCE, QoS.AT_MOST_ONCE, QoS.AT_LEAST_ONCE };
		SubscribeMessage message = new SubscribeMessage(ByteBuffer.wrap(PAYLOAD), 33);

		assertSame(MessageType.SUBSCRIBE, message.getMessageType());
		assertSame(QoS.AT_LEAST_ONCE, message.getQoS());
		assertEquals(1, message.getQoSLevel());
		assertFalse(message.isDuplicate());
		assertFalse(message.isRetain());

		assertEquals(1, message.getMessageId());
		assertArrayEquals(topics, message.getTopics());
		assertArrayEquals(requestedQoses, message.getRequestedQoSes());

		assertArrayEquals(PAYLOAD, message.buffer.array());
	}

	@Test
	public void testSetMessageId() {
		String[] topics = new String[] { "alpha", "beta", "delta", "gamma" };
		QoS[] requestedQoses = new QoS[] { QoS.AT_LEAST_ONCE, QoS.AT_LEAST_ONCE, QoS.AT_MOST_ONCE, QoS.AT_LEAST_ONCE };
		SubscribeMessage message = new SubscribeMessage(ByteBuffer.wrap(PAYLOAD), 33);

		assertSame(MessageType.SUBSCRIBE, message.getMessageType());
		assertSame(QoS.AT_LEAST_ONCE, message.getQoS());
		assertEquals(1, message.getQoSLevel());
		assertFalse(message.isDuplicate());
		assertFalse(message.isRetain());

		assertEquals(1, message.getMessageId());
		assertArrayEquals(topics, message.getTopics());
		assertArrayEquals(requestedQoses, message.getRequestedQoSes());

		assertArrayEquals(PAYLOAD, message.buffer.array());

		message.setMessageId(2);
		assertEquals(2, message.getMessageId());
	}

}
