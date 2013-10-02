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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import net.sf.xenqtt.Log;
import net.sf.xenqtt.message.MqttMessage;

/**
 * Keeps track of {@link BrokerEvent}s
 */
final class BrokerEventsImpl implements BrokerEvents {

	private final List<BrokerEvent> events = Collections.synchronizedList(new LinkedList<BrokerEvent>());
	private final String shortStringFormat;
	private final String longStringFormat;

	BrokerEventsImpl() {
		int maxLen = 0;
		for (BrokerEventType type : BrokerEventType.values()) {
			int len = type.toString().length();
			if (len > maxLen) {
				maxLen = len;
			}
		}
		shortStringFormat = String.format("%%-%ds %%-22s %%-23s", maxLen);
		longStringFormat = String.format("%%-%ds %%-22s %%-23s %%s", maxLen);
	}

	/**
	 * @see net.sf.xenqtt.mockbroker.BrokerEvents#getEvents()
	 */
	@Override
	public List<BrokerEvent> getEvents() {
		synchronized (events) {
			return new ArrayList<BrokerEvent>(events);
		}
	}

	/**
	 * @see net.sf.xenqtt.mockbroker.BrokerEvents#getEvents(java.lang.String)
	 */
	@Override
	public List<BrokerEvent> getEvents(String clientId) {
		List<BrokerEvent> list = new ArrayList<BrokerEvent>();
		synchronized (events) {
			for (BrokerEvent event : events) {
				if (clientId == null) {
					if (event.getClientId() == null) {
						list.add(event);
					}
				} else if (clientId.equals(event.getClientId())) {
					list.add(event);
				}
			}
		}
		return list;
	}

	/**
	 * @see net.sf.xenqtt.mockbroker.BrokerEvents#clearEvents()
	 */
	@Override
	public void clearEvents() {
		events.clear();
	}

	/**
	 * @see net.sf.xenqtt.mockbroker.BrokerEvents#removeEvents(java.util.Collection)
	 */
	@Override
	public void removeEvents(Collection<BrokerEvent> eventsToRemove) {
		events.removeAll(eventsToRemove);
	}

	/**
	 * @see net.sf.xenqtt.mockbroker.BrokerEvents#addEvent(net.sf.xenqtt.mockbroker.BrokerEventType, net.sf.xenqtt.mockbroker.Client)
	 */
	@Override
	public void addEvent(BrokerEventType eventType, Client client) {
		addEvent(eventType, client, null);
	}

	/**
	 * @see net.sf.xenqtt.mockbroker.BrokerEvents#addEvent(net.sf.xenqtt.mockbroker.BrokerEventType, net.sf.xenqtt.mockbroker.Client,
	 *      net.sf.xenqtt.message.MqttMessage)
	 */
	@Override
	public void addEvent(BrokerEventType eventType, Client client, MqttMessage message) {
		events.add(new BrokerEvent(eventType, client, message));
		String clientId = client == null || client.clientId == null ? "" : client.clientId;
		if (message == null) {
			Log.info(shortStringFormat, eventType, client.remoteAddress(), clientId);
		} else {
			Log.info(longStringFormat, eventType, client.remoteAddress(), clientId, message);
		}
	}
}
