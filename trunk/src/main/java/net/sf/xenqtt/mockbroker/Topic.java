package net.sf.xenqtt.mockbroker;

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
final class Topic {

	private final Map<String, Subscription> subscriptionByClientId = new HashMap<String, Subscription>();
	private final String topicName;
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
		Subscription subscription = getSubscription(client);
		if (subscription.pubAcked(messageId)) {
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

		Subscription subscription = getSubscription(client);
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
	QoS subscribe(QoS qos, Client client) {

		qos = qos.value() > 1 ? QoS.AT_LEAST_ONCE : qos;
		PubMessage retainedMessage = addSubscription(client, qos);
		if (retainedMessage != null) {
			PubMessage msg = new PubMessage(qos, true, topicName, client.getNextMessageId(), retainedMessage.getPayload());
			client.send(msg);
		}

		return qos;
	}

	/**
	 * Called when an {@link UnsubscribeMessage} is received
	 */
	void unsubscribe(Client client) {

		subscriptionByClientId.remove(client.clientId);
	}

	private PubMessage addSubscription(Client client, QoS qos) {

		Subscription subscription = getSubscription(client);

		if (subscription == null) {
			subscription = new Subscription(client.clientId, qos);
		} else {
			subscription.subscribedQos = qos;
		}

		subscriptionByClientId.put(client.clientId, subscription);

		return retainedMessage;
	}

	private Subscription getSubscription(Client client) {
		return subscriptionByClientId.get(client.clientId);
	}
}
