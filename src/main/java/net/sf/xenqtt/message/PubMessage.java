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
package net.sf.xenqtt.message;

import java.nio.ByteBuffer;

/**
 * A PUBLISH message is sent by a client to a server for distribution to interested subscribers. Each PUBLISH message is associated with a topic name (also
 * known as the Subject or Channel). This is a hierarchical name space that defines a taxonomy of information sources for which subscribers can register an
 * interest. A message that is published to a specific topic name is delivered to connected subscribers for that topic.
 * <p>
 * If a client subscribes to one or more topics, any message published to those topics are sent by the server to the client as a PUBLISH message.
 */
public final class PubMessage extends IdentifiableMqttMessage {

	private int payloadIndex = -1;

	/**
	 * Used to construct a received message.
	 */
	public PubMessage(ByteBuffer buffer, int remainingLength) {
		super(buffer, remainingLength);
	}

	/**
	 * Used to construct a message for sending
	 */
	public PubMessage(QoS qos, boolean retain, String topicName, int messageId, byte[] payload) {
		this(qos, retain, stringToUtf8(topicName), messageId, payload);
	}

	/**
	 * This must not contain Topic wildcard characters.
	 * <p>
	 * When received by a client that subscribed using wildcard characters, this string will be the absolute topic specified by the originating publisher and
	 * not the subscription string used by the client.
	 */
	public String getTopicName() {
		return getString(fixedHeaderEndOffset);
	}

	/**
	 * @see net.sf.xenqtt.message.IdentifiableMqttMessage#getMessageId()
	 */
	@Override
	public int getMessageId() {
		return getQoSLevel() == 0 ? 0 : buffer.getShort(getPayloadIndex() - 2) & 0xffff;
	}

	/**
	 * @see net.sf.xenqtt.message.IdentifiableMqttMessage#setMessageId(int)
	 */
	@Override
	public void setMessageId(int messageId) {
		if (getQoSLevel() == 0) {
			throw new UnsupportedOperationException("Message ID is not supported at QoS 0 (AT_MOST_ONCE)");
		}
		buffer.putShort(getPayloadIndex() - 2, (short) messageId);
	}

	/**
	 * Contains the data for publishing. The content and format of the data is application specific. It is valid for a PUBLISH to contain a 0-length payload.
	 */
	public byte[] getPayload() {

		int pos = buffer.position();
		buffer.position(getPayloadIndex());
		byte[] payload = new byte[buffer.limit() - buffer.position()];
		buffer.get(payload);
		buffer.position(pos);

		return payload;
	}

	private int getPayloadIndex() {

		if (payloadIndex == -1) {
			payloadIndex = fixedHeaderEndOffset + 2 + (buffer.getShort(fixedHeaderEndOffset) & 0xffff);
			if (getQoSLevel() > 0) {
				payloadIndex += 2;
			}
		}

		return payloadIndex;
	}

	private PubMessage(QoS qos, boolean retain, byte[] topicNameUtf8, int messageId, byte[] payload) {
		super(MessageType.PUBLISH, false, qos, retain, (qos.ordinal() == 0 ? 0 : 2) + mqttStringSize(topicNameUtf8) + payload.length);

		putString(topicNameUtf8);
		if (qos.ordinal() > 0) {
			buffer.putShort((short) messageId);
		}
		buffer.put(payload);
		buffer.flip();
	}
}
