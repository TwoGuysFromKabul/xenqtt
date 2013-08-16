package net.sf.xenqtt.message;

import java.nio.ByteBuffer;

/**
 * An UNSUBSCRIBE message is sent by the client to the server to unsubscribe from named topics.
 * <p>
 * The server sends an UNSUBACK to a client in response to an UNSUBSCRIBE message.
 */
public final class UnsubscribeMessage extends MqttMessage {

	private String[] topics;

	/**
	 * Used to construct a received message.
	 */
	public UnsubscribeMessage(ByteBuffer buffer, int remainingLength) {
		super(buffer, remainingLength);
	}

	/**
	 * Used to construct a message for sending
	 */
	public UnsubscribeMessage(int messageId, String[] topics) {
		this(messageId, stringsToUtf8(topics));
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
		return buffer.getShort(fixedHeaderEndOffset) & 0xffff;
	}

	/**
	 * Sets the message ID
	 */
	public void setMessageId(int messageId) {
		buffer.putShort(fixedHeaderEndOffset, (short) messageId);
	}

	/**
	 * The topics to unsubscribe from.
	 */
	public String[] getTopics() {

		if (topics == null) {
			loadTopics();
		}

		return topics;
	}

	private void loadTopics() {

		buffer.position(fixedHeaderEndOffset + 2);

		int count = 0;
		while (buffer.hasRemaining()) {
			int size = (buffer.getShort() & 0xffff) + 2;
			buffer.position(buffer.position() + size);
			count++;
		}

		topics = new String[count];

		int i = 0;
		buffer.position(fixedHeaderEndOffset + 2);
		while (buffer.hasRemaining()) {
			topics[i] = getString();
			i++;
		}
	}

	private UnsubscribeMessage(int messageId, byte[][] topicsUtf8) {
		super(MessageType.UNSUBSCRIBE, false, QoS.AT_LEAST_ONCE, false, 2 + mqttStringSize(topicsUtf8));

		buffer.putShort((short) messageId);

		for (byte[] utf8 : topicsUtf8) {
			putString(utf8);
		}

		buffer.flip();
	}
}
