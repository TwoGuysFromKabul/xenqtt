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
