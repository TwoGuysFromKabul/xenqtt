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

import net.sf.xenqtt.message.QoS;

/**
 * Statistics for the MQTT client
 */
public interface MessageStats {

	/**
	 * @return The number of messages queued to send. These messages are waiting to be sent. This stat cannot be reset
	 */
	long getMessagesQueuedToSend();

	/**
	 * @return The number of messages currently in-flight. An in-flight message is a message with a QOS other than {@link QoS#AT_MOST_ONCE} that has been sent
	 *         to the broker but that has not been acked by the broker. This stat cannot be reset
	 */
	long getMessagesInFlight();

	/**
	 * @return The number of messages that have been sent to the broker. This does not include the {@link #getMessagesResent() resent} messages. This stat can
	 *         be reset
	 */
	long getMessagesSent();

	/**
	 * @return The number of messages that have been resent. This happens when an ack is not received from the broker in a timely manner. This stat can be reset
	 */
	long getMessagesResent();

	/**
	 * @return The number of messages that have been received from the broker. This does not include the {@link #getDuplicateMessagesReceived() duplicate}
	 *         messages received. This stat can be reset
	 */
	long getMessagesReceived();

	/**
	 * @return The number of duplicate messages that have been received. This stat can be reset
	 */
	long getDuplicateMessagesReceived();

	/**
	 * @return Minimum milliseconds it takes to receive and ack from the broker for {@link QoS#AT_LEAST_ONCE} ({@link QoS#EXACTLY_ONCE not supported by the
	 *         client}). This stat can be reset
	 */
	long getMinAckLatencyMillis();

	/**
	 * @return Max milliseconds it takes to receive and ack from the broker for {@link QoS#AT_LEAST_ONCE} ({@link QoS#EXACTLY_ONCE not supported by the client}
	 *         ). This stat can be reset
	 */
	long getMaxAckLatencyMillis();

	/**
	 * @return Average milliseconds it takes to receive and ack from the broker for {@link QoS#AT_LEAST_ONCE} ({@link QoS#EXACTLY_ONCE not supported by the
	 *         client}). This stat can be reset
	 */
	double getAverageAckLatencyMillis();
}
