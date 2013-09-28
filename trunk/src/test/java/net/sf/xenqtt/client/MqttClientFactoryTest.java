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

import static org.junit.Assert.*;
import net.sf.xenqtt.message.ConnectReturnCode;

import org.junit.Test;

public class MqttClientFactoryTest {

	MqttClientConfig config = new MqttClientConfig().setReconnectionStrategy(new NullReconnectStrategy()).setMessageResendIntervalSeconds(10)
			.setBlockingTimeoutSeconds(0).setConnectTimeoutSeconds(0);

	MqttClientFactory factory = new MqttClientFactory("tcp://q.m2m.io:1883", 1, true, config);

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_Blocking_BlankBrokerUri() {
		new MqttClientFactory("", 1, true, config);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_NonBlocking_BlankBrokerUri() {
		new MqttClientFactory("", 1, false, config);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_Blocking_NullBrokerUri() {
		new MqttClientFactory(null, 1, true, config);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_NonBlocking_NullBrokerUri() {
		new MqttClientFactory(null, 1, false, config);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_Blocking_MessageHandlerThreadPoolSizeZero() {
		new MqttClientFactory("tcp://q.m2m.io:1883", 0, true, config);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_NonBlocking_MessageHandlerThreadPoolSizeZero() {
		new MqttClientFactory("tcp://q.m2m.io:1883", 0, false, config);
	}

	@Test
	public void testNewSynchronousMqttClient() {
		MqttClient client = factory.newSynchronousClient(new TestMqttClientListener());
		assertNotNull(client);
	}

	@Test
	public void testNewAsyncClient() {
		factory = new MqttClientFactory("tcp://q.m2m.io:1883", 1, false, config);
		assertNotNull(factory.newAsyncClient(new TestAsyncClientListener()));
	}

	@Test(expected = IllegalStateException.class)
	public void testNewAsynchronousMqttClient_InvalidFactory() {
		factory.newAsyncClient(new TestAsyncClientListener());
	}

	private static final class TestMqttClientListener implements MqttClientListener {

		/**
		 * @see net.sf.xenqtt.client.MqttClientListener#publishReceived(net.sf.xenqtt.client.MqttClient, net.sf.xenqtt.client.PublishMessage)
		 */
		@Override
		public void publishReceived(MqttClient client, PublishMessage message) {
		}

		/**
		 * @see net.sf.xenqtt.client.MqttClientListener#disconnected(net.sf.xenqtt.client.MqttClient, java.lang.Throwable, boolean)
		 */
		@Override
		public void disconnected(MqttClient client, Throwable cause, boolean reconnecting) {
		}

	}

	private static final class TestAsyncClientListener implements AsyncClientListener {

		/**
		 * @see net.sf.xenqtt.client.MqttClientListener#publishReceived(net.sf.xenqtt.client.MqttClient, net.sf.xenqtt.client.PublishMessage)
		 */
		@Override
		public void publishReceived(MqttClient client, PublishMessage message) {
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
		}

	}

}
