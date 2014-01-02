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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sf.xenqtt.client.MqttClient;
import net.sf.xenqtt.client.MqttClientListener;
import net.sf.xenqtt.client.PublishMessage;
import net.sf.xenqtt.client.Subscription;
import net.sf.xenqtt.client.SyncMqttClient;
import net.sf.xenqtt.message.ConnectReturnCode;
import net.sf.xenqtt.message.QoS;

import org.apache.log4j.Logger;

/**
 * Builds music catalogs from years gone by.
 */
public class MusicSubscriber {

	private static final Logger log = Logger.getLogger(MusicSubscriber.class);

	public static void main(String... args) throws Throwable {
		final List<String> catalog = Collections.synchronizedList(new ArrayList<String>());
		MqttClientListener listener = new MqttClientListener() {

			@Override
			public void publishReceived(MqttClient client, PublishMessage message) {
				catalog.add(message.getPayloadString());
				message.ack();
			}

			@Override
			public void disconnected(MqttClient client, Throwable cause, boolean reconnecting) {
				if (cause != null) {
					log.error("Disconnected from the broker due to an exception.", cause);
				} else {
					log.info("Disconnecting from the broker.");
				}

				if (reconnecting) {
					log.info("Attempting to reconnect to the broker.");
				}
			}

		};

		// Build your client. This client is a synchronous one so all interaction with the broker will block until said interaction completes.
		SyncMqttClient client = new SyncMqttClient("tcp://localhost:1883", listener, 5);
		try {
			// Connect to the broker with a specific client ID. Only if the broker accepted the connection shall we proceed.
			ConnectReturnCode returnCode = client.connect("musicLover", true);
			if (returnCode != ConnectReturnCode.ACCEPTED) {
				log.error("Unable to connect to the MQTT broker. Reason: " + returnCode);
				return;
			}

			// Create your subscriptions. In this case we want to build up a catalog of classic rock.
			List<Subscription> subscriptions = new ArrayList<Subscription>();
			subscriptions.add(new Subscription("grand/funk/railroad", QoS.AT_MOST_ONCE));
			subscriptions.add(new Subscription("jefferson/airplane", QoS.AT_MOST_ONCE));
			subscriptions.add(new Subscription("seventies/prog/#", QoS.AT_MOST_ONCE));
			client.subscribe(subscriptions);

			// Build up your catalog. After a while you've waited long enough so move on.
			try {
				Thread.sleep(30000);
			} catch (InterruptedException ignore) {
			}

			// Report on what we have found.
			for (String record : catalog) {
				log.debug("Got a record: " + record);
			}

			// We are done. Unsubscribe from further updates.
			List<String> topics = new ArrayList<String>();
			for (Subscription subscription : subscriptions) {
				topics.add(subscription.getTopic());
			}
			client.unsubscribe(topics);
		} catch (Exception ex) {
			log.error("An unexpected exception has occurred.", ex);
		} finally {
			if (!client.isClosed()) {
				client.disconnect();
			}
		}
	}

}
