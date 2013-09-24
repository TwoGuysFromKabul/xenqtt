package net.sf.xenqtt.mockbroker;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.sf.xenqtt.message.ConnectMessage;
import net.sf.xenqtt.message.PubAckMessage;
import net.sf.xenqtt.message.PubMessage;
import net.sf.xenqtt.message.QoS;
import net.sf.xenqtt.message.SubscribeMessage;
import net.sf.xenqtt.message.UnsubscribeMessage;

/**
 * A topic which includes the client subscriptions and retained message
 */
final class StandardTopic extends AbstractTopic {

	private final Map<String, Subscription> subscriptionByClientId = new HashMap<String, Subscription>();

	PubMessage retainedMessage;

	StandardTopic(String topicName, Collection<WildcardTopic> wildcardTopics) {
		super(topicName, false);

		addWildcardTopicSubscriptions(wildcardTopics);
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

		Subscription subscription = getSubscription(client.clientId);
		if (subscription != null) {
			if (client.cleanSession) {
				cleanSession(client);
				return;
			}

			subscription.connected(client);
		}
	}

	/**
	 * Removes all data for this client
	 */
	void cleanSession(Client client) {
		subscriptionByClientId.remove(client.clientId);
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
	 * Called when a {@link SubscribeMessage} is received for this topic
	 * 
	 * @return The granted {@link QoS}
	 */
	QoS subscribe(QoS qos, Client client) {

		qos = qos.value() > 1 ? QoS.AT_LEAST_ONCE : qos;
		Subscription subscription = getOrAddSubscription(client.clientId);
		doSubscribe(subscription, this, qos, client);

		return qos;
	}

	/**
	 * Called when a {@link SubscribeMessage} is received for the specified {@link WildcardTopic}
	 */
	void subscribe(WildcardTopic wildcardTopic, QoS qos, Client client) {

		if (nameMatches(wildcardTopic)) {
			Subscription subscription = getSubscription(client.clientId);
			if (subscription != null) {
				doSubscribe(subscription, wildcardTopic, qos, client);
			}
		}
	}

	/**
	 * Called when an {@link UnsubscribeMessage} is received for this topic
	 */
	void unsubscribe(Client client) {

		doUnsubscribe(this, client);
	}

	/**
	 * Called when an {@link UnsubscribeMessage} is received for the specified {@link WildcardTopic}
	 */
	void unsubscribe(WildcardTopic wildcardTopic, Client client) {

		if (nameMatches(wildcardTopic)) {
			doUnsubscribe(wildcardTopic, client);
		}
	}

	/**
	 * @return The {@link Subscription} for the specified client ID. Null if the specified client is not subscribed to this topic.
	 */
	private Subscription getSubscription(String clientId) {
		return subscriptionByClientId.get(clientId);
	}

	private void doSubscribe(Subscription subscription, AbstractTopic topicToSubscribeTo, QoS qos, Client client) {

		if (subscription.subscribe(topicToSubscribeTo.topicName, qos) && retainedMessage != null) {
			PubMessage msg = new PubMessage(qos, true, topicName, client.getNextMessageId(), retainedMessage.getPayload());
			client.send(msg);
		}
	}

	private void doUnsubscribe(AbstractTopic topicToUnsubscribeFrom, Client client) {
		Subscription subscription = getSubscription(client.clientId);
		if (subscription != null && subscription.unsubscribe(topicToUnsubscribeFrom.topicName) == 0) {
			subscriptionByClientId.remove(client.clientId);
		}
	}

	private Subscription getOrAddSubscription(String clientId) {

		Subscription subscription = getSubscription(clientId);
		if (subscription == null) {
			subscription = new Subscription(clientId);
			subscriptionByClientId.put(clientId, subscription);
		}
		return subscription;
	}

	private void addWildcardTopicSubscriptions(Collection<WildcardTopic> wildcardTopics) {

		for (WildcardTopic wildcardTopic : wildcardTopics) {
			if (nameMatches(wildcardTopic)) {
				for (Map.Entry<String, QoS> entry : wildcardTopic.qosByClientId.entrySet()) {
					String clientId = entry.getKey();
					QoS qos = entry.getValue();
					Subscription subscription = getOrAddSubscription(clientId);
					subscription.subscribe(wildcardTopic.topicName, qos);
				}
			}
		}
	}
}
