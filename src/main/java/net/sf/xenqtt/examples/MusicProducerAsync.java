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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import net.sf.xenqtt.client.AsyncClientListener;
import net.sf.xenqtt.client.AsyncMqttClient;
import net.sf.xenqtt.client.MqttClient;
import net.sf.xenqtt.client.PublishMessage;
import net.sf.xenqtt.client.Subscription;
import net.sf.xenqtt.message.ConnectReturnCode;
import net.sf.xenqtt.message.QoS;

import org.apache.log4j.Logger;

/**
 * Produces hit music from days gone by.
 */
public class MusicProducerAsync {

	private static final Logger log = Logger.getLogger(MusicProducerAsync.class);

	public static void main(String... args) throws Throwable {
		final CountDownLatch connectLatch = new CountDownLatch(1);
		final AtomicReference<ConnectReturnCode> connectReturnCode = new AtomicReference<ConnectReturnCode>();
		AsyncClientListener listener = new AsyncClientListener() {

			@Override
			public void publishReceived(MqttClient client, PublishMessage message) {
				log.warn("Received a message when no subscriptions were active. Check your broker ;)");
			}

			@Override
			public void disconnected(MqttClient client, Throwable cause, boolean reconnecting) {
				if (cause != null) {
					log.error("Disconnected from the broker due to an exception.", cause);
				} else {
					log.info("Disconnected from the broker.");
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
			public void subscribed(MqttClient client, Subscription[] requestedSubscriptions, Subscription[] grantedSubscriptions, boolean requestsGranted) {
			}

			@Override
			public void unsubscribed(MqttClient client, String[] topics) {
			}

			@Override
			public void published(MqttClient client, PublishMessage message) {
			}

		};

		// Build your client. This client is an asynchronous one so all interaction with the broker will be non-blocking.
		MqttClient client = new AsyncMqttClient("tcp://mqtt-broker:1883", listener, 5);
		try {
			// Connect to the broker. We will await the return code so that we know whether or not we can even begin publishing.
			client.connect("musicProducerAsync", false, "music-user", "music-pass");
			connectLatch.await();

			ConnectReturnCode returnCode = connectReturnCode.get();
			if (returnCode == null || returnCode != ConnectReturnCode.ACCEPTED) {
				// The broker bounced us. We are done.
				log.error("The broker rejected our attempt to connect. Reason: " + returnCode);
				return;
			}

			// Publish a musical catalog
			client.publish(new PublishMessage("grand/funk/railroad", QoS.AT_MOST_ONCE, "On Time"));
			client.publish(new PublishMessage("grand/funk/railroad", QoS.AT_MOST_ONCE, "E Pluribus Funk"));
			client.publish(new PublishMessage("jefferson/airplane", QoS.AT_MOST_ONCE, "Surrealistic Pillow"));
			client.publish(new PublishMessage("jefferson/airplane", QoS.AT_MOST_ONCE, "Crown of Creation"));
			client.publish(new PublishMessage("seventies/prog/rush", QoS.AT_MOST_ONCE, "2112"));
			client.publish(new PublishMessage("seventies/prog/rush", QoS.AT_MOST_ONCE, "A Farewell to Kings"));
			client.publish(new PublishMessage("seventies/prog/rush", QoS.AT_MOST_ONCE, "Hemispheres"));
		} catch (Exception ex) {
			log.error("An exception prevented the publishing of the full catalog.", ex);
		} finally {
			// We are done. Disconnect.
			if (!client.isClosed()) {
				client.disconnect();
			}
		}
	}

}
