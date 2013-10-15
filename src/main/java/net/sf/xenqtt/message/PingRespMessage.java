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
 * A PINGRESP message is the response sent by a server to a PINGREQ message and means "yes I am alive".
 */
public final class PingRespMessage extends MqttMessage {

	/**
	 * Used to construct a received message.
	 */
	public PingRespMessage(ByteBuffer buffer, long receivedTimestamp) {
		super(buffer, 0, receivedTimestamp);
	}

	/**
	 * Used to construct a message for sending
	 */
	public PingRespMessage() {
		super(MessageType.PINGRESP, 0);
		buffer.flip();
	}
}
