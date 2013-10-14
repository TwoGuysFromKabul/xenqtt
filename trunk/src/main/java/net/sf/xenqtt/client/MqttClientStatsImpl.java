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

import java.util.Set;

import net.sf.xenqtt.Log;
import net.sf.xenqtt.message.MqttChannel;

public class MqttClientStatsImpl implements MutableMqttClientStats {

	private final Set<MqttChannel> registeredChannels;
	private final long messagesQueuedToSend;
	private final long messagesInFlight;
	private final MessageStat messagesSent;
	private final MessageStat messagesReceived;
	private final LatencyStat ackLatency;

	public MqttClientStatsImpl(Set<MqttChannel> registeredChannels) {
		this.registeredChannels = registeredChannels;
		messagesQueuedToSend = 0;
		messagesInFlight = 0;
		messagesSent = new MessageStat();
		messagesReceived = new MessageStat();
		ackLatency = new LatencyStat();
	}

	/**
	 * @see net.sf.xenqtt.client.MqttClientStats#getMessagesQueuedToSend()
	 */
	@Override
	public long getMessagesQueuedToSend() {
		return messagesQueuedToSend;
	}

	/**
	 * @see net.sf.xenqtt.client.MqttClientStats#getMessagesInFlight()
	 */
	@Override
	public long getMessagesInFlight() {
		return messagesInFlight;
	}

	/**
	 * @see net.sf.xenqtt.client.MqttClientStats#getMessagesSent()
	 */
	@Override
	public long getMessagesSent() {
		return messagesSent.value;
	}

	/**
	 * @see net.sf.xenqtt.client.MqttClientStats#getMessagesResent()
	 */
	@Override
	public long getMessagesResent() {
		return messagesSent.resendOrDup;
	}

	/**
	 * @see net.sf.xenqtt.client.MqttClientStats#getMessagesReceived()
	 */
	@Override
	public long getMessagesReceived() {
		return messagesReceived.value;
	}

	/**
	 * @see net.sf.xenqtt.client.MqttClientStats#getDuplicateMessagesReceived()
	 */
	@Override
	public long getDuplicateMessagesReceived() {
		return messagesReceived.resendOrDup;
	}

	/**
	 * @see net.sf.xenqtt.client.MqttClientStats#getMinAckLatencyMillis()
	 */
	@Override
	public long getMinAckLatencyMillis() {
		return ackLatency.min;
	}

	/**
	 * @see net.sf.xenqtt.client.MqttClientStats#getMaxAckLatencyMillis()
	 */
	@Override
	public long getMaxAckLatencyMillis() {
		return ackLatency.max;
	}

	/**
	 * @see net.sf.xenqtt.client.MqttClientStats#getAverageAckLatencyMillis()
	 */
	@Override
	public double getAverageAckLatencyMillis() {
		return ackLatency.average();
	}

	/**
	 * @see net.sf.xenqtt.client.MutableMqttClientStats#messageSent(boolean)
	 */
	@Override
	public void messageSent(boolean resent) {
		messagesSent.messageInteraction(resent);
	}

	/**
	 * @see net.sf.xenqtt.client.MutableMqttClientStats#messageAcked(long)
	 */
	@Override
	public void messageAcked(long ackLatency) {
		this.ackLatency.processLatency(ackLatency);
	}

	/**
	 * @see net.sf.xenqtt.client.MutableMqttClientStats#messageReceived(boolean)
	 */
	@Override
	public void messageReceived(boolean duplicate) {
		messagesReceived.messageInteraction(duplicate);
	}

	/**
	 * @see net.sf.xenqtt.client.MutableMqttClientStats#reset()
	 */
	@Override
	public void reset() {
		messagesSent.reset();
		messagesReceived.reset();
		ackLatency.reset();
	}

	/**
	 * Returns a clone of this {@link MqttClientStatsImpl stats} instance. This method is invoked when a stats snapshot is requested. The clone is a deep copy.
	 * 
	 * @return A deep copy of this instance
	 * 
	 * @see java.lang.Object#clone()
	 */
	@Override
	public MqttClientStatsImpl clone() {
		long queuedToSend = getQueuedToSend();
		long inFlight = getInFlight();

		try {
			return new MqttClientStatsImpl(queuedToSend, inFlight, messagesSent.clone(), messagesReceived.clone(), ackLatency.clone());
		} catch (Exception ex) {
			Log.error(ex, "Unable to get the statistics snapshot");
			return null;
		}
	}

	private long getQueuedToSend() {
		if (registeredChannels == null) {
			return 0;
		}

		long queuedForSend = 0;
		for (MqttChannel channel : registeredChannels) {
			queuedForSend += channel.sendQueueDepth();
		}

		return queuedForSend;
	}

	private long getInFlight() {
		if (registeredChannels == null) {
			return 0;
		}

		long inFlight = 0;
		for (MqttChannel channel : registeredChannels) {
			inFlight += channel.inFlightMessageCount();
		}

		return inFlight;
	}

	/**
	 * Create a new instance of this class. This constructor is used when making a deep copy of this {@link MqttClientStatsImpl stats} instance.
	 * 
	 * @param messagesQueuedToSend
	 *            The messages currently queued for sending at the time of construction
	 * @param messagesInFlight
	 *            The messages in-flight at the time of construction
	 * @param messagesReceived
	 *            The messages received at the time of construction along with any resends
	 * @param messagesQueued
	 *            The messages that have been queued at the time of construction along with duplicates
	 * @param processQueueLatency
	 *            The latency of the process queue
	 * @param sendLatency
	 *            The latency of sending messages
	 * @param ackLatency
	 *            The latency around acks
	 */
	private MqttClientStatsImpl(long messagesQueuedToSend, long messagesInFlight, MessageStat messagesSent, MessageStat messagesReceived, LatencyStat ackLatency) {
		this.messagesQueuedToSend = messagesQueuedToSend;
		this.messagesInFlight = messagesInFlight;
		this.messagesSent = messagesSent;
		this.messagesReceived = messagesReceived;
		this.ackLatency = ackLatency;
		registeredChannels = null;
	}

	private static final class MessageStat implements Cloneable {

		private long value;
		private long resendOrDup;

		private void messageInteraction(boolean resendOrDup) {
			value++;
			if (resendOrDup) {
				this.resendOrDup++;
			}
		}

		private void reset() {
			value = resendOrDup = 0;
		}

		@Override
		public MessageStat clone() throws CloneNotSupportedException {
			return (MessageStat) super.clone();
		}

	}

	// TODO [jeremy] - Make this external and change the interface methods to return an instance of this.
	private static final class LatencyStat implements Cloneable {

		private long count;
		private long sum;
		private long min;
		private long max;

		private void processLatency(long latency) {
			count++;
			sum += latency;
			if (min == 0 || latency < min) {
				min = latency;
			}

			if (max == 0 || latency > max) {
				max = latency;
			}
		}

		private double average() {
			if (count == 0) {
				return 0.0;
			}

			return (sum * 1.0) / count;
		}

		private void reset() {
			count = sum = min = max = 0;
		}

		@Override
		public LatencyStat clone() throws CloneNotSupportedException {
			return (LatencyStat) super.clone();
		}

	}

}
