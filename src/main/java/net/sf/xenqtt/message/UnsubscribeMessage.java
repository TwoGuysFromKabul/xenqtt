package net.sf.xenqtt.message;

import java.nio.ByteBuffer;

/**
 * An UNSUBSCRIBE message is sent by the client to the server to unsubscribe from named topics.
 * <p>
 * The server sends an UNSUBACK to a client in response to an UNSUBSCRIBE message.
 */
public final class UnsubscribeMessage extends MqttMessageWithId {

	private String[] topics;

	/**
	 * Used to construct a received message.
	 */
	public UnsubscribeMessage(ByteBuffer buffer, int remainingLength) {
		super(buffer, remainingLength);
	}

	/**
	 * Used to construct a message for sending
	 */
	public UnsubscribeMessage(int messageId, String[] topics) {
		this(messageId, stringsToUtf8(topics));
	}

	/**
	 * @see net.sf.xenqtt.message.MqttMessageWithId#getMessageId()
	 */
	@Override
	public int getMessageId() {
		return buffer.getShort(fixedHeaderEndOffset) & 0xffff;
	}

	/**
	 * @see net.sf.xenqtt.message.MqttMessageWithId#setMessageId(int)
	 */
	@Override
	public void setMessageId(int messageId) {
		buffer.putShort(fixedHeaderEndOffset, (short) messageId);
	}

	/**
	 * The topics to unsubscribe from.
	 */
	public String[] getTopics() {

		if (topics == null) {
			loadTopics();
		}

		return topics;
	}

	private void loadTopics() {

		buffer.position(fixedHeaderEndOffset + 2);

		int count = 0;
		while (buffer.hasRemaining()) {
			int size = (buffer.getShort() & 0xffff) + 2;
			buffer.position(buffer.position() + size);
			count++;
		}

		topics = new String[count];

		int i = 0;
		buffer.position(fixedHeaderEndOffset + 2);
		while (buffer.hasRemaining()) {
			topics[i] = getString();
			i++;
		}
	}

	private UnsubscribeMessage(int messageId, byte[][] topicsUtf8) {
		super(MessageType.UNSUBSCRIBE, false, QoS.AT_LEAST_ONCE, false, 2 + mqttStringSize(topicsUtf8));

		buffer.putShort((short) messageId);

		for (byte[] utf8 : topicsUtf8) {
			putString(utf8);
		}

		buffer.flip();
	}
}
