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
 * The PINGREQ message is an "are you alive?" message that is sent from a connected client to the server.
 * <p>
 * The response to a PINGREQ message is a PINGRESP message.
 */
public final class PingReqMessage extends MqttMessage {

	/**
	 * Used to construct a received message.
	 */
	public PingReqMessage(ByteBuffer buffer) {
		super(buffer, 0);
	}

	/**
	 * Used to construct a message for sending
	 */
	public PingReqMessage() {
		super(MessageType.PINGREQ, 0);
		buffer.flip();
	}
}
