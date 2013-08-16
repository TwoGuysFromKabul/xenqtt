package net.sf.xenqtt.message;

import java.nio.ByteBuffer;

/**
 * A PUBACK message is the response to a PUBLISH message with QoS level 1. A PUBACK message is sent by a server in response to a PUBLISH message from a
 * publishing client, and by a subscriber in response to a PUBLISH message from the server.
 */
public final class PubAckMessage extends MqttMessage {

	/**
	 * Used to construct a received message.
	 */
	public PubAckMessage(ByteBuffer buffer, int remainingLength) {
		super(buffer, remainingLength);
	}

	/**
	 * Used to construct a message for sending
	 */
	public PubAckMessage(int messageId) {
		super(MessageType.PUBACK, 2);
		buffer.putShort((short) messageId);
	}

	/**
	 * The Message Identifier (Message ID) for the PUBLISH message that is being acknowledged.
	 * 
	 * @see PublishMessage#getMessageId()
	 */
	public int getMessageId() {
		return buffer.getShort(2) & 0xffff;
	}

	/**
	 * Sets the message ID
	 */
	public void setMessageId(int messageId) {
		buffer.putShort(2, (short) messageId);
	}
}
