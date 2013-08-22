package net.sf.xenqtt.message;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;

import org.junit.Test;

public class ConnectMessageTest {

	static final byte[] OUTBOUND_DATA = new byte[] { 16, 65, 0, 6, 77, 81, 73, 115, 100, 112, 3, -20, 0, 7, 0, 8, 109, 114, 45, 98, 117, 114, 110, 115, 0, 17,
			110, 101, 116, 46, 115, 102, 47, 119, 105, 108, 108, 47, 116, 111, 112, 105, 99, 0, 4, 68, 111, 104, 33, 0, 8, 115, 109, 105, 116, 104, 101, 114,
			115, 0, 6, 104, 111, 117, 110, 100, 115 };

	@Test
	public void testInboundCtor() {
		ConnectMessage message = new ConnectMessage("mr-burns", false, 1, "smithers", "hounds");

		assertSame(MessageType.CONNECT, message.getMessageType());
		assertEquals("MQIsdp", message.getProtocolName());
		assertEquals(3, message.getProtocolVersion());
		assertSame(QoS.AT_MOST_ONCE, message.getQoS());
		assertEquals(0, message.getQoSLevel());
		assertFalse(message.isDuplicate());
		assertFalse(message.isRetain());
		assertTrue(message.isPasswordFlag());
		assertTrue(message.isUserNameFlag());
		assertFalse(message.isWillMessageFlag());
		assertFalse(message.isWillRetain());

		assertEquals("mr-burns", message.getClientId());
		assertFalse(message.isCleanSession());
		assertEquals(1, message.getKeepAliveSeconds());
		assertEquals("smithers", message.getUserName());
		assertEquals("hounds", message.getPassword());
		assertEquals(40, message.getRemainingLength());
	}

	@Test
	public void testInboundCtor_WillTopicAndMessage() {
		ConnectMessage message = new ConnectMessage("mr-burns", true, 3, "net.sf/will/topic", "Doh!", true);

		assertSame(MessageType.CONNECT, message.getMessageType());
		assertEquals("MQIsdp", message.getProtocolName());
		assertEquals(3, message.getProtocolVersion());
		assertSame(QoS.AT_MOST_ONCE, message.getQoS());
		assertEquals(0, message.getQoSLevel());
		assertFalse(message.isDuplicate());
		assertFalse(message.isRetain());
		assertFalse(message.isPasswordFlag());
		assertFalse(message.isUserNameFlag());

		assertEquals("mr-burns", message.getClientId());
		assertTrue(message.isCleanSession());
		assertEquals(3, message.getKeepAliveSeconds());
		assertEquals("net.sf/will/topic", message.getWillTopic());
		assertEquals("Doh!", message.getWillMessage());
		assertTrue(message.isWillRetain());
		assertTrue(message.isWillMessageFlag());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInboundCtor_WillMessageNoTopic() {
		new ConnectMessage("mr-burns", true, 3, null, "Doh!", true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInboundCtor_WillTopicNoMessage() {
		new ConnectMessage("mr-burns", true, 3, "net.sf/will/topic", null, true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInboundCtor_WillTopicEmpty() {
		new ConnectMessage("mr-burns", true, 3, "", "Doh!", true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testInboundCtor_PasswordNoUsername() {
		new ConnectMessage("mr-burns", true, 3, null, "hounds", "net.sf/will/topic", "Doh!", true);
	}

	@Test
	public void testInboundCtor_WillTopicMessageAndCredentials() {
		ConnectMessage message = new ConnectMessage("mr-burns", false, 7, "smithers", "hounds", "net.sf/will/topic", "Doh!", true);

		assertSame(MessageType.CONNECT, message.getMessageType());
		assertEquals("MQIsdp", message.getProtocolName());
		assertEquals(3, message.getProtocolVersion());
		assertSame(QoS.AT_MOST_ONCE, message.getQoS());
		assertEquals(0, message.getQoSLevel());
		assertFalse(message.isDuplicate());
		assertFalse(message.isRetain());
		assertTrue(message.isPasswordFlag());
		assertTrue(message.isUserNameFlag());

		assertEquals("mr-burns", message.getClientId());
		assertFalse(message.isCleanSession());
		assertEquals(7, message.getKeepAliveSeconds());
		assertEquals("net.sf/will/topic", message.getWillTopic());
		assertEquals("Doh!", message.getWillMessage());
		assertTrue(message.isWillRetain());
		assertTrue(message.isWillMessageFlag());
	}

	@Test
	public void testOutboundCtor() {
		ConnectMessage message = new ConnectMessage(ByteBuffer.wrap(OUTBOUND_DATA), 65);

		assertSame(MessageType.CONNECT, message.getMessageType());
		assertEquals("MQIsdp", message.getProtocolName());
		assertEquals(3, message.getProtocolVersion());
		assertSame(QoS.AT_MOST_ONCE, message.getQoS());
		assertEquals(0, message.getQoSLevel());
		assertFalse(message.isDuplicate());
		assertFalse(message.isRetain());
		assertTrue(message.isPasswordFlag());
		assertTrue(message.isUserNameFlag());

		assertEquals("mr-burns", message.getClientId());
		assertFalse(message.isCleanSession());
		assertEquals(7, message.getKeepAliveSeconds());
		assertEquals("net.sf/will/topic", message.getWillTopic());
		assertEquals("Doh!", message.getWillMessage());
		assertTrue(message.isWillRetain());
		assertTrue(message.isWillMessageFlag());
	}

}
