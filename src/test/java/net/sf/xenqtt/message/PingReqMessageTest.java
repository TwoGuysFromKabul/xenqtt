package net.sf.xenqtt.message;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;

import org.junit.Test;

public class PingReqMessageTest {

	ByteBuffer buf = ByteBuffer.wrap(new byte[] { (byte) 0xc0, 0x00 });

	PingReqMessage msg;

	@Test
	public void testCtor_Receive() {

		msg = new PingReqMessage(buf);
		assertMsg();
	}

	@Test
	public void testCtor_Send() {
		msg = new PingReqMessage();
		assertMsg();
	}

	private void assertMsg() {

		assertEquals(buf, msg.buffer);

		assertEquals(MessageType.PINGREQ, msg.getMessageType());
		assertFalse(msg.isDuplicate());
		assertEquals(QoS.AT_MOST_ONCE, msg.getQoS());
		assertFalse(msg.isRetain());
		assertEquals(0, msg.getRemainingLength());
	}
}
