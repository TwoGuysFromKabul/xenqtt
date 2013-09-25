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
package net.sf.xenqtt.integration;

import net.sf.xenqtt.mockbroker.MockBroker;

import org.junit.After;
import org.junit.Before;
import org.mockito.MockitoAnnotations;

public class MockBrokerIT extends AsyncMqttClientIT {

	MockBroker broker;
	String brokerUri;

	@Override
	@Before
	public void before() {

		MockitoAnnotations.initMocks(this);

		broker = new MockBroker(null, 15, 0, true);
		broker.init();
		brokerUri = "tcp://localhost:" + broker.getPort();
		validBrokerUri = brokerUri;
		badCredentialsUri = brokerUri;
		broker.shutdown(1000);

		super.before();
	}

	@Override
	@After
	public void after() {

		super.after();
		broker.shutdown(5000);
	}
}
