package net.sf.xenqtt.client;

import static org.junit.Assert.*;
import net.sf.xenqtt.message.ConnectReturnCode;

import org.junit.Test;

public class MqttClientFactoryTest {

	MqttClientFactory factory = new MqttClientFactory("tcp://q.m2m.io:1883", new NullReconnectStrategy(), 1, 10, 0);

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_BlankBrokerUri() {
		new MqttClientFactory("", new NullReconnectStrategy(), 1, 10, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_BlankBrokerUri_NonBlocking() {
		new MqttClientFactory("", new NullReconnectStrategy(), 1, 10);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_NullBrokerUri() {
		new MqttClientFactory(null, new NullReconnectStrategy(), 1, 10, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_NullBrokerUri_NonBlocking() {
		new MqttClientFactory(null, new NullReconnectStrategy(), 1, 10);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_MessageHandlerThreadPoolSizeZero() {
		new MqttClientFactory("tcp://q.m2m.io:1883", new NullReconnectStrategy(), 0, 10, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_MessageHandlerThreadPoolSizeZero_NonBlocking() {
		new MqttClientFactory("tcp://q.m2m.io:1883", new NullReconnectStrategy(), 0, 10);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_MessageResendIntervalSecondsLessThanZero() {
		new MqttClientFactory("tcp://q.m2m.io:1883", new NullReconnectStrategy(), 1, -1, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_MessageResendIntervalSecondsLessThanZero_NonBlocking() {
		new MqttClientFactory("tcp://q.m2m.io:1883", new NullReconnectStrategy(), 1, -1);
	}

	@Test
	public void testNewSynchronousMqttClient() {
		MqttClient client = factory.newSynchronousClient(new TestMqttClientListener());
		assertNotNull(client);
	}

	@Test
	public void testNewAsyncClient() {
		factory = new MqttClientFactory("tcp://q.m2m.io:1883", new NullReconnectStrategy(), 1, 1, -1);
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
