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
package net.xenqtt.client;

import net.xenqtt.message.ConnectReturnCode;

/**
 * Acks any received messages. Ignores all other events.
 */
final class NullClientListener implements AsyncClientListener {

	/**
	 * @see net.xenqtt.client.MqttClientListener#publishReceived(net.xenqtt.client.MqttClient, net.xenqtt.client.PublishMessage)
	 */
	@Override
	public void publishReceived(MqttClient client, PublishMessage message) {
	}

	/**
	 * @see net.xenqtt.client.MqttClientListener#disconnected(net.xenqtt.client.MqttClient, java.lang.Throwable, boolean)
	 */
	@Override
	public void disconnected(MqttClient client, Throwable cause, boolean reconnecting) {
	}

	/**
	 * @see net.xenqtt.client.AsyncClientListener#connected(net.xenqtt.client.MqttClient, net.xenqtt.message.ConnectReturnCode)
	 */
	@Override
	public void connected(MqttClient client, ConnectReturnCode returnCode) {
	}

	/**
	 * @see net.xenqtt.client.AsyncClientListener#subscribed(net.xenqtt.client.MqttClient, net.xenqtt.client.Subscription[],
	 *      net.xenqtt.client.Subscription[], boolean)
	 */
	@Override
	public void subscribed(MqttClient client, Subscription[] requestedSubscriptions, Subscription[] grantedSubscriptions, boolean requestsGranted) {
	}

	/**
	 * @see net.xenqtt.client.AsyncClientListener#unsubscribed(net.xenqtt.client.MqttClient, java.lang.String[])
	 */
	@Override
	public void unsubscribed(MqttClient client, String[] topics) {
	}

	/**
	 * @see net.xenqtt.client.AsyncClientListener#published(net.xenqtt.client.MqttClient, net.xenqtt.client.PublishMessage)
	 */
	@Override
	public void published(MqttClient client, PublishMessage message) {
		message.ack();
	}
}
