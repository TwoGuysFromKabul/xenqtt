package net.sf.xenqtt.mockbroker;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import net.sf.xenqtt.message.ConnectMessage;
import net.sf.xenqtt.message.PubAckMessage;
import net.sf.xenqtt.message.PubMessage;
import net.sf.xenqtt.message.QoS;
import net.sf.xenqtt.message.SubscribeMessage;

/**
 * A {@link Client client's} subscription to a {@link Topic topic}
 */
final class Subscription {

	private final List<TopicSubscription> topicSubscriptions = new LinkedList<TopicSubscription>();

	private final String clientId;
	QoS subscribedQos = QoS.AT_MOST_ONCE;
	private final Queue<PubMessage> messageQueue;

	/**
	 * Creates a subscription to a topic
	 */
	Subscription(String clientId) {
		this.clientId = clientId;
		this.messageQueue = new LinkedList<PubMessage>();
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
	 * Called when a {@link PubMessage} is received
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

	private void send(Client client, PubMessage message) {

		if (subscribedQos.value() < message.getQoSLevel()) {
			if (subscribedQos.value() > 0) {
				message.setMessageId(client.getNextMessageId());
			}
			message = new PubMessage(subscribedQos, message.isRetain(), message.getTopicName(), message.getMessageId(), message.getPayload());
		} else if (message.getQoSLevel() > 0) {
			message.setMessageId(client.getNextMessageId());
		}

		client.send(message);
	}

	/**
	 * Called when a {@link SubscribeMessage} is received. Adds/updates the specified topic's association with this subscription.
	 * 
	 * @param topicName
	 *            Name of the topic to associate with this subscription
	 * @param qos
	 *            The qos level subscribed by this topic
	 * @return True if the subscription was added. False if it already existed even if the qos was updated
	 */
	public boolean subscribe(String topicName, QoS qos) {

		boolean found = unsubscribe(topicName);

		TopicSubscription topicSubscription = new TopicSubscription(topicName, qos);
		topicSubscriptions.add(0, topicSubscription);
		subscribedQos = qos;

		return !found;
	}

	/**
	 * Removes the specified topic's association with this subscription
	 * 
	 * @return True if the topic was removed. False if was not associated with this subscription
	 */
	public boolean unsubscribe(String topicName) {

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
				return true;
			}
		}

		return false;
	}

	private static class TopicSubscription {

		private final String topicName;
		private final QoS topicQos;

		public TopicSubscription(String topicName, QoS topicQos) {
			this.topicName = topicName;
			this.topicQos = topicQos;
		}
	}
}
