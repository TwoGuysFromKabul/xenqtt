package net.sf.xenqtt.message;

import java.nio.ByteBuffer;

/**
 * The UNSUBACK message is sent by the server to the client to confirm receipt of an UNSUBSCRIBE message.
 */
public final class UnsubAckMessage extends MqttMessage implements MqttMessageWithId {

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
	 * @see net.sf.xenqtt.message.MqttMessageWithId#getMessageId()
	 */
	@Override
	public int getMessageId() {
		return buffer.getShort(2) & 0xffff;
	}

	/**
	 * @see net.sf.xenqtt.message.MqttMessageWithId#setMessageId(int)
	 */
	@Override
	public void setMessageId(int messageId) {
		buffer.putShort(2, (short) messageId);
	}
}
