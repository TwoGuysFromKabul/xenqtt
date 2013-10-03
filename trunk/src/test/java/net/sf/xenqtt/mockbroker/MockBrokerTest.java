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
package net.sf.xenqtt.mockbroker;

import org.junit.Test;

public class MockBrokerTest {

	MockBroker broker = new MockBroker(null, 15, 0, true, true, 50);

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_InvalidMessageResendIntervalSeconds() {
		new MockBroker(null, -1, 1234, true, true, 50);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_InvalidPort_BelowRange() {
		new MockBroker(null, 1, -1, true, true, 50);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_InvalidPort_AboveRange() {
		new MockBroker(null, 1, 65536, true, true, 50);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_InvalidMaxInFLightMessages() {
		new MockBroker(null, 1, 65536, true, true, 0);
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
