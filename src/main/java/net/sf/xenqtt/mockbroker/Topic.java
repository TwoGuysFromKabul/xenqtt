package net.sf.xenqtt.mockbroker;

import java.util.HashMap;
import java.util.Map;

import net.sf.xenqtt.XenqttUtil;
import net.sf.xenqtt.message.ConnectMessage;
import net.sf.xenqtt.message.PubAckMessage;
import net.sf.xenqtt.message.PubMessage;
import net.sf.xenqtt.message.QoS;
import net.sf.xenqtt.message.SubscribeMessage;
import net.sf.xenqtt.message.UnsubscribeMessage;

/**
 * A topic which includes the client subscriptions and retained message
 */
final class Topic {

	private final Map<String, Subscription> subscriptionByClientId = new HashMap<String, Subscription>();
	private final String topicName;
	private String[] topicLevels;
	PubMessage retainedMessage;

	Topic(String topicName) {
		this.topicName = topicName;
	}

	/**
	 * Called when a {@link PubAckMessage} is received
	 * 
	 * @return true if this specified message is in a subscription queue in this topic for the specified client
	 */
	boolean pubAcked(Client client, int messageId) {
		Subscription subscription = getSubscription(client.clientId);
		if (subscription != null && subscription.pubAcked(messageId)) {
			return true;
		}
		return false;
	}

	/**
	 * Called when a {@link ConnectMessage} is received
	 */
	void connected(Client client) {

		if (client.cleanSession) {
			unsubscribe(client);
			return;
		}

		Subscription subscription = getSubscription(client.clientId);
		if (subscription != null) {
			subscription.connected(client);
		}
	}

	/**
	 * Called when a {@link PubMessage} is received
	 */
	void publish(PubMessage message, Map<String, Client> clientById) {

		if (message.isRetain()) {
			byte[] payload = message.getPayload();
			if (payload == null || payload.length == 0) {
				retainedMessage = null;
				return;
			} else {
				retainedMessage = message;
			}
		}

		for (Subscription subscription : subscriptionByClientId.values()) {
			subscription.publish(message, clientById);
		}
	}

	/**
	 * Called when a {@link SubscribeMessage} is received
	 * 
	 * @return The granted {@link QoS}
	 */
	QoS subscribe(QoS qos, Client client, Subscription subscription) {

		qos = qos.value() > 1 ? QoS.AT_LEAST_ONCE : qos;
		if (subscription.subscribe(topicName, qos)) {
			subscriptionByClientId.put(client.clientId, subscription);
			if (retainedMessage != null) {
				PubMessage msg = new PubMessage(qos, true, topicName, client.getNextMessageId(), retainedMessage.getPayload());
				client.send(msg);
			}
		}

		return qos;
	}

	/**
	 * Called when an {@link UnsubscribeMessage} is received
	 */
	void unsubscribe(Client client) {

		Subscription subscription = subscriptionByClientId.remove(client.clientId);
		if (subscription != null) {
			subscription.unsubscribe(topicName);
		}
	}

	/**
	 * @return True if this topic's name matches the specified topic name including wildcard resolution.
	 */
	boolean nameMatches(String[] topicLevels) {

		if (this.topicLevels == null) {
			this.topicLevels = XenqttUtil.quickSplit(topicName, '/');
		}

		// if they are not the same depth and neither ends in # they can't match
		if (topicLevels.length != this.topicLevels.length && !"#".equals(topicLevels[topicLevels.length - 1])
				&& !"#".equals(this.topicLevels[this.topicLevels.length - 1])) {
			return false;
		}

		int size = topicLevels.length < this.topicLevels.length ? topicLevels.length : this.topicLevels.length;
		for (int i = 0; i < size; i++) {

			String s1 = topicLevels[i];
			String s2 = this.topicLevels[i];

			// if either has a # the rest doesn't matter - presumably the topic was already validated so # can only be the last level
			if ("#".equals(s1) || "#".equals(s2)) {
				return true;
			}

			// if neither has a + and the this level is not equal then the topics don't match
			if (!"+".equals(s1) && !"+".equals(s2) && !s1.equals(s2)) {
				return false;
			}
		}

		// if everything else matched then they are equal only if they have the same length
		return topicLevels.length == this.topicLevels.length;
	}

	/**
	 * @return The {@link Subscription} for the specified client ID. Null if the specified client is not subscribed to this topic.
	 */
	Subscription getSubscription(String clientId) {
		return subscriptionByClientId.get(clientId);
	}
}
