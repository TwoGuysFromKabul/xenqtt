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

import net.sf.xenqtt.message.MqttMessage;

/**
 * Allows you to see low level events from within an {@link MqttClient}. This has a performance impact and should only be used for debugging purposes. The same
 * executor service that calls the {@link AsyncClientListener} and {@link MqttClientListener} methods is used to invoke the methods in implementations of this
 * interface.
 */
public interface MqttClientDebugListener {

	/**
	 * Called when a connection is established. Established in this case means the socket connection was established.
	 * 
	 * @param client
	 *            The client that owns the connection that opened
	 * @param localAddress
	 *            Local address of the MQTT connection
	 * @param remoteAddress
	 *            Remote address of the MQTT connection
	 */
	void connectionOpened(MqttClient client, String localAddress, String remoteAddress);

	/**
	 * Called when a connection is closed. Closed in this case means the socket connection was closed.
	 * 
	 * @param client
	 *            The client that owns the connection that closed
	 * @param localAddress
	 *            Local address of the MQTT connection
	 * @param remoteAddress
	 *            Remote address of the MQTT connection
	 */
	void connectionClosed(MqttClient client, String localAddress, String remoteAddress);

	/**
	 * Called when a message is received
	 * 
	 * @param client
	 *            The client that received the message
	 * @param localAddress
	 *            Local address of the MQTT connection
	 * @param remoteAddress
	 *            Remote address of the MQTT connection
	 * @param message
	 *            The message that was received
	 */
	void messageReceived(MqttClient client, String localAddress, String remoteAddress, MqttMessage message);

	/**
	 * Called when a message is sent
	 * 
	 * @param client
	 *            The client that sent the message
	 * @param localAddress
	 *            Local address of the MQTT connection
	 * @param remoteAddress
	 *            Remote address of the MQTT connection
	 * @param message
	 *            The message that was sent
	 */
	void messageSent(MqttClient client, String localAddress, String remoteAddress, MqttMessage message);
}
