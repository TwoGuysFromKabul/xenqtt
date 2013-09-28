package net.sf.xenqtt.client;

import static org.junit.Assert.*;

import org.junit.Test;

public class MqttClientConfigTest {

	MqttClientConfig config = new MqttClientConfig();

	@Test
	public void testDefaults() {
		fail("Not yet implemented");
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
