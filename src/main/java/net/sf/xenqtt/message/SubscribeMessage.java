package net.sf.xenqtt.message;

import java.nio.ByteBuffer;

/**
 * The SUBSCRIBE message allows a client to register an interest in one or more topic names with the server. Messages published to these topics are delivered
 * from the server to the client as PUBLISH messages. The SUBSCRIBE message also specifies the QoS level at which the subscriber wants to receive published
 * messages.
 * <p>
 * Assuming that the requested QoS level is granted, the client receives PUBLISH messages at less than or equal to this level, depending on the QoS level of the
 * original message from the publisher. For example, if a client has a QoS level 1 subscription to a particular topic, then a QoS level 0 PUBLISH message to
 * that topic is delivered to the client at QoS level 0. A QoS level 2 PUBLISH message to the same topic is downgraded to QoS level 1 for delivery to the
 * client.
 * <p>
 * A corollary to this is that subscribing to a topic at QoS level 2 is equivalent to saying
 * "I would like to receive messages on this topic at the QoS at which they are published".
 * <p>
 * This means a publisher is responsible for determining the maximum QoS a message can be delivered at, but a subscriber is able to downgrade the QoS to one
 * more suitable for its usage. The QoS of a message is never upgraded.
 * <p>
 * When it receives a SUBSCRIBE message from a client, the server responds with a SUBACK message.
 * <p>
 * A server may start sending PUBLISH messages due to the subscription before the client receives the SUBACK message.
 * <p>
 * Note that if a server implementation does not authorize a SUBSCRIBE request to be made by a client, it has no way of informing that client. It must therefore
 * make a positive acknowledgement with a SUBACK, and the client will not be informed that it was not authorized to subscribe.
 * <p>
 * A server may chose to grant a lower level of QoS than the client requested. This could happen if the server is not able to provide the higher levels of QoS.
 * For example, if the server does not provider a reliable persistence mechanism it may chose to only grant subscriptions at QoS 0.
 */
public final class SubscribeMessage extends MqttMessageWithId {

	private String[] topics;
	private QoS[] qoses;

	/**
	 * Used to construct a received message.
	 */
	public SubscribeMessage(ByteBuffer buffer, int remainingLength) {
		super(buffer, remainingLength);
	}

	/**
	 * Used to construct a message for sending
	 */
	public SubscribeMessage(int messageId, String[] topics, QoS[] requestedQoses) {
		this(messageId, stringsToUtf8(topics), requestedQoses);
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
	 * The topics to subscribe to. The topic strings may contain special Topic wildcard characters to represent a set of topics.
	 */
	public String[] getTopics() {

		if (topics == null) {
			loadTopicsAndQoses();
		}

		return topics;
	}

	/**
	 * The requested QoS's for the topics subscribed to. Each entry in this array applies to the topic at the same index in the array returned by
	 * {@link #getTopics()}.
	 */
	public QoS[] getRequestedQoSes() {

		if (qoses == null) {
			loadTopicsAndQoses();
		}

		return qoses;
	}

	private void loadTopicsAndQoses() {

		buffer.position(fixedHeaderEndOffset + 2);

		int count = 0;
		while (buffer.hasRemaining()) {
			int size = (buffer.getShort() & 0xffff) + 3;
			buffer.position(buffer.position() + size);
			count++;
		}

		topics = new String[count];
		qoses = new QoS[count];

		int i = 0;
		buffer.position(fixedHeaderEndOffset + 2);
		while (buffer.hasRemaining()) {
			topics[i] = getString();
			qoses[i] = QoS.lookup(buffer.get() & 0xff);
			i++;
		}
	}

	private SubscribeMessage(int messageId, byte[][] topicsUtf8, QoS[] reqeustedQoses) {
		super(MessageType.SUBSCRIBE, false, QoS.AT_LEAST_ONCE, false, 2 + mqttStringSize(topicsUtf8) + reqeustedQoses.length);

		if (topicsUtf8.length != reqeustedQoses.length) {
			throw new IllegalArgumentException("There must be exactly 1 requested QoS per topic");
		}

		buffer.putShort((short) messageId);

		for (int i = 0; i < reqeustedQoses.length; i++) {
			putString(topicsUtf8[i]);
			buffer.put((byte) reqeustedQoses[i].value());
		}

		buffer.flip();
	}
}
