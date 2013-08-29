package net.sf.xenqtt.message;

import static org.junit.Assert.*;

import org.junit.Test;

public class IdentifiableMqttMessageTest {

	@Test
	public void testSetMessageId() {
		TestIdentifiableMqttMessage message = new TestIdentifiableMqttMessage(MessageType.PUBLISH, 2);
		message.setMessageId(7);
		assertEquals(7, message.getMessageId());
	}

	private static final class TestIdentifiableMqttMessage extends IdentifiableMqttMessage {

		public TestIdentifiableMqttMessage(MessageType messageType, int remainingLength) {
			super(messageType, remainingLength);
		}

	}

}
