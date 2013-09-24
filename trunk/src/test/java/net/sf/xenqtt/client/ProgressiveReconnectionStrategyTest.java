package net.sf.xenqtt.client;

import static org.junit.Assert.*;

import org.junit.Test;

public class ProgressiveReconnectionStrategyTest {

	ProgressiveReconnectionStrategy strategy = new ProgressiveReconnectionStrategy(1000, 7, 5);

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_BaseMillisZero() {
		new ProgressiveReconnectionStrategy(0, 7, 5);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_ProgressiveIntervalZero() {
		new ProgressiveReconnectionStrategy(1000, 0, 5);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_RetriesLessThanZero() {
		new ProgressiveReconnectionStrategy(1000, 7, -1);
	}

	@Test
	public void testConnectionLost() {
		assertEquals(1000, strategy.connectionLost(null, null));
		assertEquals(7000, strategy.connectionLost(null, null));
		assertEquals(49000, strategy.connectionLost(null, null));
		assertEquals(343000, strategy.connectionLost(null, null));
		assertEquals(2401000, strategy.connectionLost(null, null));
		assertEquals(-1, strategy.connectionLost(null, null));
		assertEquals(-1, strategy.connectionLost(null, null));
	}

	@Test
	public void testConnectionEstablished() {
		assertEquals(1000, strategy.connectionLost(null, null));
		assertEquals(7000, strategy.connectionLost(null, null));
		assertEquals(49000, strategy.connectionLost(null, null));
		assertEquals(343000, strategy.connectionLost(null, null));
		assertEquals(2401000, strategy.connectionLost(null, null));
		assertEquals(-1, strategy.connectionLost(null, null));
		assertEquals(-1, strategy.connectionLost(null, null));

		strategy.connectionEstablished();
		assertEquals(1000, strategy.connectionLost(null, null));
		assertEquals(7000, strategy.connectionLost(null, null));
		assertEquals(49000, strategy.connectionLost(null, null));
		assertEquals(343000, strategy.connectionLost(null, null));
		assertEquals(2401000, strategy.connectionLost(null, null));
		assertEquals(-1, strategy.connectionLost(null, null));
		assertEquals(-1, strategy.connectionLost(null, null));
	}

	@Test
	public void testClone() throws Exception {

		assertEquals(1000, strategy.connectionLost(null, null));
		assertEquals(7000, strategy.connectionLost(null, null));
		assertEquals(49000, strategy.connectionLost(null, null));
		assertEquals(343000, strategy.connectionLost(null, null));
		assertEquals(2401000, strategy.connectionLost(null, null));
		assertEquals(-1, strategy.connectionLost(null, null));
		assertEquals(-1, strategy.connectionLost(null, null));

		ReconnectionStrategy clone = strategy.clone();
		assertNotSame(strategy, clone);

		assertEquals(1000, clone.connectionLost(null, null));
		assertEquals(7000, clone.connectionLost(null, null));
		assertEquals(49000, clone.connectionLost(null, null));
		assertEquals(343000, clone.connectionLost(null, null));
		assertEquals(2401000, clone.connectionLost(null, null));
		assertEquals(-1, clone.connectionLost(null, null));
		assertEquals(-1, clone.connectionLost(null, null));
	}
}
