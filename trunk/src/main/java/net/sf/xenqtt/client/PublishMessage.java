package net.sf.xenqtt.client;

import net.sf.xenqtt.message.QoS;

public interface PublishMessage {

	String getTopic();

	byte[] getPayload();

	String getPayloadString();

	/**
	 * If the Retain flag is set (1), the server should hold on to the message after it has been delivered to the current subscribers.
	 * <p>
	 * When a new subscription is established on a topic, the last retained message on that topic should be sent to the subscriber with the Retain flag set. If
	 * there is no retained message, nothing is sent
	 * <p>
	 * This is useful where publishers send messages on a "report by exception" basis, where it might be some time between messages. This allows new subscribers
	 * to instantly receive data with the retained, or Last Known Good, value.
	 * <p>
	 * When a server sends a PUBLISH to a client as a result of a subscription that already existed when the original PUBLISH arrived, the Retain flag should
	 * not be set, regardless of the Retain flag of the original PUBLISH. This allows a client to distinguish messages that are being received because they were
	 * retained and those that are being received "live".
	 * <p>
	 * Retained messages should be kept over restarts of the server.
	 * <p>
	 * A server may delete a retained message if it receives a message with a zero-length payload and the Retain flag set on the same topic.
	 * 
	 * @return
	 */
	boolean isRetain();

	boolean isDuplicate();

	QoS getQoS();

	int getQoSLevel();

	void ack();
}
