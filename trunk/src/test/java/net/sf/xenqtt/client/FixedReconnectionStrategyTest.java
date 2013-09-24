package net.sf.xenqtt.client;

import static org.junit.Assert.*;

import org.junit.Test;

public class FixedReconnectionStrategyTest {

	FixedReconnectionStrategy strategy = new FixedReconnectionStrategy(7000, 3);

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_ReconnectMillisZero() {
		new FixedReconnectionStrategy(0, 1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_ReconnectAttemptsLessThanZero() {
		new FixedReconnectionStrategy(7000, -1);
	}

	@Test
	public void testConnectionLost() {
		assertEquals(7000, strategy.connectionLost(null, null));
		assertEquals(7000, strategy.connectionLost(null, null));
		assertEquals(7000, strategy.connectionLost(null, null));
		assertEquals(-1, strategy.connectionLost(null, null));
		assertEquals(-1, strategy.connectionLost(null, null));
	}

	@Test
	public void testConnectionEstablished() {
		assertEquals(7000, strategy.connectionLost(null, null));
		assertEquals(7000, strategy.connectionLost(null, null));
		assertEquals(7000, strategy.connectionLost(null, null));
		assertEquals(-1, strategy.connectionLost(null, null));
		assertEquals(-1, strategy.connectionLost(null, null));

		strategy.connectionEstablished();
		assertEquals(7000, strategy.connectionLost(null, null));
		assertEquals(7000, strategy.connectionLost(null, null));
		assertEquals(7000, strategy.connectionLost(null, null));
		assertEquals(-1, strategy.connectionLost(null, null));
		assertEquals(-1, strategy.connectionLost(null, null));
	}

	@Test
	public void testClone() throws Exception {

		assertEquals(7000, strategy.connectionLost(null, null));
		assertEquals(7000, strategy.connectionLost(null, null));
		assertEquals(7000, strategy.connectionLost(null, null));
		assertEquals(-1, strategy.connectionLost(null, null));
		assertEquals(-1, strategy.connectionLost(null, null));

		ReconnectionStrategy clone = strategy.clone();
		assertNotSame(strategy, clone);

		assertEquals(7000, clone.connectionLost(null, null));
		assertEquals(7000, clone.connectionLost(null, null));
		assertEquals(7000, clone.connectionLost(null, null));
		assertEquals(-1, clone.connectionLost(null, null));
		assertEquals(-1, clone.connectionLost(null, null));
	}
}
