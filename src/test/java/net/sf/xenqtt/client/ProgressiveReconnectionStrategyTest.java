/**
    Copyright 2013 James McClure

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package net.sf.xenqtt.client;

import static org.junit.Assert.*;

import org.junit.Test;

public class ProgressiveReconnectionStrategyTest {

	ProgressiveReconnectionStrategy strategy = new ProgressiveReconnectionStrategy(1000, 7, 5, 340000);

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_BaseMillisZero() {
		new ProgressiveReconnectionStrategy(0, 7, 5, 20);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_ProgressiveIntervalZero() {
		new ProgressiveReconnectionStrategy(1000, 0, 5, 20);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_RetriesLessThanZero() {
		new ProgressiveReconnectionStrategy(1000, 7, -1, 20);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_MaxMillisLessThanBaseMillis() {
		new ProgressiveReconnectionStrategy(1000, 7, -1, 999);
	}

	@Test
	public void testConnectionLost() {
		assertEquals(1000, strategy.connectionLost(null, null));
		assertEquals(7000, strategy.connectionLost(null, null));
		assertEquals(49000, strategy.connectionLost(null, null));
		assertEquals(340000, strategy.connectionLost(null, null));
		assertEquals(340000, strategy.connectionLost(null, null));
		assertEquals(-1, strategy.connectionLost(null, null));
		assertEquals(-1, strategy.connectionLost(null, null));
	}

	@Test
	public void testConnectionEstablished() {
		assertEquals(1000, strategy.connectionLost(null, null));
		assertEquals(7000, strategy.connectionLost(null, null));
		assertEquals(49000, strategy.connectionLost(null, null));
		assertEquals(340000, strategy.connectionLost(null, null));
		assertEquals(340000, strategy.connectionLost(null, null));
		assertEquals(-1, strategy.connectionLost(null, null));
		assertEquals(-1, strategy.connectionLost(null, null));

		strategy.connectionEstablished();
		assertEquals(1000, strategy.connectionLost(null, null));
		assertEquals(7000, strategy.connectionLost(null, null));
		assertEquals(49000, strategy.connectionLost(null, null));
		assertEquals(340000, strategy.connectionLost(null, null));
		assertEquals(340000, strategy.connectionLost(null, null));
		assertEquals(-1, strategy.connectionLost(null, null));
		assertEquals(-1, strategy.connectionLost(null, null));
	}

	@Test
	public void testClone() throws Exception {

		assertEquals(1000, strategy.connectionLost(null, null));
		assertEquals(7000, strategy.connectionLost(null, null));
		assertEquals(49000, strategy.connectionLost(null, null));
		assertEquals(340000, strategy.connectionLost(null, null));
		assertEquals(340000, strategy.connectionLost(null, null));
		assertEquals(-1, strategy.connectionLost(null, null));
		assertEquals(-1, strategy.connectionLost(null, null));

		ReconnectionStrategy clone = strategy.clone();
		assertNotSame(strategy, clone);

		assertEquals(1000, clone.connectionLost(null, null));
		assertEquals(7000, clone.connectionLost(null, null));
		assertEquals(49000, clone.connectionLost(null, null));
		assertEquals(340000, clone.connectionLost(null, null));
		assertEquals(340000, clone.connectionLost(null, null));
		assertEquals(-1, clone.connectionLost(null, null));
		assertEquals(-1, clone.connectionLost(null, null));
	}
}
