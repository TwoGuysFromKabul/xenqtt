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
final class BrokerEvents {

	private final List<BrokerEvent> events = Collections.synchronizedList(new LinkedList<BrokerEvent>());
	private final String shortStringFormat;
	private final String longStringFormat;

	BrokerEvents() {
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
	 * @return All broker events. This list is a copy.
	 */
	public List<BrokerEvent> getEvents() {
		synchronized (events) {
			return new ArrayList<BrokerEvent>(events);
		}
	}

	/**
	 * @return All broker events for the specified client ID. This list is a copy.
	 */
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
	 * Removes all broker events
	 */
	public void clearEvents() {
		events.clear();
	}

	/**
	 * Removes the specified broker events.
	 */
	public void removeEvents(Collection<BrokerEvent> eventsToRemove) {
		events.removeAll(eventsToRemove);
	}

	void addEvent(BrokerEventType eventType, Client client) {
		addEvent(eventType, client, null);
	}

	void addEvent(BrokerEventType eventType, Client client, MqttMessage message) {
		events.add(new BrokerEvent(eventType, client, message));
		String clientId = client == null || client.clientId == null ? "" : client.clientId;
		if (message == null) {
			Log.info(shortStringFormat, eventType, client.remoteAddress(), clientId);
		} else {
			Log.info(longStringFormat, eventType, client.remoteAddress(), clientId, message);
		}
	}
}
