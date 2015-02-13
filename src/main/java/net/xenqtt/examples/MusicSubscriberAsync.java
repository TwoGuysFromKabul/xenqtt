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
package net.xenqtt.examples;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import net.xenqtt.client.AsyncClientListener;
import net.xenqtt.client.AsyncMqttClient;
import net.xenqtt.client.MqttClient;
import net.xenqtt.client.PublishMessage;
import net.xenqtt.client.Subscription;
import net.xenqtt.message.ConnectReturnCode;
import net.xenqtt.message.QoS;

import org.apache.log4j.Logger;

/**
 * Builds music catalogs from years gone by.
 */
public class MusicSubscriberAsync {

	private static final Logger log = Logger.getLogger(MusicSubscriberAsync.class);

	public static void main(String... args) throws Throwable {
		final CountDownLatch connectLatch = new CountDownLatch(1);
		final AtomicReference<ConnectReturnCode> connectReturnCode = new AtomicReference<ConnectReturnCode>();
		final List<String> catalog = Collections.synchronizedList(new ArrayList<String>());
		AsyncClientListener listener = new AsyncClientListener() {

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

			@Override
			public void connected(MqttClient client, ConnectReturnCode returnCode) {
				connectReturnCode.set(returnCode);
				connectLatch.countDown();
			}

			@Override
			public void published(MqttClient client, PublishMessage message) {
				// We do not publish so this should never be called, in theory ;).
			}

			@Override
			public void subscribed(MqttClient client, Subscription[] requestedSubscriptions, Subscription[] grantedSubscriptions, boolean requestsGranted) {
				if (!requestsGranted) {
					log.error("Unable to subscribe to the following subscriptions: " + Arrays.toString(requestedSubscriptions));
				}

				log.debug("Granted subscriptions: " + Arrays.toString(grantedSubscriptions));
			}

			@Override
			public void unsubscribed(MqttClient client, String[] topics) {
				log.debug("Unsubscribed from the following topics: " + Arrays.toString(topics));
			}

		};

		// Build your client. This client is an asynchronous one so all interaction with the broker will be non-blocking.
		AsyncMqttClient client = new AsyncMqttClient("tcp://mqtt-broker:1883", listener, 5);
		try {
			// Connect to the broker with a specific client ID. Only if the broker accepted the connection shall we proceed.
			client.connect("musicLover", true);
			ConnectReturnCode returnCode = connectReturnCode.get();
			if (returnCode == null || returnCode != ConnectReturnCode.ACCEPTED) {
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

			// We are done. Unsubscribe at this time.
			List<String> topics = new ArrayList<String>();
			for (Subscription subscription : subscriptions) {
				topics.add(subscription.getTopic());
			}
			client.unsubscribe(topics);
		} finally {
			if (!client.isClosed()) {
				client.disconnect();
			}
		}
	}

}
