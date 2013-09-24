package net.sf.xenqtt.client;

import java.util.concurrent.Executors;

import net.sf.xenqtt.message.ConnectReturnCode;

import org.junit.Test;

public class AsyncMqttClientTest {

	@Test
	public void testCtor_Executor() throws Exception {
		new AsyncMqttClient("tcp://q.m2m.io:1883", new TestAsyncClientListener(), new NullReconnectStrategy(), Executors.newFixedThreadPool(1), 0);
	}

	@Test
	public void testCtor_NoExecutor() throws Exception {
		new AsyncMqttClient("tcp://q.m2m.io:1883", new TestAsyncClientListener(), new NullReconnectStrategy(), 1, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_BlankBrokerUri() throws Exception {
		new AsyncMqttClient("", new TestAsyncClientListener(), new NullReconnectStrategy(), Executors.newFixedThreadPool(1), 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_NullBrokerUri() throws Exception {
		new AsyncMqttClient(null, new TestAsyncClientListener(), new NullReconnectStrategy(), Executors.newFixedThreadPool(1), 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_NullAsyncClientListener() throws Exception {
		new AsyncMqttClient("tcp://q.m2m.io:1883", null, new NullReconnectStrategy(), Executors.newFixedThreadPool(1), 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_NullReconnectStrategy() throws Exception {
		new AsyncMqttClient("tcp://q.m2m.io:1883", new TestAsyncClientListener(), null, Executors.newFixedThreadPool(1), 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_NullExecutor() throws Exception {
		new AsyncMqttClient("tcp://q.m2m.io:1883", new TestAsyncClientListener(), new NullReconnectStrategy(), null, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_MessageHandlerThreadPoolSizeLessThanOne() throws Exception {
		new AsyncMqttClient("tcp://q.m2m.io:1883", new TestAsyncClientListener(), new NullReconnectStrategy(), 0, 0);
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
