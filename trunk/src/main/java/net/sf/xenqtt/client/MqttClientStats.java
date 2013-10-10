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
public interface MqttClientStats {

	/**
	 * @return The number of messages queued to send. These messages are waiting to be sent.
	 */
	long getMessagesQueuedToSend();

	/**
	 * @return The number of messages currently in-flight. An in-flight message is a message with a QOS other than {@link QoS#AT_MOST_ONCE} that has been sent
	 *         to the broker but that has not been acked by the broker.
	 */
	long getMessagesInFlight();

	/**
	 * @return The number of messages that have been sent to the broker. This does not include the {@link #getMessagesResent() resent} messages.
	 */
	long getMessagesSent();

	/**
	 * @return The number of messages that have been resent. This happens when an ack is not received from the broker in a timely manner.
	 */
	long getMessagesResent();

	/**
	 * @return The number of messages that have been received from the broker. This does not include the {@link #getDuplicateMessagesReceived() duplicate}
	 *         messages received.
	 */
	long getMessagesReceived();

	/**
	 * @return The number of duplicate messages that have been received.
	 */
	long getDuplicateMessagesReceived();

	/**
	 * @return The number of messages that have been received and are queued for processing by the application.
	 */
	long getMessagesQueuedToProcess();

	/**
	 * @return Minimum milliseconds it takes to start processing a received message from the time it is received.
	 */
	long getMinProcessQueueLatencyMillis();

	/**
	 * @return Maximum milliseconds it takes to start processing a received message from the time it is received.
	 */
	long getMaxProcessQueueLatencyMillis();

	/**
	 * @return Average milliseconds it takes to start processing a received message from the time it is received.
	 */
	double getAverageProcessQueueLatencyMillis();

	/**
	 * @return Minimum milliseconds it takes to completely send a message from the time it is queued for sending
	 */
	long getMinSendLatencyMillis();

	/**
	 * @return Maximum milliseconds it takes to completely send a message from the time it is queued for sending
	 */
	long getMaxSendLatencyMillis();

	/**
	 * @return Average milliseconds it takes to completely send a message from the time it is queued for sending
	 */
	double getAverageSendLatencyMillis();

	/**
	 * @return Minimum milliseconds it takes to receive and ack from the broker for {@link QoS#AT_LEAST_ONCE} ({@link QoS#EXACTLY_ONCE not supported by the
	 *         client})
	 */
	long getMinAckLatencyMillis();

	/**
	 * @return Max milliseconds it takes to receive and ack from the broker for {@link QoS#AT_LEAST_ONCE} ({@link QoS#EXACTLY_ONCE not supported by the client})
	 */
	long getMaxAckLatencyMillis();

	/**
	 * @return Average milliseconds it takes to receive and ack from the broker for {@link QoS#AT_LEAST_ONCE} ({@link QoS#EXACTLY_ONCE not supported by the
	 *         client})
	 */
	double getAverageAckLatencyMillis();
}
