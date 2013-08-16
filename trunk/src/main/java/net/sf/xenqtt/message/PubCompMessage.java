package net.sf.xenqtt.message;

import java.nio.ByteBuffer;

/**
 * This message is either the response from the server to a PUBREL message from a publisher, or the response from a subscriber to a PUBREL message from the
 * server. It is the fourth and last message in the QoS 2 protocol flow.
 */
public final class PubCompMessage extends MqttMessage {

	/**
	 * Used to construct a received message.
	 */
	public PubCompMessage(ByteBuffer buffer, int remainingLength) {
		super(buffer, remainingLength);
	}

	/**
	 * Used to construct a message for sending
	 */
	public PubCompMessage(int messageId) {
		super(MessageType.PUBCOMP, 2);
		buffer.putShort((short) messageId);
	}

	/**
	 * The same Message ID as the acknowledged PUBREL message.
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
