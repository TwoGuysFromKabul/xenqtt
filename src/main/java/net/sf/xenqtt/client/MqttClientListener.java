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

import net.sf.xenqtt.message.QoS;

/**
 * Implement this interface to use {@link SyncMqttClient}. The client will invoke the methods in this interface when a published message is received. A single
 * instance of this interface may be used with multiple clients.
 */
public interface MqttClientListener {

	/**
	 * Use this {@link MqttClientListener listener} when you want to ignore all client events including received messages. Any received messages will be
	 * {@link PublishMessage#ack() ack'd}.
	 */
	public static MqttClientListener NULL_LISTENER = new NullClientListener();

	/**
	 * Called when a published message is received from the broker. You should always call {@link PublishMessage#ack() ack()} when you are done processing the
	 * message. This is not required if the {@link PublishMessage#getQoS() QoS} is {@link QoS#AT_MOST_ONCE} but it is a good practice to always call it.
	 * 
	 * @param client
	 *            The client that received the message
	 * @param message
	 *            The message that was published
	 */
	void publishReceived(MqttClient client, PublishMessage message);

	/**
	 * Called when the connection to the broker is lost either unintentionally or because the client requested the disconnect.
	 * 
	 * @param client
	 *            The client that was disconnected
	 * @param cause
	 *            The exception that caused the client to disconnect. Null if there was no exception.
	 * @param reconnecting
	 *            True if the client will attempt to reconnect. False if either all reconnect attempts have failed or the disconnect was not because of an
	 *            exception.
	 */
	void disconnected(MqttClient client, Throwable cause, boolean reconnecting);
}
