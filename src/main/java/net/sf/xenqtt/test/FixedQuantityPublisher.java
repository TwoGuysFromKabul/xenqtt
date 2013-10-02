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
package net.sf.xenqtt.test;

import java.util.concurrent.atomic.AtomicLong;

import net.sf.xenqtt.Log;
import net.sf.xenqtt.client.MqttClient;
import net.sf.xenqtt.client.PublishMessage;
import net.sf.xenqtt.message.QoS;

/**
 * TODO [jeremy] - Document this type.
 */
final class FixedQuantityPublisher implements Publisher {

	private final MqttClient client;
	private final XenqttTestClientStats stats;
	private final AtomicLong messagesToPublish;
	private final boolean asyncClient;

	FixedQuantityPublisher(MqttClient client, long messagesToPublish, XenqttTestClientStats stats, boolean asyncClient) {
		this.client = client;
		this.stats = stats;
		this.asyncClient = asyncClient;
		this.messagesToPublish = new AtomicLong(messagesToPublish);
	}

	/**
	 * @see net.sf.xenqtt.test.XenqttTestClient.Publisher#publish(java.lang.String, net.sf.xenqtt.message.QoS, byte[])
	 */
	@Override
	public long publish(String topic, QoS qos, byte[] payload) {
		long remainingMessagesToPublish = messagesToPublish.decrementAndGet();
		try {
			PublishMessage message = new PublishMessage(topic, qos, payload);
			client.publish(message);
			if (!asyncClient) {
				stats.publishComplete(message);
			}
		} catch (Exception ex) {
			Log.error(ex, "Failed to publish a message to the following topic: %s", topic);
		}

		return remainingMessagesToPublish;
	}

}
