package net.sf.xenqtt.message;

import java.nio.ByteBuffer;

/**
 * A PUBLISH message is sent by a client to a server for distribution to interested subscribers. Each PUBLISH message is associated with a topic name (also
 * known as the Subject or Channel). This is a hierarchical name space that defines a taxonomy of information sources for which subscribers can register an
 * interest. A message that is published to a specific topic name is delivered to connected subscribers for that topic.
 * <p>
 * If a client subscribes to one or more topics, any message published to those topics are sent by the server to the client as a PUBLISH message.
 */
public final class PublishMessage extends MqttMessage {

	/**
	 * Used to construct a received message.
	 */
	public PublishMessage(ByteBuffer buffer, int remainingLength) {
		super(buffer, remainingLength);
	}

	/**
	 * Used to construct a message for sending
	 */
	public PublishMessage(boolean duplicate, QoS qos, boolean retain, String topicName, int messageId) {
		this(duplicate, qos, retain, stringToUtf8(topicName), messageId);
	}

	/**
	 * This must not contain Topic wildcard characters.
	 * <p>
	 * When received by a client that subscribed using wildcard characters, this string will be the absolute topic specified by the originating publisher and
	 * not the subscription string used by the client.
	 */
	public String getTopicName() {
		return getString(fixedHeaderEndOffset);
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
	 */
	public int getMessageId() {
		return buffer.getShort(getMessageIdIndex()) & 0xffff;
	}

	/**
	 * Sets the message ID
	 */
	public void setMessageId(int messageId) {
		buffer.putShort(getMessageIdIndex(), (short) messageId);
	}

	private int getMessageIdIndex() {
		return fixedHeaderEndOffset + 2 + (buffer.getShort(fixedHeaderEndOffset) & 0xffff);
	}

	private PublishMessage(boolean duplicate, QoS qos, boolean retain, byte[] topicNameUtf8, int messageId) {
		super(MessageType.PUBLISH, duplicate, qos, retain, 2 + mqttStringSize(topicNameUtf8));

		putString(topicNameUtf8);
		buffer.putShort((short) messageId);
		buffer.flip();
	}
}
