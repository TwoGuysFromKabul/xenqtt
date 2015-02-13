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
 * The CONNACK message is the message sent by the server in response to a CONNECT request from a client.
 */
public final class ConnAckMessage extends MqttMessage {

	/**
	 * Used to construct a received message.
	 */
	public ConnAckMessage(ByteBuffer buffer, long receivedTimestamp) {
		super(buffer, 2, receivedTimestamp);
	}

	/**
	 * Used to construct a message for sending
	 */
	public ConnAckMessage(ConnectReturnCode returnCode) {
		super(MessageType.CONNACK, 2);

		buffer.put((byte) 0); // Topic Name Compression Response.Reserved values. Not used.
		buffer.put((byte) returnCode.value());
		buffer.flip();
	}

	/**
	 * @return The resulting status of the connect attempt
	 */
	public ConnectReturnCode getReturnCode() {
		return ConnectReturnCode.lookup(buffer.get(3) & 0xff);
	}

	/**
	 * Sets the return code
	 */
	public void setReturnCode(ConnectReturnCode returnCode) {
		buffer.put(3, (byte) returnCode.value());
	}
}
