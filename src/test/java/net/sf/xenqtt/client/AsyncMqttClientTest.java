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

import java.util.concurrent.Executors;

import net.sf.xenqtt.message.ConnectReturnCode;

import org.junit.Test;

public class AsyncMqttClientTest {

	MqttClientConfig config = new MqttClientConfig().setReconnectionStrategy(new NullReconnectStrategy()).setConnectTimeoutSeconds(0)
			.setMessageResendIntervalSeconds(10);

	@Test
	public void testCtor_Executor() throws Exception {
		new AsyncMqttClient("tcp://q.m2m.io:1883", new TestAsyncClientListener(), Executors.newFixedThreadPool(1), config);
	}

	@Test
	public void testCtor_NoExecutor() throws Exception {
		new AsyncMqttClient("tcp://q.m2m.io:1883", new TestAsyncClientListener(), 1, config);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_Executor_BlankBrokerUri() throws Exception {
		new AsyncMqttClient("", new TestAsyncClientListener(), Executors.newFixedThreadPool(1), config);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_NoExecutor_BlankBrokerUri() throws Exception {
		new AsyncMqttClient("", new TestAsyncClientListener(), 1, config);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_Executor_NullBrokerUri() throws Exception {
		new AsyncMqttClient(null, new TestAsyncClientListener(), Executors.newFixedThreadPool(1), config);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_NoExecutor_NullBrokerUri() throws Exception {
		new AsyncMqttClient(null, new TestAsyncClientListener(), 1, config);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_Executor_NullAsyncClientListener() throws Exception {
		new AsyncMqttClient("tcp://q.m2m.io:1883", null, Executors.newFixedThreadPool(1), config);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_NoExecutor_NullAsyncClientListener() throws Exception {
		new AsyncMqttClient("tcp://q.m2m.io:1883", null, 1, config);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_Executor_NullExecutor() throws Exception {
		new AsyncMqttClient("tcp://q.m2m.io:1883", new TestAsyncClientListener(), null, config);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_NoExecutor_MessageHandlerThreadPoolSizeLessThanOne() throws Exception {
		new AsyncMqttClient("tcp://q.m2m.io:1883", new TestAsyncClientListener(), 0, config);
	}

	@Test
	public void testGetStats() {
		MessageStats stats = new AsyncMqttClient("tcp://q.m2m.io:1883", new TestAsyncClientListener(), 1, config).getStats(false);
		assertNotNull(stats);
		assertEquals(0, stats.getMessagesQueuedToSend());
		assertEquals(0, stats.getMessagesInFlight());
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
