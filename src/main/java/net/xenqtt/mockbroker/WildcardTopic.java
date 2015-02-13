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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.xenqtt.message.QoS;
import net.xenqtt.message.SubscribeMessage;
import net.xenqtt.message.UnsubscribeMessage;

/**
 * A topic which includes the client subscriptions and retained message
 */
final class WildcardTopic extends AbstractTopic {

	final Map<String, QoS> qosByClientId = new HashMap<String, QoS>();

	/**
	 * Create a new instance of this class.
	 * 
	 * @param topicName
	 *            The name of the topic (e.g. {@code p/mop/123/65})
	 */
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
				standardTopic.unsubscribe(this, client);
			}
		}
	}
}
