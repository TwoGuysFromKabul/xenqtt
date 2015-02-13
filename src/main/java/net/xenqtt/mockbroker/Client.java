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
package net.xenqtt.mockbroker;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import net.xenqtt.XenqttUtil;
import net.xenqtt.message.ConnectMessage;
import net.xenqtt.message.MqttChannel;
import net.xenqtt.message.MqttMessage;
import net.xenqtt.message.PubAckMessage;
import net.xenqtt.message.PubMessage;

/**
 * Info on a client connected to the mock broker
 */
public final class Client {

	private final int maxInFlightMessages;
	private final Queue<PubMessage> pendingMessages = new LinkedList<PubMessage>();
	private final Set<Integer> inFlightMessages = new HashSet<Integer>();

	String clientId;
	boolean cleanSession;
	private int nextMessageId;
	private final MqttChannel channel;
	private final BrokerEvents events;

	/**
	 * Create a new instance of this class.
	 * 
	 * @param channel
	 *            The {@link MqttChannel channel} associated with this client
	 * @param events
	 *            The {@link BrokerEvents events} being tracked for the mock broker
	 * @param maxInFlightMessages
	 *            The maximum number of in-flight messages allowed
	 */
	Client(MqttChannel channel, BrokerEvents events, int maxInFlightMessages) {
		this.channel = channel;
		this.events = events;
		this.maxInFlightMessages = maxInFlightMessages;
	}

	/**
	 * @return The client ID of this client. Null if no {@link ConnectMessage} has been received.
	 */
	public String getClientId() {
		return clientId;
	}

	/**
	 * Closes the connection to this client
	 */
	public void close() {
		channel.close();
	}

	/**
	 * Sends the message to this client
	 * 
	 * @return true if the message was sent, false if the maximum number of in-flight messages has already been reached and this message was queued to be sent
	 *         later.
	 */
	public boolean send(MqttMessage message) {

		XenqttUtil.validateNotNull("message", message);

		if (inFlightMessages.size() >= maxInFlightMessages && message.getQoSLevel() > 0 && message instanceof PubMessage) {
			PubMessage pubMessage = (PubMessage) message;
			pendingMessages.add(pubMessage);
			return false;
		}

		doSend(message);
		return true;
	}

	/**
	 * Called whenever an {@link MqttMessage} is received
	 */
	void messageReceived(MqttMessage message) {
		events.addEvent(BrokerEventType.MESSAGE_RECEIVED, this, message);

		if (!(message instanceof PubAckMessage)) {
			return;
		}

		int messageId = ((PubAckMessage) message).getMessageId();
		inFlightMessages.remove(messageId);
		while (inFlightMessages.size() < maxInFlightMessages) {
			PubMessage nextMessage = pendingMessages.poll();
			if (nextMessage == null) {
				break;
			}
			doSend(nextMessage);
		}
	}

	private void doSend(MqttMessage message) {

		if (message.getQoSLevel() > 0 && message instanceof PubMessage) {
			PubMessage pubMessage = (PubMessage) message;
			pubMessage.setMessageId(getNextMessageId());
			inFlightMessages.add(pubMessage.getMessageId());
		}

		channel.send(message, null);
		events.addEvent(BrokerEventType.MESSAGE_SENT, this, message);
	}

	/**
	 * @return The message ID to use for the next identifiable message sent to this client by the broker
	 */
	private int getNextMessageId() {

		if (++nextMessageId > 0xffff) {
			nextMessageId = 1;
		}
		return nextMessageId;
	}

	/**
	 * @return The client's address
	 */
	String remoteAddress() {
		return channel.getRemoteAddress();
	}
}
