package net.sf.xenqtt.client;

import static org.junit.Assert.*;

import org.junit.Test;

public class MqttClientConfigTest {

	MqttClientConfig config = new MqttClientConfig();

	@Test
	public void testDefaults() {

		assertEquals(0, config.getBlockingTimeoutSeconds());
		assertEquals(30000, config.getConnectTimeoutMillis());
		assertEquals(30, config.getConnectTimeoutSeconds());
		assertEquals(300, config.getKeepAliveSeconds());
		assertEquals(30, config.getMessageResendIntervalSeconds());
		assertEquals(50, ((ProgressiveReconnectionStrategy) config.getReconnectionStrategy()).getBaseReconnectMillis());
		assertEquals(0, ((ProgressiveReconnectionStrategy) config.getReconnectionStrategy()).getCurrentRetry());
		assertEquals(Integer.MAX_VALUE, ((ProgressiveReconnectionStrategy) config.getReconnectionStrategy()).getMaxNumberOfReconnects());
		assertEquals(30000, ((ProgressiveReconnectionStrategy) config.getReconnectionStrategy()).getMaxReconnectMillis());
		assertEquals(5, ((ProgressiveReconnectionStrategy) config.getReconnectionStrategy()).getProgressiveFactor());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSetReconnectStrategy_NullReconnectStrategy() throws Exception {
		config.setReconnectionStrategy(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSetMessageResendInterval_LessThanTwo() throws Exception {
		config.setMessageResendIntervalSeconds(1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSetConnectTimeout_LessThanZero() throws Exception {
		config.setConnectTimeoutSeconds(-1);
	}
}
