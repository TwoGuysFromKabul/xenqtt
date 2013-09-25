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

import java.nio.ByteBuffer;

/**
 * A PUBREL message is the response either from a publisher to a PUBREC message from the server, or from the server to a PUBREC message from a subscriber. It is
 * the third message in the QoS 2 protocol flow.
 */
public final class PubRelMessage extends IdentifiableMqttMessage {

	/**
	 * Used to construct a received message.
	 */
	public PubRelMessage(ByteBuffer buffer) {
		super(buffer, 2);
	}

	/**
	 * Used to construct a message for sending
	 */
	public PubRelMessage(int messageId) {
		super(MessageType.PUBREL, false, QoS.AT_LEAST_ONCE, false, 2);
		buffer.putShort((short) messageId);
		buffer.flip();
	}
}
