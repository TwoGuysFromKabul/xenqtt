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

	@Test
	public void testClone() throws Exception {

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

		MqttClientConfig configClone = config.clone();

		assertEquals(0, configClone.getBlockingTimeoutSeconds());
		assertEquals(30000, configClone.getConnectTimeoutMillis());
		assertEquals(30, configClone.getConnectTimeoutSeconds());
		assertEquals(300, configClone.getKeepAliveSeconds());
		assertEquals(30, configClone.getMessageResendIntervalSeconds());
		assertEquals(50, ((ProgressiveReconnectionStrategy) configClone.getReconnectionStrategy()).getBaseReconnectMillis());
		assertEquals(0, ((ProgressiveReconnectionStrategy) configClone.getReconnectionStrategy()).getCurrentRetry());
		assertEquals(Integer.MAX_VALUE, ((ProgressiveReconnectionStrategy) configClone.getReconnectionStrategy()).getMaxNumberOfReconnects());
		assertEquals(30000, ((ProgressiveReconnectionStrategy) configClone.getReconnectionStrategy()).getMaxReconnectMillis());
		assertEquals(5, ((ProgressiveReconnectionStrategy) configClone.getReconnectionStrategy()).getProgressiveFactor());

		config.setBlockingTimeoutSeconds(1);
		config.setConnectTimeoutSeconds(2);
		config.setKeepAliveSeconds(3);
		config.setMessageResendIntervalSeconds(4);
		config.getReconnectionStrategy().connectionLost(null, null);

		assertEquals(1, config.getBlockingTimeoutSeconds());
		assertEquals(2000, config.getConnectTimeoutMillis());
		assertEquals(2, config.getConnectTimeoutSeconds());
		assertEquals(3, config.getKeepAliveSeconds());
		assertEquals(4, config.getMessageResendIntervalSeconds());
		assertEquals(1, ((ProgressiveReconnectionStrategy) config.getReconnectionStrategy()).getCurrentRetry());

		assertEquals(0, configClone.getBlockingTimeoutSeconds());
		assertEquals(30000, configClone.getConnectTimeoutMillis());
		assertEquals(30, configClone.getConnectTimeoutSeconds());
		assertEquals(300, configClone.getKeepAliveSeconds());
		assertEquals(30, configClone.getMessageResendIntervalSeconds());
		assertEquals(50, ((ProgressiveReconnectionStrategy) configClone.getReconnectionStrategy()).getBaseReconnectMillis());
		assertEquals(0, ((ProgressiveReconnectionStrategy) configClone.getReconnectionStrategy()).getCurrentRetry());
		assertEquals(Integer.MAX_VALUE, ((ProgressiveReconnectionStrategy) configClone.getReconnectionStrategy()).getMaxNumberOfReconnects());
		assertEquals(30000, ((ProgressiveReconnectionStrategy) configClone.getReconnectionStrategy()).getMaxReconnectMillis());
		assertEquals(5, ((ProgressiveReconnectionStrategy) configClone.getReconnectionStrategy()).getProgressiveFactor());
	}
}
