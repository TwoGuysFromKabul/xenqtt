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
