package net.sf.xenqtt.message;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;

import org.junit.Test;

public class DisconnectMessageTest {

	ByteBuffer buf = ByteBuffer.wrap(new byte[] { (byte) 0xe0, 0x00 });

	DisconnectMessage msg;

	@Test
	public void testCtor_Receive() {

		msg = new DisconnectMessage(buf);
		assertMsg();
	}

	@Test
	public void testCtor_Send() {
		msg = new DisconnectMessage();
		assertMsg();
	}

	private void assertMsg() {

		assertEquals(buf, msg.buffer);

		assertEquals(MessageType.DISCONNECT, msg.getMessageType());
		assertFalse(msg.isDuplicate());
		assertEquals(QoS.AT_MOST_ONCE, msg.getQoS());
		assertFalse(msg.isRetain());
		assertEquals(0, msg.getRemainingLength());
	}

}
