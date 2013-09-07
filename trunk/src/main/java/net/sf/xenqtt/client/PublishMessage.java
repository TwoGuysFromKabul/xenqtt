package net.sf.xenqtt.client;

import net.sf.xenqtt.message.QoS;

/**
 * A message published to this client will implement this interface
 */
public interface PublishMessage {

	/**
	 * @return The topic the message was published to. When received by a client that subscribed using wildcard characters, this string will be the absolute
	 *         topic specified by the originating publisher and not the subscription string used by the client. This will never contain wildcards.
	 */
	String getTopic();

	/**
	 * @return The message's payload as a byte[]
	 */
	byte[] getPayload();

	/**
	 * @return The message's payload as a string. The payload is converted to a string using the UTF8 character set.
	 */
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
	 */
	boolean isRetain();

	/**
	 * @return True if the broker is re-delivering the message and the message's QoS is not {@link QoS#AT_MOST_ONCE}. The recipient should treat this flag as a
	 *         hint as to whether the message may have been previously received. It should not be relied on to detect duplicates.
	 */
	boolean isDuplicate();

	/**
	 * @return The level of assurance for delivery
	 */
	QoS getQoS();

	/**
	 * Sends an acknowledgment to the broker for this message unless {@link #getQoS()} is {@link QoS#AT_MOST_ONCE} in which case this does nothing. This method
	 * is always asynchronous.
	 */
	void ack();
}
