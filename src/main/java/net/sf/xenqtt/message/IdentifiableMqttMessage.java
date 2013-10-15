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
 * Adds the message ID property to {@link MqttMessage}. Since different messages have this property in different places the implementations are abstract.
 */
public abstract class IdentifiableMqttMessage extends MqttMessage {

	/**
	 * Used by {@link AbstractMqttChannel} to keep track of when to resend an unack'd message
	 */
	long nextSendTime;

	/**
	 * @see MqttMessage#MqttMessage(ByteBuffer, int)
	 */
	public IdentifiableMqttMessage(ByteBuffer buffer, int remainingLength, long receivedTimestamp) {
		super(buffer, remainingLength, receivedTimestamp);
	}

	/**
	 * @see MqttMessage#MqttMessage(MessageType, int)
	 */
	public IdentifiableMqttMessage(MessageType messageType, int remainingLength) {
		super(messageType, remainingLength);
	}

	/**
	 * @see MqttMessage#MqttMessage(MessageType, boolean, QoS, boolean, int)
	 */
	public IdentifiableMqttMessage(MessageType messageType, boolean duplicate, QoS qos, boolean retain, int remainingLength) {
		super(messageType, duplicate, qos, retain, remainingLength);
	}

	/**
	 * The message identifier is present in the variable header of the following MQTT messages: PUBLISH, PUBACK, PUBREC, PUBREL, PUBCOMP, SUBSCRIBE, SUBACK,
	 * UNSUBSCRIBE, UNSUBACK.
	 * <p>
	 * The Message Identifier (Message ID) field is only present in messages where the QoS bits in the fixed header indicate QoS levels 1 or 2. See section on
	 * Quality of Service levels and flows for more information.
	 * <p>
	 * The Message ID is a 16-bit unsigned integer that must be unique amongst the set of "in flight" messages in a particular direction of communication. It
	 * typically increases by exactly one from one message to the next, but is not required to do so.
	 * <p>
	 * A client will maintain its own list of Message IDs separate to the Message IDs used by the server it is connected to. It is possible for a client to send
	 * a PUBLISH with Message ID 1 at the same time as receiving a PUBLISH with Message ID 1.
	 * <p>
	 * Do not use Message ID 0. It is reserved as an invalid Message ID.
	 * <p>
	 * Override this implementation for messages where the message ID is not at offset 2 of a fixed length message
	 */
	public int getMessageId() {
		return buffer.getShort(2) & 0xffff;
	}

	/**
	 * Sets the message ID
	 * <p>
	 * Override this implementation for messages where the message ID is not at offset 2 of a fixed length message
	 */
	public void setMessageId(int messageId) {
		buffer.putShort(2, (short) messageId);
	}
}
