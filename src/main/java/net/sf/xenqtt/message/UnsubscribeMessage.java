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
 * An UNSUBSCRIBE message is sent by the client to the server to unsubscribe from named topics.
 * <p>
 * The server sends an UNSUBACK to a client in response to an UNSUBSCRIBE message.
 */
public final class UnsubscribeMessage extends IdentifiableMqttMessage {

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
	 * @see net.sf.xenqtt.message.IdentifiableMqttMessage#getMessageId()
	 */
	@Override
	public int getMessageId() {
		return buffer.getShort(fixedHeaderEndOffset) & 0xffff;
	}

	/**
	 * @see net.sf.xenqtt.message.IdentifiableMqttMessage#setMessageId(int)
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

		int index = fixedHeaderEndOffset + 2;

		int count = 0;
		while (index < buffer.limit()) {
			int size = (buffer.getShort(index) & 0xffff) + 2;
			index += size;
			count++;
		}

		topics = new String[count];

		int i = 0;
		index = fixedHeaderEndOffset + 2;
		while (index < buffer.limit()) {
			int len = buffer.getShort(index) & 0xffff;
			index += 2;
			topics[i] = new String(getBytes(index, len), UTF8);
			index += len;
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
