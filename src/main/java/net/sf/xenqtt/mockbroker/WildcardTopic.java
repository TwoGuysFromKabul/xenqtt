package net.sf.xenqtt.mockbroker;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.sf.xenqtt.message.QoS;
import net.sf.xenqtt.message.SubscribeMessage;
import net.sf.xenqtt.message.UnsubscribeMessage;

/**
 * A topic which includes the client subscriptions and retained message
 */
final class WildcardTopic extends AbstractTopic {

	final Map<String, QoS> qosByClientId = new HashMap<String, QoS>();

	WildcardTopic(String topicName) {
		super(topicName, true);
	}

	/**
	 * Removes all data for this client
	 */
	void cleanSession(Client client) {

		qosByClientId.remove(client.clientId);
	}

	/**
	 * Called when a {@link SubscribeMessage} is received for this wildcard topic
	 * 
	 * @return The granted {@link QoS}
	 */
	QoS subscribe(QoS qos, Client client, Collection<StandardTopic> standardTopics) {

		qos = qos.value() > 1 ? QoS.AT_LEAST_ONCE : qos;
		QoS oldQos = qosByClientId.put(client.clientId, qos);
		if (oldQos != qos) {
			for (StandardTopic standardTopic : standardTopics) {
				standardTopic.subscribe(this, qos, client);
			}
		}

		return qos;
	}

	/**
	 * Called when an {@link UnsubscribeMessage} is received
	 */
	void unsubscribe(Client client, Collection<StandardTopic> standardTopics) {

		QoS qos = qosByClientId.remove(client.clientId);
		if (qos != null) {
			for (StandardTopic standardTopic : standardTopics) {
				standardTopic.unsubscribe(client);
			}
		}
	}
}
