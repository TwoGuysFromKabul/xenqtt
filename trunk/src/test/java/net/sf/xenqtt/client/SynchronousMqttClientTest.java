package net.sf.xenqtt.client;

import java.util.concurrent.Executors;

import org.junit.Test;

public class SynchronousMqttClientTest {

	@Test
	public void testCtor_NoExecutor() throws Exception {
		new SynchronousMqttClient("tcp://q.m2m.io:1883", new TestMqttClientListener(), new NullReconnectStrategy(), 1, 0, 0, 0);
	}

	@Test
	public void testCtor_Executor() throws Exception {
		new SynchronousMqttClient("tcp://q.m2m.io:1883", new TestMqttClientListener(), new NullReconnectStrategy(), Executors.newFixedThreadPool(1), 0, 0, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_Executor_BlankBrokerUri() throws Exception {
		new SynchronousMqttClient("", new TestMqttClientListener(), new NullReconnectStrategy(), Executors.newFixedThreadPool(1), 0, 0, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_NoExecutor_BlankBrokerUri() throws Exception {
		new SynchronousMqttClient("", new TestMqttClientListener(), new NullReconnectStrategy(), 1, 0, 0, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_Executor_NullBrokerUri() throws Exception {
		new SynchronousMqttClient(null, new TestMqttClientListener(), new NullReconnectStrategy(), Executors.newFixedThreadPool(1), 0, 0, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_NoExecutor_NullBrokerUri() throws Exception {
		new SynchronousMqttClient(null, new TestMqttClientListener(), new NullReconnectStrategy(), 1, 0, 0, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_Executor_NullMqttClientListener() throws Exception {
		new SynchronousMqttClient("tcp://q.m2m.io:1883", null, new NullReconnectStrategy(), Executors.newFixedThreadPool(1), 0, 0, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_NoExecutor_NullMqttClientListener() throws Exception {
		new SynchronousMqttClient("tcp://q.m2m.io:1883", null, new NullReconnectStrategy(), 1, 0, 0, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_Executor_NullReconnectionStrategy() throws Exception {
		new SynchronousMqttClient("tcp://q.m2m.io:1883", new TestMqttClientListener(), null, Executors.newFixedThreadPool(1), 0, 0, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_NoExecutor_NullReconnectionStrategy() throws Exception {
		new SynchronousMqttClient("tcp://q.m2m.io:1883", new TestMqttClientListener(), null, 1, 0, 0, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_NoExecutor_ThreadPoolSizeLessThanOne() throws Exception {
		new SynchronousMqttClient("tcp://q.m2m.io:1883", new TestMqttClientListener(), new NullReconnectStrategy(), 0, 0, 0, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_Executor_NullExecutor() throws Exception {
		new SynchronousMqttClient("tcp://q.m2m.io:1883", new TestMqttClientListener(), new NullReconnectStrategy(), null, 0, 0, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_Executor_MessageResendIntervalLessThanZero() throws Exception {
		new SynchronousMqttClient("tcp://q.m2m.io:1883", new TestMqttClientListener(), new NullReconnectStrategy(), Executors.newFixedThreadPool(1), 0, -1, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_NoExecutor_MessageResendIntervalLessThanZero() throws Exception {
		new SynchronousMqttClient("tcp://q.m2m.io:1883", new TestMqttClientListener(), new NullReconnectStrategy(), 1, 0, -1, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_Executor_ConnectTimeoutLessThanZero() throws Exception {
		new SynchronousMqttClient("tcp://q.m2m.io:1883", new TestMqttClientListener(), new NullReconnectStrategy(), Executors.newFixedThreadPool(1), -1, 0, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_NoExecutor_ConnectTimeoutLessThanZero() throws Exception {
		new SynchronousMqttClient("tcp://q.m2m.io:1883", new TestMqttClientListener(), new NullReconnectStrategy(), 1, -1, 0, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_Executor_BlockingTimeoutLessThanZero() throws Exception {
		new SynchronousMqttClient("tcp://q.m2m.io:1883", new TestMqttClientListener(), new NullReconnectStrategy(), Executors.newFixedThreadPool(1), 0, 0, -1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_NoExecutor_BlockingTimeoutLessThanZero() throws Exception {
		new SynchronousMqttClient("tcp://q.m2m.io:1883", new TestMqttClientListener(), new NullReconnectStrategy(), 1, 0, 0, -1);
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

}
