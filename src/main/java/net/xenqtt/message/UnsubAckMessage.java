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

import java.nio.ByteBuffer;

/**
 * The UNSUBACK message is sent by the server to the client to confirm receipt of an UNSUBSCRIBE message.
 */
public final class UnsubAckMessage extends IdentifiableMqttMessage {

	/**
	 * Used to construct a received message.
	 */
	public UnsubAckMessage(ByteBuffer buffer, long receivedTimestamp) {
		super(buffer, 2, receivedTimestamp);
	}

	/**
	 * Used to construct a message for sending
	 */
	public UnsubAckMessage(int messageId) {
		super(MessageType.UNSUBACK, 2);
		buffer.putShort((short) messageId);
		buffer.flip();
	}
}
