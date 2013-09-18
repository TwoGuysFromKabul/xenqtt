package net.sf.xenqtt.integration;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.net.ConnectException;
import java.nio.channels.UnresolvedAddressException;

import net.sf.xenqtt.MqttException;
import net.sf.xenqtt.client.AsyncClientListener;
import net.sf.xenqtt.client.AsyncMqttClient;
import net.sf.xenqtt.client.ReconnectionStrategy;
import net.sf.xenqtt.message.ConnectReturnCode;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AsyncMqttClientIT {

	@Mock AsyncClientListener listener;
	@Mock ReconnectionStrategy reconnectionStrategy;

	AsyncMqttClient client;

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void test() {
		fail("Not yet implemented");
	}

	@Test
	public void testInvalidScheme() throws Exception {

		try {
			client = new AsyncMqttClient("ftp://foo:1883", listener, reconnectionStrategy, 5, 5);
			fail("expected exception");
		} catch (MqttException e) {
			assertEquals("Invalid broker URI (scheme must be 'tcp'): ftp://foo:1883", e.getMessage());
		}

		verifyZeroInteractions(listener, reconnectionStrategy);
	}

	@Test
	public void testInvalidHost() throws Exception {

		Throwable thrown = null;
		try {
			client = new AsyncMqttClient("tcp://foo:1883", listener, reconnectionStrategy, 5, 5);
			fail("expected exception");
		} catch (MqttException e) {
			thrown = e.getCause();
			assertEquals(UnresolvedAddressException.class, thrown.getClass());
		}

		verify(listener, timeout(1000)).disconnected(any(AsyncMqttClient.class), same(thrown), eq(false));
		verifyZeroInteractions(reconnectionStrategy);
	}

	// This test can take over a minute to run so ignore it by default
	@Ignore
	@Test
	public void testInvalidPort() throws Exception {

		client = new AsyncMqttClient("tcp://test.mosquitto.org:1234", listener, reconnectionStrategy, 5, 5);

		verify(listener, timeout(100000)).disconnected(eq(client), any(ConnectException.class), eq(false));

		verifyZeroInteractions(reconnectionStrategy);
	}

	@Test
	public void testConnectDisconnect() throws Exception {

		client = new AsyncMqttClient("tcp://test.mosquitto.org:1883", listener, reconnectionStrategy, 5, 5);
		client.connect("testclient1", false, 90);
		verify(listener, timeout(1000)).connected(client, ConnectReturnCode.ACCEPTED);
		verify(reconnectionStrategy, timeout(1000)).connectionEstablished();

		client.disconnect();
		verify(listener, timeout(1000)).disconnected(eq(client), isNull(Throwable.class), eq(false));
		verifyNoMoreInteractions(reconnectionStrategy);
	}
}
