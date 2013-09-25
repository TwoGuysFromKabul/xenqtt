package net.sf.xenqtt.mockbroker;

import org.junit.Test;

public class MockBrokerTest {

	MockBroker broker = new MockBroker(null, 15, 0, true);

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_InvalidMessageResendIntervalSeconds() {
		new MockBroker(null, -1, 1234, true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_InvalidPort_BelowRange() {
		new MockBroker(null, 1, -1, true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_InvalidPort_AboveRange() {
		new MockBroker(null, 1, 65536, true);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testShutdown_InvalidMillis() {
		broker.shutdown(-1L);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAddCredentials_NullUserName() {
		broker.addCredentials(null, "password");
	}
}
