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
package net.sf.xenqtt.client;

import net.sf.xenqtt.XenqttUtil;
import net.sf.xenqtt.message.QoS;

/**
 * An MQTT topic subscription
 */
public final class Subscription {

	private final String topic;
	private final QoS qos;

	/**
	 * @param topic
	 *            The topic being subscribed to. This can include wildcards:
	 *            <ul>
	 *            <li>'+': Matches a single level in the topic. foo/+ would match foo/bar but not foo/a/b or foo/a/b/c. foo/+/+/c would match foo/a/b/c and
	 *            foo/d/g/c but not foo/a/c</li>
	 *            <li>'#': Matches the rest of the topic. Must be the last character in the topic. foo/# would match foo/bar, foo/a/b/c, etc</li>
	 *            </ul>
	 * @param qos
	 *            The QoS for the topic. When a subscription request is sent to the broker this is the requested qos. When the subscription acknowledgment is
	 *            received from the broker this is the granted qos.
	 */
	public Subscription(String topic, QoS qos) {
		this.topic = XenqttUtil.validateNotNull("topic", topic);
		this.qos = XenqttUtil.validateNotNull("qos", qos);
	}

	/**
	 * @return The topic being subscribed to. This can include wildcards:
	 *         <ul>
	 *         <li>'+': Matches a single level in the topic. foo/+ would match foo/bar but not foo/a/b or foo/a/b/c. foo/+/+/c would match foo/a/b/c and
	 *         foo/d/g/c but not foo/a/c</li>
	 *         <li>'#': Matches the rest of the topic. Must be the last character in the topic. foo/# would match foo/bar, foo/a/b/c, etc</li>
	 *         </ul>
	 */
	public String getTopic() {
		return topic;
	}

	/**
	 * @return The QoS for the topic. When a subscription request is sent to the broker this is the requested qos. When the subscription acknowledgment is
	 *         received from the broker this is the granted qos.
	 */
	public QoS getQos() {
		return qos;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {

		int hash = topic.hashCode();
		hash = 31 * hash + qos.hashCode();
		return hash;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {

		if (obj == this) {
			return true;
		}

		if (!(obj instanceof Subscription)) {
			return false;
		}

		Subscription that = (Subscription) obj;

		return qos == that.qos && topic.equals(that.topic);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Subscription [topic=" + topic + ", qos=" + qos + "]";
	}
}
