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

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import net.sf.xenqtt.Log;
import net.sf.xenqtt.message.ConnectMessage;
import net.sf.xenqtt.message.PubAckMessage;
import net.sf.xenqtt.message.PubMessage;
import net.sf.xenqtt.message.QoS;
import net.sf.xenqtt.message.SubscribeMessage;
import net.sf.xenqtt.message.UnsubscribeMessage;

/**
 * Manages topics for the mock broker
 */
final class TopicManager {

	private static final Charset ASCII = Charset.forName("ASCII");

	private final Map<String, WildcardTopic> wildcardTopicByName = new LinkedHashMap<String, WildcardTopic>();
	private final Map<String, StandardTopic> standardTopicByName = new HashMap<String, StandardTopic>();
	private final Map<String, Client> clientById;

	/**
	 * Create a new instance of this class.
	 * 
	 * @param clientById
	 *            A {@link Map map} of {@link Client client}s by client ID
	 */
	public TopicManager(Map<String, Client> clientById) {
		this.clientById = clientById;
	}

	/**
	 * Called when a {@link ConnectMessage} is received
	 */
	void connected(Client client, ConnectMessage message) {
		for (StandardTopic topic : standardTopicByName.values()) {
			topic.connected(client);
		}

		if (client.cleanSession) {
			for (WildcardTopic topic : wildcardTopicByName.values()) {
				topic.cleanSession(client);
			}
		}

		if (message.isWillMessageFlag()) {
			StandardTopic topic = getStandardTopic(message.getWillTopic(), false);
			String willMessage = message.getWillMessage();
			byte[] payload = willMessage == null ? null : willMessage.getBytes(ASCII);
			PubMessage msg = new PubMessage(message.getWillQoS(), message.isWillRetain(), message.getWillTopic(), 0, payload);
			topic.publish(msg, clientById);
		}
	}

	/**
	 * Called when a {@link PubMessage} is received
	 */
	void publish(PubMessage message) {

		StandardTopic topic = getStandardTopic(message.getTopicName(), false);
		topic.publish(message, clientById);
	}

	/**
	 * Called when a {@link PubAckMessage} is received
	 * 
	 * @return true if this specified message is in a subscription queue in this topic for the specified client
	 */
	void pubAcked(Client client, PubAckMessage message) {
		int messageId = message.getMessageId();
		for (StandardTopic topic : standardTopicByName.values()) {
			if (topic.pubAcked(client, messageId)) {
				return;
			}
		}
	}

	/**
	 * Called when a {@link SubscribeMessage} is received
	 * 
	 * @return The granted qoses
	 */
	public QoS[] subscribe(Client client, SubscribeMessage message) {

		String[] topics = message.getTopics();
		QoS[] requestedQoses = message.getRequestedQoSes();
		QoS[] grantedQoses = new QoS[requestedQoses.length];

		for (int i = 0; i < topics.length; i++) {
			String topicName = topics[i];
			QoS qos = requestedQoses[i];

			try {
				if (AbstractTopic.checkWildcardAndVerifyTopic(topicName, true)) {
					WildcardTopic topic = getWildcardTopic(topicName);
					grantedQoses[i] = topic.subscribe(qos, client, standardTopicByName.values());
				} else {
					StandardTopic topic = getStandardTopic(topicName, true);
					grantedQoses[i] = topic.subscribe(qos, client);
				}
			} catch (Exception e) {
				Log.error(e, "Failed to subscribe to topic: " + topicName);
				grantedQoses[i] = QoS.AT_MOST_ONCE;
			}
		}

		return grantedQoses;
	}

	/**
	 * Called when an {@link UnsubscribeMessage} is received
	 */
	void unsubscribe(Client client, UnsubscribeMessage message) {

		for (String topicName : message.getTopics()) {
			try {
				if (AbstractTopic.checkWildcardAndVerifyTopic(topicName, true)) {
					if (wildcardTopicByName.containsKey(topicName)) {
						WildcardTopic topic = getWildcardTopic(topicName);
						topic.unsubscribe(client, standardTopicByName.values());
					}
				} else {
					if (standardTopicByName.containsKey(topicName)) {
						StandardTopic topic = getStandardTopic(topicName, true);
						topic.unsubscribe(client);
					}
				}

			} catch (Exception e) {
				Log.error(e, "Failed to unsubscribe from topic: " + topicName);
			}
		}
	}

	/**
	 * Called when a client channel is closed
	 */
	void clientClosed(Client client) {

		if (client.cleanSession) {
			for (WildcardTopic topic : wildcardTopicByName.values()) {
				topic.cleanSession(client);
			}
			for (StandardTopic topic : standardTopicByName.values()) {
				topic.cleanSession(client);
			}
		}
	}

	private WildcardTopic getWildcardTopic(String topicName) {

		WildcardTopic topic = wildcardTopicByName.get(topicName);
		if (topic == null) {
			topic = new WildcardTopic(topicName);
			wildcardTopicByName.put(topicName, topic);
		}
		return topic;
	}

	private StandardTopic getStandardTopic(String topicName, boolean alreadyVerified) {

		if (!alreadyVerified) {
			AbstractTopic.checkWildcardAndVerifyTopic(topicName, false);
		}

		StandardTopic topic = standardTopicByName.get(topicName);
		if (topic == null) {
			topic = new StandardTopic(topicName, wildcardTopicByName.values());
			standardTopicByName.put(topicName, topic);
		}

		return topic;
	}
}
