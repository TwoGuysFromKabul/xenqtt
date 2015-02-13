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
import java.util.List;

import net.xenqtt.message.MqttMessage;

/**
 * Specifies a type that captures and reports on events within the {@link MockBroker mock broker}.
 */
interface BrokerEvents {

	/**
	 * @return All broker events. The returned {@link List list} should be a copy
	 */
	List<BrokerEvent> getEvents();

	/**
	 * @return All broker events for the specified client ID. The returned {@link List list} is a copy
	 */
	List<BrokerEvent> getEvents(String clientId);

	/**
	 * Removes all broker events.
	 */
	void clearEvents();

	/**
	 * Removes the specified broker events.
	 */
	void removeEvents(Collection<BrokerEvent> eventsToRemove);

	/**
	 * Add an event.
	 * 
	 * @param eventType
	 *            The {@link BrokerEventType event type} to add
	 * @param client
	 *            The {@link Client client} to associate the event with
	 */
	void addEvent(BrokerEventType eventType, Client client);

	/**
	 * Add an event.
	 * 
	 * @param eventType
	 *            The {@link BrokerEventType event type} to add
	 * @param client
	 *            The {@link Client client} to associate the event with
	 * @param message
	 *            The {@link MqttMessage message} that is the focus of the event being added
	 */
	void addEvent(BrokerEventType eventType, Client client, MqttMessage message);

}
