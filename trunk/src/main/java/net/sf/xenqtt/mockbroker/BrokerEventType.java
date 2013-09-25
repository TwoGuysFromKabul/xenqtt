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

import net.sf.xenqtt.message.MqttMessage;

/**
 * Type of event for {@link BrokerEvent}.
 */
public enum BrokerEventType {

	/**
	 * An {@link MqttMessage} queued for sending from the broker to a client
	 */
	MESSAGE_SENT,

	/**
	 * An {@link MqttMessage} was received by the broker from the client
	 */
	MESSAGE_RECEIVED,

	/**
	 * A socket was opened on the broker from the client (a new client connected).
	 */
	CHANNEL_OPENED,

	/**
	 * The broker's socket to the client was closed.
	 */
	CHANNEL_CLOSED
}
