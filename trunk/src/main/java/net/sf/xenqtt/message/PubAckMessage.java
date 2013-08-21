package net.sf.xenqtt.message;

import java.nio.ByteBuffer;

/**
 * A PUBACK message is the response to a PUBLISH message with QoS level 1. A PUBACK message is sent by a server in response to a PUBLISH message from a
 * publishing client, and by a subscriber in response to a PUBLISH message from the server.
 */
public final class PubAckMessage extends IdentifiableMqttMessage {

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
}
