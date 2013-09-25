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
 * A SUBACK message is sent by the server to the client to confirm receipt of a SUBSCRIBE message.
 * <p>
 * A SUBACK message contains a list of granted QoS levels. The order of granted QoS levels in the SUBACK message matches the order of the topic names in the
 * corresponding SUBSCRIBE message.
 */
public final class SubAckMessage extends IdentifiableMqttMessage {

	/**
	 * Used to construct a received message.
	 */
	public SubAckMessage(ByteBuffer buffer, int remainingLength) {
		super(buffer, remainingLength);
	}

	/**
	 * Used to construct a message for sending
	 */
	public SubAckMessage(int messageId, QoS[] grantedQoses) {
		super(MessageType.SUBACK, 2 + grantedQoses.length);
		buffer.putShort((short) messageId);
		for (QoS qos : grantedQoses) {
			buffer.put((byte) qos.value());
		}
		buffer.flip();
	}

	/**
	 * The Message Identifier (Message ID) for the SUBSCRIBE message that is being acknowledged.
	 * 
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
	 * Granted QoS levels. Each level corresponds to a topic name in the corresponding SUBSCRIBE message. The order of QoS levels in the SUBACK message matches
	 * the order of topic name and Requested QoS pairs in the SUBSCRIBE message. The Message ID in the variable header enables you to match SUBACK messages with
	 * the corresponding SUBSCRIBE messages.
	 */
	public QoS[] getGrantedQoses() {

		QoS[] qoses = new QoS[getRemainingLength() - 2];
		for (int i = 0; i < qoses.length; i++) {
			qoses[i] = QoS.lookup(buffer.get(fixedHeaderEndOffset + 2 + i) & 0xff);
		}

		return qoses;
	}
}
