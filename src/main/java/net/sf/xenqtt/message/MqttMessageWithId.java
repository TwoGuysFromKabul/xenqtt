package net.sf.xenqtt.message;

/**
 * This interface is implemented by all {@link MqttMessage}s that have a message ID field.
 */
public interface MqttMessageWithId {

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
	 * 
	 * Do not use Message ID 0. It is reserved as an invalid Message ID.
	 */
	int getMessageId();

	/**
	 * Sets the message ID
	 */
	void setMessageId(int messageId);
}
