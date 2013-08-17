package net.sf.xenqtt.message;

import java.nio.ByteBuffer;

/**
 * The DISCONNECT message is sent from the client to the server to indicate that it is about to close its TCP/IP connection. This allows for a clean
 * disconnection, rather than just dropping the line.
 * <p>
 * If the client had connected with the clean session flag set, then all previously maintained information about the client will be discarded.
 * <p>
 * A server should not rely on the client to close the TCP/IP connection after receiving a DISCONNECT.
 */
public final class DisconnectMessage extends MqttMessage {

	/**
	 * Used to construct a received message.
	 */
	public DisconnectMessage(ByteBuffer buffer) {
		super(buffer, 0);
	}

	/**
	 * Used to construct a message for sending
	 */
	public DisconnectMessage() {
		super(MessageType.DISCONNECT, 0);
		buffer.flip();
	}
}
