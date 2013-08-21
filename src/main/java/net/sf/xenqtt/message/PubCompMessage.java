package net.sf.xenqtt.message;

import java.nio.ByteBuffer;

/**
 * This message is either the response from the server to a PUBREL message from a publisher, or the response from a subscriber to a PUBREL message from the
 * server. It is the fourth and last message in the QoS 2 protocol flow.
 */
public final class PubCompMessage extends MqttMessageWithId {

	/**
	 * Used to construct a received message.
	 */
	public PubCompMessage(ByteBuffer buffer) {
		super(buffer, 2);
	}

	/**
	 * Used to construct a message for sending
	 */
	public PubCompMessage(int messageId) {
		super(MessageType.PUBCOMP, 2);
		buffer.putShort((short) messageId);
		buffer.flip();
	}
}
