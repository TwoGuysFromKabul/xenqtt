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
 * The DISCONNECT message is sent from the client to the server to indicate that it is about to close its TCP/IP connection. This allows for a clean
 * disconnection, rather than just dropping the line.
 * <p>
 * If the client had connected with the clean session flag set, then all previously maintained information about the client will be discarded.
 * <p>
 * A server should not rely on the client to close the TCP/IP connection after receiving a DISCONNECT.
 */
public final class DisconnectMessage extends MqttMessage {

	/**
	 * Used to construct a received message.
	 */
	public DisconnectMessage(ByteBuffer buffer, long receivedTimestamp) {
		super(buffer, 0, receivedTimestamp);
	}

	/**
	 * Used to construct a message for sending
	 */
	public DisconnectMessage() {
		super(MessageType.DISCONNECT, 0);
		buffer.flip();
	}
}
