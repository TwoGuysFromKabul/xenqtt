package net.sf.xenqtt.message;

import java.nio.ByteBuffer;

/**
 * The CONNACK message is the message sent by the server in response to a CONNECT request from a client.
 */
public final class ConnAckMessage extends MqttMessage {

	/**
	 * Used to construct a received message.
	 */
	public ConnAckMessage(ByteBuffer buffer, int remainingLength) {
		super(buffer, remainingLength);
	}

	/**
	 * Used to construct a message for sending
	 */
	public ConnAckMessage(ConnectReturnCode returnCode) {
		super(MessageType.CONNACK, 2);

		buffer.put((byte) 0); // Topic Name Compression Response.Reserved values. Not used.
		buffer.put((byte) returnCode.value());
		buffer.flip();
	}

	/**
	 * @return The resulting status of the connect attempt
	 */
	public ConnectReturnCode getReturnCode() {
		return ConnectReturnCode.lookup(buffer.get(3) & 0xff);
	}

	/**
	 * Sets the return code
	 */
	public void setReturnCode(ConnectReturnCode returnCode) {
		buffer.put(3, (byte) returnCode.value());
	}
}
