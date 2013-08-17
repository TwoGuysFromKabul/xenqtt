package net.sf.xenqtt.message;

import java.nio.ByteBuffer;

/**
 * A PINGRESP message is the response sent by a server to a PINGREQ message and means "yes I am alive".
 */
public final class PingRespMessage extends MqttMessage {

	/**
	 * Used to construct a received message.
	 */
	public PingRespMessage(ByteBuffer buffer) {
		super(buffer, 0);
	}

	/**
	 * Used to construct a message for sending
	 */
	public PingRespMessage() {
		super(MessageType.PINGRESP, 0);
		buffer.flip();
	}
}
