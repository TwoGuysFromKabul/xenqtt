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
package net.sf.xenqtt.mockbroker;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import net.sf.xenqtt.XenqttUtil;
import net.sf.xenqtt.message.ConnectMessage;
import net.sf.xenqtt.message.PubAckMessage;
import net.sf.xenqtt.message.PubMessage;
import net.sf.xenqtt.message.QoS;

/**
 * A {@link Client client's} subscription to a set of {@link StandardTopic topics}
 */
final class Subscription {

	private final List<TopicSubscription> topicSubscriptions = new LinkedList<TopicSubscription>();

	private final Queue<PubMessage> messageQueue = new LinkedList<PubMessage>();
	private final String clientId;
	private QoS subscribedQos = QoS.AT_MOST_ONCE;

	/**
	 * Creates a subscription to a topic
	 */
	Subscription(String clientId) {
		this.clientId = clientId;
	}

	/**
	 * Called when a {@link ConnectMessage} is received
	 */
	void connected(Client client) {

		for (PubMessage pub : messageQueue) {
			send(client, pub);
		}
	}

	/**
	 * Called when a {@link PubAckMessage} is received
	 * 
	 * @return true if this specified message is in the subscription queue for the specified client
	 */
	boolean pubAcked(int messageId) {
		Iterator<PubMessage> iter = messageQueue.iterator();
		while (iter.hasNext()) {
			PubMessage pub = iter.next();
			if (pub.getMessageId() == messageId) {
				iter.remove();
				return true;
			}
		}

		return false;
	}

	/**
	 * Published the specified message to this subscription
	 * 
	 * @param message
	 *            The message to publish
	 * @param clientById
	 *            Map of currently connected clients by ID. If the client this subscription is for then the message is sent to the client immediately
	 */
	void publish(PubMessage message, Map<String, Client> clientById) {

		if (message.getQoSLevel() > 0 && subscribedQos.value() > 0) {
			messageQueue.add(message);
		}

		Client client = clientById.get(clientId);
		if (client != null) {
			send(client, message);
		}
	}

	/**
	 * Adds/updates the specified topic's association with this subscription. If the topic is already associated with the same qos this method does nothing. If
	 * the topic is already associated but with a different qos this method {@link #unsubscribe(String) unsubscribes} the original subscription then adds this
	 * subscription as a new one.
	 * 
	 * @param topicName
	 *            Name of the topic to associate with this subscription
	 * @param qos
	 *            The qos level subscribed by this topic
	 * 
	 * @return True if the subscription was added. False if it already existed even if the qos was updated
	 */
	public boolean subscribe(String topicName, QoS qos) {
		XenqttUtil.validateNotEmpty("topicName", topicName);
		XenqttUtil.validateNotNull("qos", qos);

		for (TopicSubscription topicSubscription : topicSubscriptions) {
			if (topicSubscription.topicName.equals(topicName)) {
				if (topicSubscription.topicQos == qos) {
					return false;
				}
			}
		}

		int oldSize = topicSubscriptions.size();
		int newSize = unsubscribe(topicName);

		TopicSubscription topicSubscription = new TopicSubscription(topicName, qos);
		topicSubscriptions.add(0, topicSubscription);
		subscribedQos = qos;

		return newSize == oldSize;
	}

	/**
	 * Removes the specified topic's association with this subscription
	 * 
	 * @return Number of topics still subscribed
	 */
	public int unsubscribe(String topicName) {
		XenqttUtil.validateNotEmpty("topicName", topicName);

		Iterator<TopicSubscription> iter = topicSubscriptions.iterator();
		int i = 0;
		while (iter.hasNext()) {
			TopicSubscription topicSubscription = iter.next();
			if (topicSubscription.topicName.equals(topicName)) {
				iter.remove();
				if (i == 0) {
					if (iter.hasNext()) {
						topicSubscription = iter.next();
						subscribedQos = topicSubscription.topicQos;
					} else {
						subscribedQos = QoS.AT_MOST_ONCE;
					}
				}
				return topicSubscriptions.size();
			}
			i++;
		}

		return topicSubscriptions.size();
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Subscription [clientId=" + clientId + ", subscribedQos=" + subscribedQos + ", topicSubscriptions=" + topicSubscriptions
				+ ", queuedMessageCount=" + messageQueue.size() + "]";
	}

	private void send(Client client, PubMessage message) {

		QoS qos = subscribedQos.value() < message.getQoSLevel() ? subscribedQos : message.getQoS();
		int messageId = qos.value() > 0 ? client.getNextMessageId() : message.getMessageId();
		message = new PubMessage(qos, message.isRetain(), message.getTopicName(), messageId, message.getPayload());

		client.send(message);
	}

	private static class TopicSubscription {

		private final String topicName;
		private final QoS topicQos;

		public TopicSubscription(String topicName, QoS topicQos) {
			this.topicName = topicName;
			this.topicQos = topicQos;
		}

		/**
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "[topicName=" + topicName + ", topicQos=" + topicQos + "]";
		}
	}
}
