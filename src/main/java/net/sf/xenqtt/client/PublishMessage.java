/**
    Copyright 2013 James McClure

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package net.sf.xenqtt.client;

import java.nio.charset.Charset;

import net.sf.xenqtt.XenqttUtil;
import net.sf.xenqtt.message.ChannelManager;
import net.sf.xenqtt.message.MqttChannelRef;
import net.sf.xenqtt.message.MqttMessage;
import net.sf.xenqtt.message.PubAckMessage;
import net.sf.xenqtt.message.PubMessage;
import net.sf.xenqtt.message.QoS;

/**
 * Message published either by the client to a topic or published to the client from subscribed topic. Users may extend this class to add user defined data.
 * This can be especially useful when using the {@link AsyncMqttClient}.
 */
public class PublishMessage {

	private static final Charset UTF8 = Charset.forName("UTF-8");

	private final ChannelManager manager;
	private final MqttChannelRef channel;
	private final PubMessage pubMessage;

	/**
	 * Creates a binary message.
	 * 
	 * @param topicName
	 *            The name of the topic to publish to. This may not contain wildcards ('+' and '#')
	 * @param qos
	 *            The level of assurance for delivery.
	 * @param payload
	 *            The payload as a byte array. It is valid to publish a zero length payload.
	 * @param retain
	 *            If the Retain flag is set (1), the broker should hold on to the message after it has been delivered to the current subscribers. This is useful
	 *            where publishers send messages on a "report by exception" basis, where it might be some time between messages. This allows new subscribers to
	 *            instantly receive data with the retained, or Last Known Good, value. A broker may delete a retained message if it receives a message with a
	 *            zero-length payload and the Retain flag set on the same topic.
	 */
	public PublishMessage(String topicName, QoS qos, byte[] payload, boolean retain) {
		XenqttUtil.validateNotNull("topicName", topicName);
		XenqttUtil.validateNotNull("qos", qos);
		XenqttUtil.validateNotNull("payload", payload);

		this.channel = null;
		this.manager = null;
		this.pubMessage = new PubMessage(qos, retain, topicName, 0, payload);
	}

	/**
	 * Creates a binary message with retain set to false. Delegates to {@link #publish(String, QoS, byte[], boolean)}.
	 * 
	 * @see PublishMessage#PublishMessage(String, QoS, byte[], boolean)
	 */
	public PublishMessage(String topicName, QoS qos, byte[] payload) {
		this(topicName, qos, payload, false);
	}

	/**
	 * Creates a message with a string as the payload with retain set to false. The string is converted to a byte[] using UTF8 encoding and used as the binary
	 * message payload. Delegates to {@link #PublishMessage(String, QoS, byte[], boolean)}.
	 * 
	 * @see PublishMessage#PublishMessage(String, QoS, byte[], boolean)
	 */
	public PublishMessage(String topicName, QoS qos, String payload) {
		this(topicName, qos, payload, false);
	}

	/**
	 * Creates a message with a string as the payload. The string is converted to a byte[] using UTF8 encoding and used as the binary message payload. Delegates
	 * to {@link #PublishMessage(String, QoS, byte[], boolean)}.
	 * 
	 * @see PublishMessage#PublishMessage(String, QoS, byte[], boolean)
	 */
	public PublishMessage(String topicName, QoS qos, String payload, boolean retain) {
		this(topicName, qos, payload.getBytes(UTF8), retain);
	}

	/**
	 * Package visible as this is only for internal use
	 */
	PublishMessage(ChannelManager manager, MqttChannelRef channel, PubMessage pubMessage) {
		this.manager = manager;
		this.channel = channel;
		this.pubMessage = new PubMessage(pubMessage);
	}

	/**
	 * Package visible as this is only for internal use
	 * 
	 * @return A new {@link PubMessage} that is a copy of the internal one.
	 */
	PubMessage getPubMessage() {
		return new PubMessage(pubMessage);
	}

	/**
	 * @return The topic the message was published to. When received by a client that subscribed using wildcard characters, this string will be the absolute
	 *         topic specified by the originating publisher and not the subscription string used by the client. This will never contain wildcards.
	 */
	public final String getTopic() {
		return pubMessage.getTopicName();
	}

	/**
	 * @return The message's payload as a byte[]
	 */
	public final byte[] getPayload() {
		return pubMessage.getPayload();
	}

	/**
	 * @return The message's payload as a string. The payload is converted to a string using the UTF8 character set.
	 */
	public final String getPayloadString() {
		return new String(pubMessage.getPayload(), UTF8);
	}

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
	public final boolean isRetain() {
		return pubMessage.isRetain();
	}

	/**
	 * @return True if the broker is re-delivering the message and the message's QoS is not {@link QoS#AT_MOST_ONCE}. The recipient should treat this flag as a
	 *         hint as to whether the message may have been previously received. It should not be relied on to detect duplicates.
	 */
	public final boolean isDuplicate() {
		return pubMessage.isDuplicate();
	}

	/**
	 * @return The level of assurance for delivery
	 */
	public final QoS getQoS() {
		return pubMessage.getQoS();
	}

	/**
	 * Sends an acknowledgment to the broker for this message unless {@link #getQoS()} is {@link QoS#AT_MOST_ONCE} in which case this does nothing. This method
	 * is always asynchronous. If called on an instance created by the client to send to the broker this method does nothing.
	 */
	public final void ack() {

		if (manager != null && pubMessage.getQoSLevel() > 0) {
			manager.send(channel, new PubAckMessage(pubMessage.getMessageId()));
		}
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		return "PublishMessage [topic=" + pubMessage.getTopicName() + ", QoS=" + pubMessage.getQoS() + ", duplicate=" + pubMessage.isDuplicate() + ", retain="
				+ pubMessage.isRetain() + ", Payload=" + MqttMessage.bytesToHex(pubMessage.getPayload()) + "]";
	}
}
