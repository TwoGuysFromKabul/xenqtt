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

import java.util.concurrent.Executors;

import net.sf.xenqtt.message.ConnectReturnCode;

import org.junit.Test;

public class AsyncMqttClientTest {

	@Test
	public void testCtor_Executor() throws Exception {
		new AsyncMqttClient("tcp://q.m2m.io:1883", new TestAsyncClientListener(), new NullReconnectStrategy(), Executors.newFixedThreadPool(1), 0, 5);
	}

	@Test
	public void testCtor_NoExecutor() throws Exception {
		new AsyncMqttClient("tcp://q.m2m.io:1883", new TestAsyncClientListener(), new NullReconnectStrategy(), 1, 0, 5);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_Executor_BlankBrokerUri() throws Exception {
		new AsyncMqttClient("", new TestAsyncClientListener(), new NullReconnectStrategy(), Executors.newFixedThreadPool(1), 0, 5);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_NoExecutor_BlankBrokerUri() throws Exception {
		new AsyncMqttClient("", new TestAsyncClientListener(), new NullReconnectStrategy(), 1, 0, 5);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_Executor_NullBrokerUri() throws Exception {
		new AsyncMqttClient(null, new TestAsyncClientListener(), new NullReconnectStrategy(), Executors.newFixedThreadPool(1), 0, 5);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_NoExecutor_NullBrokerUri() throws Exception {
		new AsyncMqttClient(null, new TestAsyncClientListener(), new NullReconnectStrategy(), 1, 0, 5);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_Executor_NullAsyncClientListener() throws Exception {
		new AsyncMqttClient("tcp://q.m2m.io:1883", null, new NullReconnectStrategy(), Executors.newFixedThreadPool(1), 0, 5);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_NoExecutor_NullAsyncClientListener() throws Exception {
		new AsyncMqttClient("tcp://q.m2m.io:1883", null, new NullReconnectStrategy(), 1, 0, 5);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_Executor_NullReconnectStrategy() throws Exception {
		new AsyncMqttClient("tcp://q.m2m.io:1883", new TestAsyncClientListener(), null, Executors.newFixedThreadPool(1), 0, 5);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_NoExecutor_NullReconnectStrategy() throws Exception {
		new AsyncMqttClient("tcp://q.m2m.io:1883", new TestAsyncClientListener(), null, 1, 0, 5);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_Executor_NullExecutor() throws Exception {
		new AsyncMqttClient("tcp://q.m2m.io:1883", new TestAsyncClientListener(), new NullReconnectStrategy(), null, 0, 5);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_NoExecutor_MessageHandlerThreadPoolSizeLessThanOne() throws Exception {
		new AsyncMqttClient("tcp://q.m2m.io:1883", new TestAsyncClientListener(), new NullReconnectStrategy(), 0, 0, 5);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_Executor_MessageResendIntervalLessThanTwo() throws Exception {
		new AsyncMqttClient("tcp://q.m2m.io:1883", new TestAsyncClientListener(), new NullReconnectStrategy(), Executors.newFixedThreadPool(1), 0, 1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_NoExecutor_MessageResendIntervalLessThanTwo() throws Exception {
		new AsyncMqttClient("tcp://q.m2m.io:1883", new TestAsyncClientListener(), new NullReconnectStrategy(), 1, 0, 1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_Executor_ConnectTimeoutLessThanZero() throws Exception {
		new AsyncMqttClient("tcp://q.m2m.io:1883", new TestAsyncClientListener(), new NullReconnectStrategy(), Executors.newFixedThreadPool(1), -1, 5);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_NoExecutor_ConnectTimeoutLessThanZero() throws Exception {
		new AsyncMqttClient("tcp://q.m2m.io:1883", new TestAsyncClientListener(), new NullReconnectStrategy(), 1, -1, 5);
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
