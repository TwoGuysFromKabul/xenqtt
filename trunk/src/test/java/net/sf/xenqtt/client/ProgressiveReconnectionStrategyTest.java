package net.sf.xenqtt.client;

import static org.junit.Assert.*;

import org.junit.Test;

public class ProgressiveReconnectionStrategyTest {

	ProgressiveReconnectionStrategy strategy = new ProgressiveReconnectionStrategy(1000, 7, 5);

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

}
