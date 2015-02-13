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
package net.xenqtt.mockbroker;

import static org.junit.Assert.*;

import java.util.regex.Pattern;

import net.xenqtt.mockbroker.MockBroker;

import org.junit.After;
import org.junit.Test;

public class MockBrokerTest {

	MockBroker broker = new MockBroker(null, 15, 0, true, false, true, 50);

	@After
	public void after() {
		broker.shutdown(5000);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_InvalidMessageResendIntervalSeconds() {
		new MockBroker(null, -1, 1234, true, false, true, 50);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_InvalidPort_BelowRange() {
		new MockBroker(null, 1, -1, true, false, true, 50);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_InvalidPort_AboveRange() {
		new MockBroker(null, 1, 65536, true, false, true, 50);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_InvalidMaxInFLightMessages() {
		new MockBroker(null, 1, 65536, true, false, true, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testShutdown_InvalidMillis() {
		broker.shutdown(-1L);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testAddCredentials_NullUserName() {
		broker.addCredentials(null, "password");
	}

	@Test
	public void testGetPort() throws Exception {

		assertEquals(24156, new MockBroker(null, 15, 24156, true, false, true, 50).getPort());
	}

	@Test
	public void testUri() throws Exception {

		String uri = new MockBroker(null, 15, 24156, true, false, true, 50).getURI();
		assertTrue(Pattern.matches("tcp://\\d+\\.\\d+\\.\\d+\\.\\d+:24156", uri));
	}
}
