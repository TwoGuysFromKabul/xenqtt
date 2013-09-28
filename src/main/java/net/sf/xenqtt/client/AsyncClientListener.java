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

import net.sf.xenqtt.message.ConnectReturnCode;
import net.sf.xenqtt.message.QoS;

/**
 * <p>
 * Implement this interface to use the {@link AsyncMqttClient}. The client will invoke the methods in this interface when various events happen. A single
 * instance of this interface may be used with multiple clients.
 * </p>
 */
public interface AsyncClientListener extends MqttClientListener {

	/**
	 * Use this {@link AsyncClientListener listener} when you want to ignore all client events including received messages. Any received messages will be
	 * {@link PublishMessage#ack() ack'd}.
	 */
	AsyncClientListener NULL_LISTENER = new NullClientListener();

	/**
	 * Called after the client has received a connect acknowledgment from the broker.
	 * 
	 * @param client
	 *            The client that is connected
	 * 
	 * @param returnCode
	 *            The connect return code from the broker. Anything other than {@link ConnectReturnCode#ACCEPTED} will result in the client being immediately
	 *            disconnected.
	 */
	void connected(MqttClient client, ConnectReturnCode returnCode);

	/**
	 * Called when the client receives a subscribe acknowledgment from the broker.
	 * 
	 * @param client
	 *            The client that requested the subscriptions
	 * @param requestedSubscriptions
	 *            The subscriptions requested. The topics will be the same as those in grantedSubscriptions and the {@link Subscription#getQos() QoS} will be
	 *            the QoS the client requested.
	 * @param grantedSubscriptions
	 *            The subscriptions. The topics will be the same as in requestedSubscriptions but the {@link Subscription#getQos() QoS} will be the QoS granted
	 *            by the broker, not the QoS requested by the client.
	 * @param requestsGranted
	 *            True if the requested {@link QoS} for each topic matches the granted QoS. False otherwise.
	 */
	void subscribed(MqttClient client, Subscription[] requestedSubscriptions, Subscription[] grantedSubscriptions, boolean requestsGranted);

	/**
	 * Called when an unsubscribe acknowledgment is received from the broker.
	 * 
	 * @param client
	 *            The client that requested the unsubscribe
	 * @param topics
	 *            The topics unsubscribe from. These may include wildcards.
	 */
	void unsubscribed(MqttClient client, String[] topics);

	/**
	 * Called when the protocol to send a client publish message to the broker is complete.
	 * 
	 * @param client
	 *            The client the message was published to
	 * @param message
	 *            The message that was published. This will be the same object passed to {@link MqttClient#publish(PublishMessage)}.
	 */
	void published(MqttClient client, PublishMessage message);
}
