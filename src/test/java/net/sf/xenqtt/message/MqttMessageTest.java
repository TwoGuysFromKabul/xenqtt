package net.sf.xenqtt.message;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;

import org.junit.Test;

public class MqttMessageTest {

	static final byte[] PAYLOAD = new byte[] { 29, 24, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

	@Test
	public void testInboundCtor_MessageTypeAndRemainingLength() {
		MqttMessage message = new MqttMessage(MessageType.CONNECT, 24) {
		};
		message.buffer.flip();

		assertEquals(MessageType.CONNECT, message.getMessageType());
		assertEquals(24, message.getRemainingLength());
		assertEquals(QoS.AT_MOST_ONCE, message.getQoS());
	}

	@Test
	public void testInboundCtor_MessageTypeAndRemainingLengthAndFlags_FalseOnAllFlags() {
		MqttMessage message = new TestMessage(MessageType.CONNECT, false, QoS.AT_LEAST_ONCE, false, 24);
		message.buffer.flip();

		assertEquals(MessageType.CONNECT, message.getMessageType());
		assertEquals(24, message.getRemainingLength());
		assertEquals(QoS.AT_LEAST_ONCE, message.getQoS());
		assertEquals(0, message.buffer.get(0) & 0x08);
		assertEquals(0, message.buffer.get(0) & 0x01);
	}

	@Test
	public void testInboundCtor_MessageTypeAndRemainingLengthAndFlags_Duplicate() {
		MqttMessage message = new MqttMessage(MessageType.CONNECT, true, QoS.AT_LEAST_ONCE, false, 24) {
		};
		message.buffer.flip();

		assertEquals(MessageType.CONNECT, message.getMessageType());
		assertEquals(24, message.getRemainingLength());
		assertEquals(QoS.AT_LEAST_ONCE, message.getQoS());
		assertEquals(0x08, message.buffer.get(0) & 0x08);
		assertEquals(0, message.buffer.get(0) & 0x01);
	}

	@Test
	public void testInboundCtor_MessageTypeAndRemainingLengthAndFlags_Retain() {
		MqttMessage message = new MqttMessage(MessageType.CONNECT, false, QoS.AT_LEAST_ONCE, true, 24) {
		};
		message.buffer.flip();

		assertEquals(MessageType.CONNECT, message.getMessageType());
		assertEquals(24, message.getRemainingLength());
		assertEquals(QoS.AT_LEAST_ONCE, message.getQoS());
		assertEquals(0, message.buffer.get(0) & 0x08);
		assertEquals(0x01, message.buffer.get(0) & 0x01);
	}

	@Test
	public void testInboundCtor_MessageTypeAndRemainingLengthAndFlags_AllFlags() {
		MqttMessage message = new MqttMessage(MessageType.CONNECT, true, QoS.AT_LEAST_ONCE, true, 24) {
		};
		message.buffer.flip();

		assertEquals(MessageType.CONNECT, message.getMessageType());
		assertEquals(24, message.getRemainingLength());
		assertEquals(QoS.AT_LEAST_ONCE, message.getQoS());
		assertEquals(0x08, message.buffer.get(0) & 0x08);
		assertEquals(0x01, message.buffer.get(0) & 0x01);
	}

	@Test
	public void testInboundCtor() {
		MqttMessage message = new MqttMessage(ByteBuffer.wrap(PAYLOAD), 24) {
		};

		assertEquals(MessageType.CONNECT, message.getMessageType());
		assertEquals(24, message.getRemainingLength());
		assertEquals(QoS.AT_LEAST_ONCE, message.getQoS());
		assertEquals(0x08, message.buffer.get(0) & 0x08);
		assertEquals(0x01, message.buffer.get(0) & 0x01);

		assertArrayEquals(PAYLOAD, message.buffer.array());
	}

	@Test
	public void testIsAckable() throws Exception {

		assertFalse(new TestMessage(MessageType.CONNACK, 0).isAckable());
		assertFalse(new TestMessage(MessageType.CONNECT, 0).isAckable());
		assertFalse(new TestMessage(MessageType.DISCONNECT, 0).isAckable());
		assertFalse(new TestMessage(MessageType.PINGREQ, 0).isAckable());
		assertFalse(new TestMessage(MessageType.PINGRESP, 0).isAckable());
		assertFalse(new TestMessage(MessageType.PUBACK, 0).isAckable());
		assertFalse(new TestMessage(MessageType.PUBCOMP, 0).isAckable());
		assertTrue(new TestMessage(MessageType.PUBLISH, 0).isAckable());
		assertFalse(new TestMessage(MessageType.PUBREC, 0).isAckable());
		assertTrue(new TestMessage(MessageType.PUBREL, 0).isAckable());
		assertFalse(new TestMessage(MessageType.SUBACK, 0).isAckable());
		assertTrue(new TestMessage(MessageType.SUBSCRIBE, 0).isAckable());
		assertFalse(new TestMessage(MessageType.UNSUBACK, 0).isAckable());
		assertTrue(new TestMessage(MessageType.UNSUBSCRIBE, 0).isAckable());

	}

	@Test
	public void testIsAck() throws Exception {
		assertFalse(new TestMessage(MessageType.CONNACK, 0).isAck());
		assertFalse(new TestMessage(MessageType.CONNECT, 0).isAck());
		assertFalse(new TestMessage(MessageType.DISCONNECT, 0).isAck());
		assertFalse(new TestMessage(MessageType.PINGREQ, 0).isAck());
		assertFalse(new TestMessage(MessageType.PINGRESP, 0).isAck());
		assertTrue(new TestMessage(MessageType.PUBACK, 0).isAck());
		assertTrue(new TestMessage(MessageType.PUBCOMP, 0).isAck());
		assertFalse(new TestMessage(MessageType.PUBLISH, 0).isAck());
		assertTrue(new TestMessage(MessageType.PUBREC, 0).isAck());
		assertFalse(new TestMessage(MessageType.PUBREL, 0).isAck());
		assertTrue(new TestMessage(MessageType.SUBACK, 0).isAck());
		assertFalse(new TestMessage(MessageType.SUBSCRIBE, 0).isAck());
		assertTrue(new TestMessage(MessageType.UNSUBACK, 0).isAck());
		assertFalse(new TestMessage(MessageType.UNSUBSCRIBE, 0).isAck());
	}

	@Test
	public void testSetDuplicateFlag() throws Exception {

		MqttMessage message = new TestMessage(MessageType.CONNECT, false, QoS.AT_LEAST_ONCE, false, 24);
		message.buffer.flip();

		assertEquals(MessageType.CONNECT, message.getMessageType());
		assertEquals(24, message.getRemainingLength());
		assertEquals(QoS.AT_LEAST_ONCE, message.getQoS());
		assertEquals(0, message.buffer.get(0) & 0x08);
		assertEquals(0, message.buffer.get(0) & 0x01);
		assertFalse(message.isDuplicate());
		assertFalse(message.isRetain());

		message.setDuplicateFlag();

		assertEquals(MessageType.CONNECT, message.getMessageType());
		assertEquals(24, message.getRemainingLength());
		assertEquals(QoS.AT_LEAST_ONCE, message.getQoS());
		assertEquals(0x08, message.buffer.get(0) & 0x08);
		assertEquals(0, message.buffer.get(0) & 0x01);
		assertTrue(message.isDuplicate());
		assertFalse(message.isRetain());
	}

	private static class TestMessage extends MqttMessage {

		public TestMessage(ByteBuffer buffer, int remainingLength) {
			super(buffer, remainingLength);
		}

		public TestMessage(MessageType messageType, boolean duplicate, QoS qos, boolean retain, int remainingLength) {
			super(messageType, duplicate, qos, retain, remainingLength);
		}

		public TestMessage(MessageType messageType, int remainingLength) {
			super(messageType, remainingLength);
		}
	}
}
