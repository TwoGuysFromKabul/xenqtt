/**
    Copyright 2013 James McClure

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package net.xenqtt.message;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;

import net.xenqtt.message.MessageType;
import net.xenqtt.message.QoS;
import net.xenqtt.message.UnsubscribeMessage;

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
		UnsubscribeMessage message = new UnsubscribeMessage(ByteBuffer.wrap(PAYLOAD), 29, 0);

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
		UnsubscribeMessage message = new UnsubscribeMessage(ByteBuffer.wrap(PAYLOAD), 29, 0);

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
