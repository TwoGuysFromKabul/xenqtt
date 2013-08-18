package net.sf.xenqtt.message;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;

import org.junit.Test;

public class ConnAckMessageTest {

	ByteBuffer buf = ByteBuffer.wrap(new byte[] { (byte) 0x20, 0x02, 0x00, 0x00 });

	ConnAckMessage msg;

	@Test
	public void testCtor_Receive() {

		msg = new ConnAckMessage(buf);
		assertMsg();
	}

	@Test
	public void testCtor_Send() {
		msg = new ConnAckMessage(ConnectReturnCode.ACCEPTED);
		assertMsg();
	}

	@Test
	public void testReturnCodes() throws Exception {

		for (ConnectReturnCode code : ConnectReturnCode.values()) {
			buf = ByteBuffer.wrap(new byte[] { (byte) 0x20, 0x02, 0x00, (byte) code.value() });
			msg = new ConnAckMessage(code);
			assertEquals(buf, msg.buffer);
			assertEquals(code, msg.getReturnCode());
		}

		for (int i = 0; i < ConnectReturnCode.values().length; i++) {
			buf = ByteBuffer.wrap(new byte[] { (byte) 0x20, 0x02, 0x00, (byte) i });
			msg = new ConnAckMessage(buf);
			assertEquals(ConnectReturnCode.lookup(i), msg.getReturnCode());
		}
	}

	private void assertMsg() {

		assertEquals(buf, msg.buffer);

		assertEquals(MessageType.CONNACK, msg.getMessageType());
		assertFalse(msg.isDuplicate());
		assertEquals(QoS.AT_MOST_ONCE, msg.getQoS());
		assertFalse(msg.isRetain());
		assertEquals(2, msg.getRemainingLength());

		assertEquals(ConnectReturnCode.ACCEPTED, msg.getReturnCode());
	}
}
