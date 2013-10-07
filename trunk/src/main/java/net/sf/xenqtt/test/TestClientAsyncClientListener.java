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
package net.sf.xenqtt.test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

import net.sf.xenqtt.Log;
import net.sf.xenqtt.client.AsyncClientListener;
import net.sf.xenqtt.client.MqttClient;
import net.sf.xenqtt.client.PublishMessage;
import net.sf.xenqtt.client.Subscription;
import net.sf.xenqtt.message.ConnectReturnCode;

/**
 * An {@link AsyncClientListener} that provides the {@link XenqttTestClient test client} with appropriate controls and stats monitoring as messages are
 * exchanged through an MQTT broker. This implementation allows for the test client to await at appropriate stages (connect, publish, and receive completion),
 * controls in-flight message limits (publish pathway), and helps to track and monitor stats.
 */
final class TestClientAsyncClientListener implements AsyncClientListener {

	private final XenqttTestClientStats stats;
	private final CountDownLatch connectedLatch;
	private final CountDownLatch publishCompleteLatch;
	private final CountDownLatch messageReceivedLatch;
	private final Semaphore inFlight;

	/**
	 * Create a new instance of this class.
	 * 
	 * @param stats
	 *            The {@link XenqttTestClientStats stats} that are being trended for the current test
	 * @param connectedLatch
	 *            The latch that is triggered after receipt of a CONNACK with a state of accepted
	 * @param publishCompleteLatch
	 *            A latch that is triggered each time a message is published
	 * @param messageReceivedLatch
	 *            A latch that is triggered each time a message is received
	 * @param inFlight
	 *            A {@link Semaphore semaphore} that is used to regular in-flight messages (publish pathway)
	 */
	TestClientAsyncClientListener(XenqttTestClientStats stats, CountDownLatch connectedLatch, CountDownLatch publishCompleteLatch,
			CountDownLatch messageReceivedLatch, Semaphore inFlight) {
		this.stats = stats;
		this.connectedLatch = connectedLatch;
		this.publishCompleteLatch = publishCompleteLatch;
		this.messageReceivedLatch = messageReceivedLatch;
		this.inFlight = inFlight;
	}

	/**
	 * @see net.sf.xenqtt.client.MqttClientListener#publishReceived(net.sf.xenqtt.client.MqttClient, net.sf.xenqtt.client.PublishMessage)
	 */
	@Override
	public void publishReceived(MqttClient client, PublishMessage message) {
		message.ack();
		stats.messageReceived(message);
		if (!message.isDuplicate()) {
			messageReceivedLatch.countDown();
		}
	}

	/**
	 * @see net.sf.xenqtt.client.MqttClientListener#disconnected(net.sf.xenqtt.client.MqttClient, java.lang.Throwable, boolean)
	 */
	@Override
	public void disconnected(MqttClient client, Throwable cause, boolean reconnecting) {
	}

	/**
	 * @see net.sf.xenqtt.client.AsyncClientListener#connected(net.sf.xenqtt.client.MqttClient, net.sf.xenqtt.message.ConnectReturnCode)
	 */
	@Override
	public void connected(MqttClient client, ConnectReturnCode returnCode) {
		if (returnCode != ConnectReturnCode.ACCEPTED) {
			Log.error("The broker refused the connection. Error code: %s", returnCode);
			System.err.println("The broker refused the connection. This error is not recoverable.");
			System.exit(-1);
		}

		if (connectedLatch != null) {
			connectedLatch.countDown();
		}
	}

	/**
	 * @see net.sf.xenqtt.client.AsyncClientListener#subscribed(net.sf.xenqtt.client.MqttClient, net.sf.xenqtt.client.Subscription[],
	 *      net.sf.xenqtt.client.Subscription[], boolean)
	 */
	@Override
	public void subscribed(MqttClient client, Subscription[] requestedSubscriptions, Subscription[] grantedSubscriptions, boolean requestsGranted) {
	}

	/**
	 * @see net.sf.xenqtt.client.AsyncClientListener#unsubscribed(net.sf.xenqtt.client.MqttClient, java.lang.String[])
	 */
	@Override
	public void unsubscribed(MqttClient client, String[] topics) {
	}

	/**
	 * @see net.sf.xenqtt.client.AsyncClientListener#published(net.sf.xenqtt.client.MqttClient, net.sf.xenqtt.client.PublishMessage)
	 */
	@Override
	public void published(MqttClient client, PublishMessage message) {
		stats.publishComplete(message);
		inFlight.release();
		publishCompleteLatch.countDown();
	}

}
