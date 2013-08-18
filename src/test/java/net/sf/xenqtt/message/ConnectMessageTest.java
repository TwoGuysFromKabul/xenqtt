package net.sf.xenqtt.message;

import static org.junit.Assert.*;

import org.junit.Test;

public class ConnectMessageTest {

	// TODO [jeremy] - There appears to be a problem in calculating the remaining length. This test precipitates a buffer overflow exception.
	@Test
	public void testInboundCtor() {
		ConnectMessage message = new ConnectMessage("mr-burns", false, 1, "smithers", "hounds");

		assertEquals("mr-burns", message.getClientId());
		assertFalse(message.isCleanSession());
		assertEquals(1, message.getKeepAliveSeconds());
		assertEquals("smithers", message.getUserName());
		assertEquals("hounds", message.getPassword());
	}

}
