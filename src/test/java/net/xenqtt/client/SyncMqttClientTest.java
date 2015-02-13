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
package net.xenqtt.client;

import static org.junit.Assert.*;

import java.util.concurrent.Executors;

import net.xenqtt.client.MessageStats;
import net.xenqtt.client.MqttClient;
import net.xenqtt.client.MqttClientConfig;
import net.xenqtt.client.MqttClientListener;
import net.xenqtt.client.NullReconnectStrategy;
import net.xenqtt.client.PublishMessage;
import net.xenqtt.client.SyncMqttClient;

import org.junit.Test;

public class SyncMqttClientTest {

	MqttClientConfig config = new MqttClientConfig().setReconnectionStrategy(new NullReconnectStrategy()).setConnectTimeoutSeconds(0)
			.setMessageResendIntervalSeconds(5).setBlockingTimeoutSeconds(0);

	@Test
	public void testCtor_NoExecutor() throws Exception {
		new SyncMqttClient("tcp://q.m2m.io:1883", new TestMqttClientListener(), 1, config);
	}

	@Test
	public void testCtor_Executor() throws Exception {
		new SyncMqttClient("tcp://q.m2m.io:1883", new TestMqttClientListener(), Executors.newFixedThreadPool(1), config);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_Executor_BlankBrokerUri() throws Exception {
		new SyncMqttClient("", new TestMqttClientListener(), Executors.newFixedThreadPool(1), config);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_NoExecutor_BlankBrokerUri() throws Exception {
		new SyncMqttClient("", new TestMqttClientListener(), 1, config);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_Executor_NullBrokerUri() throws Exception {
		new SyncMqttClient(null, new TestMqttClientListener(), Executors.newFixedThreadPool(1), config);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_NoExecutor_NullBrokerUri() throws Exception {
		new SyncMqttClient(null, new TestMqttClientListener(), 1, config);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_Executor_NullMqttClientListener() throws Exception {
		new SyncMqttClient("tcp://q.m2m.io:1883", null, Executors.newFixedThreadPool(1), config);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_NoExecutor_NullMqttClientListener() throws Exception {
		new SyncMqttClient("tcp://q.m2m.io:1883", null, 1, config);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_NoExecutor_ThreadPoolSizeLessThanOne() throws Exception {
		new SyncMqttClient("tcp://q.m2m.io:1883", new TestMqttClientListener(), 0, config);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_Executor_NullExecutor() throws Exception {
		new SyncMqttClient("tcp://q.m2m.io:1883", new TestMqttClientListener(), null, config);
	}

	@Test
	public void testGetStats() {
		MessageStats stats = new SyncMqttClient("tcp://q.m2m.io:1883", new TestMqttClientListener(), 1, config).getStats(false);
		assertNotNull(stats);
		assertEquals(0, stats.getMessagesQueuedToSend());
		assertEquals(0, stats.getMessagesInFlight());
	}

	private static final class TestMqttClientListener implements MqttClientListener {

		/**
		 * @see net.xenqtt.client.MqttClientListener#publishReceived(net.xenqtt.client.MqttClient, net.xenqtt.client.PublishMessage)
		 */
		@Override
		public void publishReceived(MqttClient client, PublishMessage message) {
		}

		/**
		 * @see net.xenqtt.client.MqttClientListener#disconnected(net.xenqtt.client.MqttClient, java.lang.Throwable, boolean)
		 */
		@Override
		public void disconnected(MqttClient client, Throwable cause, boolean reconnecting) {
		}

	}

}
