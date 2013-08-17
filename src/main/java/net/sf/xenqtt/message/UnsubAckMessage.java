package net.sf.xenqtt.message;

import java.nio.ByteBuffer;

/**
 * The UNSUBACK message is sent by the server to the client to confirm receipt of an UNSUBSCRIBE message.
 */
public final class UnsubAckMessage extends MqttMessage {

	/**
	 * Used to construct a received message.
	 */
	public UnsubAckMessage(ByteBuffer buffer) {
		super(buffer, 2);
	}

	/**
	 * Used to construct a message for sending
	 */
	public UnsubAckMessage(int messageId) {
		super(MessageType.UNSUBACK, 2);
		buffer.putShort((short) messageId);
		buffer.flip();
	}

	/**
	 * The Message Identifier (Message ID) for the UNSUBSCRIBE message that is being acknowledged.
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
