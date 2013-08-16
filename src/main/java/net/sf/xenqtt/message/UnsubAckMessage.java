package net.sf.xenqtt.message;

import java.nio.ByteBuffer;

/**
 * The UNSUBACK message is sent by the server to the client to confirm receipt of an UNSUBSCRIBE message.
 */
public final class UnsubAckMessage extends MqttMessage {

	/**
	 * Used to construct a received message.
	 */
	public UnsubAckMessage(ByteBuffer buffer, int remainingLength) {
		super(buffer, remainingLength);
	}

	// FIXME [jim] - check all buffer offset references to be sure fixedHeaderEndOffset is used instead of a fixed number
	// FIXME [jim] - check all message types to be sure they are correct
	/**
	 * Used to construct a message for sending
	 */
	public UnsubAckMessage(int messageId) {
		super(MessageType.UNSUBACK, 2);
	}

	/**
	 * The Message Identifier (Message ID) for the UNSUBSCRIBE message that is being acknowledged.
	 * 
	 * @see PublishMessage#getMessageId()
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
}
