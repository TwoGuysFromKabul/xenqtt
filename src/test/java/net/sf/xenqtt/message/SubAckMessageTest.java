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
package net.sf.xenqtt.message;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;

import org.junit.Test;

public class SubAckMessageTest {

	static final byte[] RECEIVED = new byte[] { -112, 9, 0, 1, 0, 1, 0, 1, 0, 1, 0 };

	@Test
	public void testInboundCtor() {
		QoS[] qoses = new QoS[1];
		qoses[0] = QoS.AT_LEAST_ONCE;
		SubAckMessage message = new SubAckMessage(1, qoses);

		assertEquals(1, message.getMessageId());
		assertArrayEquals(qoses, message.getGrantedQoses());
	}

	@Test
	public void testInboundCtor_MultipleGrantedQoses() {
		QoS[] qoses = new QoS[7];
		for (int i = 0; i < 7; i++) {
			qoses[i] = QoS.lookup(i % 2);
		}
		SubAckMessage message = new SubAckMessage(1, qoses);

		assertEquals(1, message.getMessageId());
		assertArrayEquals(qoses, message.getGrantedQoses());
	}

	@Test
	public void testOutboundCtor() {
		QoS[] qoses = new QoS[7];
		for (int i = 0; i < 7; i++) {
			qoses[i] = QoS.lookup(i % 2);
		}
		SubAckMessage message = new SubAckMessage(ByteBuffer.wrap(RECEIVED), 9, 0);

		assertEquals(1, message.getMessageId());
		assertArrayEquals(qoses, message.getGrantedQoses());
	}

	@Test
	public void testSetMessageId() {
		QoS[] qoses = new QoS[7];
		for (int i = 0; i < 7; i++) {
			qoses[i] = QoS.lookup(i % 2);
		}
		SubAckMessage message = new SubAckMessage(ByteBuffer.wrap(RECEIVED), 9, 0);

		assertEquals(1, message.getMessageId());
		assertArrayEquals(qoses, message.getGrantedQoses());

		message.setMessageId(7);
		assertEquals(7, message.getMessageId());
	}

}
