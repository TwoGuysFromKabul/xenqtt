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

import net.sf.xenqtt.Log;
import net.sf.xenqtt.client.MqttClient;
import net.sf.xenqtt.client.PublishMessage;
import net.sf.xenqtt.message.QoS;

/**
 * A {@link Publisher} implementation that publishes messages for a certain duration and then stops.
 */
final class TimeBasedPublisher implements Publisher {

	private final MqttClient client;
	private final XenqttTestClientStats stats;
	private final boolean asyncClient;

	/**
	 * Create a new instance of this class.
	 * 
	 * @param client
	 *            The {@link MqttClient client} to use in publishing messages to the broker
	 * @param stats
	 *            The {@link XenqttTestClientStats stats} being used in this test
	 * @param asyncClient
	 *            Whether or not the specified {@link client} was asynchronous or not
	 */
	TimeBasedPublisher(MqttClient client, XenqttTestClientStats stats, boolean asyncClient) {
		this.client = client;
		this.stats = stats;
		this.asyncClient = asyncClient;
	}

	/**
	 * @see net.sf.xenqtt.test.XenqttTestClient.Publisher#publish(java.lang.String, net.sf.xenqtt.message.QoS, byte[])
	 */
	@Override
	public long publish(String topic, QoS qos, byte[] payload) {
		try {
			PublishMessage message = new PublishMessage(topic, qos, payload);
			client.publish(message);
			if (!asyncClient) {
				stats.publishComplete(message);
			}
		} catch (Exception ex) {
			Log.error(ex, "Failed to publish a message to the following topic: %s", topic);
		}

		return 1;
	}

}
