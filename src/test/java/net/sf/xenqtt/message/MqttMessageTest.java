package net.sf.xenqtt.message;

import static org.junit.Assert.*;

import org.junit.Test;

public class MqttMessageTest {

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
		MqttMessage message = new MqttMessage(MessageType.CONNECT, false, QoS.EXACTLY_ONCE, false, 24) {
		};
		message.buffer.flip();

		assertEquals(MessageType.CONNECT, message.getMessageType());
		assertEquals(24, message.getRemainingLength());
		assertEquals(QoS.EXACTLY_ONCE, message.getQoS());
		assertEquals(0, message.buffer.get(0) & 0x08);
		assertEquals(0, message.buffer.get(0) & 0x01);
	}

	@Test
	public void testInboundCtor_MessageTypeAndRemainingLengthAndFlags_Duplicate() {
		MqttMessage message = new MqttMessage(MessageType.CONNECT, true, QoS.EXACTLY_ONCE, false, 24) {
		};
		message.buffer.flip();

		assertEquals(MessageType.CONNECT, message.getMessageType());
		assertEquals(24, message.getRemainingLength());
		assertEquals(QoS.EXACTLY_ONCE, message.getQoS());
		assertEquals(0x08, message.buffer.get(0) & 0x08);
		assertEquals(0, message.buffer.get(0) & 0x01);
	}

	@Test
	public void testInboundCtor_MessageTypeAndRemainingLengthAndFlags_Retain() {
		MqttMessage message = new MqttMessage(MessageType.CONNECT, false, QoS.EXACTLY_ONCE, true, 24) {
		};
		message.buffer.flip();

		assertEquals(MessageType.CONNECT, message.getMessageType());
		assertEquals(24, message.getRemainingLength());
		assertEquals(QoS.EXACTLY_ONCE, message.getQoS());
		assertEquals(0, message.buffer.get(0) & 0x08);
		assertEquals(0x01, message.buffer.get(0) & 0x01);
	}

	@Test
	public void testInboundCtor_MessageTypeAndRemainingLengthAndFlags_AllFlags() {
		MqttMessage message = new MqttMessage(MessageType.CONNECT, true, QoS.EXACTLY_ONCE, true, 24) {
		};
		message.buffer.flip();

		assertEquals(MessageType.CONNECT, message.getMessageType());
		assertEquals(24, message.getRemainingLength());
		assertEquals(QoS.EXACTLY_ONCE, message.getQoS());
		assertEquals(0x08, message.buffer.get(0) & 0x08);
		assertEquals(0x01, message.buffer.get(0) & 0x01);
	}

	@Test
	public void testOutboundCtor() {
		fail("Need to test this.");
	}

}
