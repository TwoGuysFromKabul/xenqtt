package net.sf.xenqtt.message;

import java.nio.ByteBuffer;

/**
 * A PUBACK message is the response to a PUBLISH message with QoS level 1. A PUBACK message is sent by a server in response to a PUBLISH message from a
 * publishing client, and by a subscriber in response to a PUBLISH message from the server.
 */
public final class PubAckMessage extends MqttMessage implements MqttMessageWithId {

	/**
	 * Used to construct a received message.
	 */
	public PubAckMessage(ByteBuffer buffer) {
		super(buffer, 2);
	}

	/**
	 * Used to construct a message for sending
	 */
	public PubAckMessage(int messageId) {
		super(MessageType.PUBACK, 2);
		buffer.putShort((short) messageId);
		buffer.flip();
	}

	/**
	 * The Message Identifier (Message ID) for the PUBLISH message that is being acknowledged.
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
