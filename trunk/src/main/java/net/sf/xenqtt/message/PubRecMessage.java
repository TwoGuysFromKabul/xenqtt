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
 * A PUBREC message is the response to a PUBLISH message with QoS level 2. It is the second message of the QoS level 2 protocol flow. A PUBREC message is sent
 * by the server in response to a PUBLISH message from a publishing client, or by a subscriber in response to a PUBLISH message from the server.
 * <p>
 * When it receives a PUBREC message, the recipient sends a PUBREL message to the sender with the same Message ID as the PUBREC message.
 */
public final class PubRecMessage extends IdentifiableMqttMessage {

	/**
	 * Used to construct a received message.
	 */
	public PubRecMessage(ByteBuffer buffer, long receivedTimestamp) {
		super(buffer, 2, receivedTimestamp);
	}

	/**
	 * Used to construct a message for sending
	 */
	public PubRecMessage(int messageId) {
		super(MessageType.PUBREC, 2);
		buffer.putShort((short) messageId);
		buffer.flip();
	}
}
