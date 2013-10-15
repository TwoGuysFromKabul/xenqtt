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

import java.util.Set;

import net.sf.xenqtt.Log;

final class MessageStatsImpl implements MutableMessageStats {

	private final Set<MqttChannel> registeredChannels;
	private final long messagesQueuedToSend;
	private final long messagesInFlight;
	private final MessageStat messagesSent;
	private final MessageStat messagesReceived;
	private final LatencyStatImpl ackLatency;

	MessageStatsImpl(Set<MqttChannel> registeredChannels) {
		this.registeredChannels = registeredChannels;
		messagesQueuedToSend = 0;
		messagesInFlight = 0;
		messagesSent = new MessageStat();
		messagesReceived = new MessageStat();
		ackLatency = new LatencyStatImpl();
	}

	/**
	 * @see net.sf.xenqtt.client.MessageStats#getMessagesQueuedToSend()
	 */
	@Override
	public long getMessagesQueuedToSend() {
		return messagesQueuedToSend;
	}

	/**
	 * @see net.sf.xenqtt.client.MessageStats#getMessagesInFlight()
	 */
	@Override
	public long getMessagesInFlight() {
		return messagesInFlight;
	}

	/**
	 * @see net.sf.xenqtt.client.MessageStats#getMessagesSent()
	 */
	@Override
	public long getMessagesSent() {
		return messagesSent.value;
	}

	/**
	 * @see net.sf.xenqtt.client.MessageStats#getMessagesResent()
	 */
	@Override
	public long getMessagesResent() {
		return messagesSent.resendOrDup;
	}

	/**
	 * @see net.sf.xenqtt.client.MessageStats#getMessagesReceived()
	 */
	@Override
	public long getMessagesReceived() {
		return messagesReceived.value;
	}

	/**
	 * @see net.sf.xenqtt.client.MessageStats#getDuplicateMessagesReceived()
	 */
	@Override
	public long getDuplicateMessagesReceived() {
		return messagesReceived.resendOrDup;
	}

	/**
	 * @see net.sf.xenqtt.client.MessageStats#getMinAckLatencyMillis()
	 */
	@Override
	public long getMinAckLatencyMillis() {
		return ackLatency.getMin();
	}

	/**
	 * @see net.sf.xenqtt.client.MessageStats#getMaxAckLatencyMillis()
	 */
	@Override
	public long getMaxAckLatencyMillis() {
		return ackLatency.getMax();
	}

	/**
	 * @see net.sf.xenqtt.client.MessageStats#getAverageAckLatencyMillis()
	 */
	@Override
	public double getAverageAckLatencyMillis() {
		return ackLatency.getAverage();
	}

	/**
	 * @see net.sf.xenqtt.message.MutableMessageStats#messageSent(boolean)
	 */
	@Override
	public void messageSent(boolean resent) {
		messagesSent.messageInteraction(resent);
	}

	/**
	 * @see net.sf.xenqtt.message.MutableMessageStats#messageAcked(long)
	 */
	@Override
	public void messageAcked(long ackLatency) {
		this.ackLatency.processLatency(ackLatency);
	}

	/**
	 * @see net.sf.xenqtt.message.MutableMessageStats#messageReceived(boolean)
	 */
	@Override
	public void messageReceived(boolean duplicate) {
		messagesReceived.messageInteraction(duplicate);
	}

	/**
	 * @see net.sf.xenqtt.message.MutableMessageStats#reset()
	 */
	@Override
	public void reset() {
		messagesSent.reset();
		messagesReceived.reset();
		ackLatency.reset();
	}

	/**
	 * Returns a clone of this {@link MessageStatsImpl stats} instance. This method is invoked when a stats snapshot is requested. The clone is a deep copy.
	 * 
	 * @return A deep copy of this instance
	 * 
	 * @see java.lang.Object#clone()
	 */
	@Override
	public MessageStatsImpl clone() {
		long queuedToSend = getQueuedToSend();
		long inFlight = getInFlight();

		try {
			return new MessageStatsImpl(queuedToSend, inFlight, messagesSent.clone(), messagesReceived.clone(), ackLatency.clone());
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
	 * Create a new instance of this class. This constructor is used when making a deep copy of this {@link MessageStatsImpl stats} instance.
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
	private MessageStatsImpl(long messagesQueuedToSend, long messagesInFlight, MessageStat messagesSent, MessageStat messagesReceived,
			LatencyStatImpl ackLatency) {
		this.messagesQueuedToSend = messagesQueuedToSend;
		this.messagesInFlight = messagesInFlight;
		this.messagesSent = messagesSent;
		this.messagesReceived = messagesReceived;
		this.ackLatency = ackLatency;
		registeredChannels = null;
	}

	private final class MessageStat implements Cloneable {

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

}