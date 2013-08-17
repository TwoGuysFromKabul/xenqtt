package net.sf.xenqtt.message;

import java.nio.ByteBuffer;

/**
 * The PINGREQ message is an "are you alive?" message that is sent from a connected client to the server.
 * <p>
 * The response to a PINGREQ message is a PINGRESP message.
 */
public final class PingReqMessage extends MqttMessage {

	/**
	 * Used to construct a received message.
	 */
	public PingReqMessage(ByteBuffer buffer) {
		super(buffer, 0);
	}

	/**
	 * Used to construct a message for sending
	 */
	public PingReqMessage() {
		super(MessageType.PINGREQ, 0);
		buffer.flip();
	}
}
