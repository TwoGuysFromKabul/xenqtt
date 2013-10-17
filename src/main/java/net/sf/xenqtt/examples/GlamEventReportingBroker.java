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
package net.sf.xenqtt.examples;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.sf.xenqtt.message.MessageType;
import net.sf.xenqtt.message.PubMessage;
import net.sf.xenqtt.message.QoS;
import net.sf.xenqtt.message.SubscribeMessage;
import net.sf.xenqtt.mockbroker.BrokerEvent;
import net.sf.xenqtt.mockbroker.BrokerEventType;
import net.sf.xenqtt.mockbroker.Client;
import net.sf.xenqtt.mockbroker.MockBroker;
import net.sf.xenqtt.mockbroker.MockBrokerHandler;

/**
 * Fires up a mock broker that specializes in routing data of the 'Glam' variety. This particular broker has a special handler that rejects any and all attempts
 * to interact with country music. In addition, it allows for retrieval of events from the broker and their subsequent interrogation.
 */
public class GlamEventReportingBroker {

	public static void main(String... args) throws InterruptedException {
		MockBrokerHandler handler = new GlamBrokerHandler();
		MockBroker broker = new MockBroker(handler);

		broker.init(); // Blocks until startup is complete.

		// At this point the broker is online. Clients can connect to it, publish messages, subscribe, etc.
		Thread.sleep(60000);

		// Examine broker events and report some useful stats about them.
		List<BrokerEvent> events = broker.getEvents();
		Set<String> clientIds = new HashSet<String>();
		Map<BrokerEventType, Integer> eventTypes = new EnumMap<BrokerEventType, Integer>(BrokerEventType.class);
		Map<MessageType, Integer> messageTypes = new EnumMap<MessageType, Integer>(MessageType.class);
		for (BrokerEvent event : events) {
			clientIds.add(event.getClientId());

			BrokerEventType brokerEventType = event.getEventType();
			Integer brokerEventCount = eventTypes.get(brokerEventType);
			if (brokerEventCount == null) {
				brokerEventCount = Integer.valueOf(0);
			}
			eventTypes.put(brokerEventType, Integer.valueOf(brokerEventCount.intValue() + 1));

			MessageType messageType = event.getMessage().getMessageType();
			Integer messageTypeCount = messageTypes.get(messageType);
			if (messageTypeCount == null) {
				messageTypeCount = Integer.valueOf(0);
			}
			messageTypes.put(messageType, Integer.valueOf(messageTypeCount.intValue() + 1));
		}
		System.out.printf("Total events: %d\n", events.size());
		System.out.printf("Total client IDs: %d\n", clientIds.size());

		System.out.println("Counts by broker event type:");
		for (Entry<BrokerEventType, Integer> entry : eventTypes.entrySet()) {
			System.out.printf("\t%s: %d\n", entry.getKey().name(), entry.getValue());
		}

		System.out.printf("Counts by MQTT message type:");
		for (Entry<MessageType, Integer> entry : messageTypes.entrySet()) {
			System.out.printf("\t%s: %d\n", entry.getKey().name(), entry.getValue());
		}

		// We are done. Shutdown the broker. Wait forever (> 0 means wait that many milliseconds).
		broker.shutdown(0);
	}

	private static final class GlamBrokerHandler extends MockBrokerHandler {

		@Override
		public boolean publish(Client client, PubMessage message) throws Exception {
			String payload = new String(message.getPayload(), Charset.forName("UTF-8"));
			if (payload.indexOf("Country Music") > -1) {
				// We don't do that stuff here! Return true to suppress processing of the message
				return true;
			}

			return super.publish(client, message);
		}

		/**
		 * @see net.sf.xenqtt.mockbroker.MockBrokerHandler#subscribe(net.sf.xenqtt.mockbroker.Client, net.sf.xenqtt.message.SubscribeMessage)
		 */
		@Override
		public boolean subscribe(Client client, SubscribeMessage message) throws Exception {
			String[] topics = message.getTopics();
			QoS[] qoses = message.getRequestedQoSes();
			List<String> allowedTopics = new ArrayList<String>();
			List<QoS> allowedQoses = new ArrayList<QoS>();
			int index = 0;
			for (String topic : topics) {
				// Only allow topic subscriptions for topics that don't include country music.
				if (!topic.matches("^.*(?i:country).*$")) {
					allowedTopics.add(topic);
					allowedQoses.add(qoses[index]);
				}

				index++;
			}

			message = new SubscribeMessage(message.getMessageId(), allowedTopics.toArray(new String[0]), allowedQoses.toArray(new QoS[0]));

			return super.subscribe(client, message);
		}

	}

}
