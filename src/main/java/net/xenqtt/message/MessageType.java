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

/**
 * MQTT Message types. The order must not be changed as the enum ordinal() is the numeric value of the message type.
 */
public enum MessageType {
	UNUSED_0, // Reserved
	CONNECT, // Client request to connect to Server
	CONNACK, // Connect Acknowledgment
	PUBLISH, // Publish message
	PUBACK, // Publish Acknowledgment
	PUBREC, // Publish Received (assured delivery part 1)
	PUBREL, // Publish Release (assured delivery part 2)
	PUBCOMP, // Publish Complete (assured delivery part 3)
	SUBSCRIBE, // Client Subscribe request
	SUBACK, // Subscribe Acknowledgment
	UNSUBSCRIBE, // Client Unsubscribe request
	UNSUBACK, // Unsubscribe Acknowledgment
	PINGREQ, // PING Request
	PINGRESP, // PING Response
	DISCONNECT, // Client is Disconnecting
	UNUSED_15 // Reserved
	;

	/**
	 * @return The {@link MessageType} associated with the specified numeric value.
	 */
	public static MessageType lookup(int value) {
		return values()[value];
	}

	/**
	 * @return The numeric value for this {@link MessageType}
	 */
	public int value() {
		return ordinal();
	}
}
