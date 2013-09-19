package net.sf.xenqtt.integration;

import static org.junit.Assert.*;
import static org.mockito.AdditionalMatchers.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.net.ConnectException;
import java.nio.channels.UnresolvedAddressException;
import java.util.Arrays;
import java.util.List;

import net.sf.xenqtt.MqttException;
import net.sf.xenqtt.client.AsyncClientListener;
import net.sf.xenqtt.client.AsyncMqttClient;
import net.sf.xenqtt.client.MqttClient;
import net.sf.xenqtt.client.PublishMessage;
import net.sf.xenqtt.client.ReconnectionStrategy;
import net.sf.xenqtt.client.Subscription;
import net.sf.xenqtt.message.ConnectReturnCode;
import net.sf.xenqtt.message.QoS;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AsyncMqttClientIT {

	@Mock AsyncClientListener listener;
	@Mock ReconnectionStrategy reconnectionStrategy;
	@Captor ArgumentCaptor<Subscription[]> subscriptionCaptor;;
	@Captor ArgumentCaptor<PublishMessage> messageCaptor;;

	AsyncMqttClient client;

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		when(reconnectionStrategy.connectionLost(any(MqttClient.class), any(Throwable.class))).thenReturn(-1L);
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

		verifyNoMoreInteractions(listener);
		verifyZeroInteractions(reconnectionStrategy);
	}

	// This test can take over a minute to run so ignore it by default
	@Ignore
	@Test
	public void testInvalidPort() throws Exception {

		client = new AsyncMqttClient("tcp://test.mosquitto.org:1234", listener, reconnectionStrategy, 5, 5);

		verify(listener, timeout(100000)).disconnected(eq(client), any(ConnectException.class), eq(false));

		verifyNoMoreInteractions(listener);
		verifyZeroInteractions(reconnectionStrategy);
	}

	@Test
	public void testConnectDisconnect_NoCredentialsNoWill() throws Exception {

		client = new AsyncMqttClient("tcp://test.mosquitto.org:1883", listener, reconnectionStrategy, 5, 5);
		client.connect("testclient1", true, 90);
		verify(listener, timeout(1000)).connected(client, ConnectReturnCode.ACCEPTED);
		verify(reconnectionStrategy, timeout(1000)).connectionEstablished();

		client.disconnect();
		verify(listener, timeout(1000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		verifyNoMoreInteractions(listener, reconnectionStrategy);
	}

	@Test
	public void testConnect_BadCredentials() throws Exception {

		client = new AsyncMqttClient("tcp://q.m2m.io:1883", listener, reconnectionStrategy, 5, 5);
		client.connect("testclient1", true, 90, "not_a_user", "not_a_password");

		verify(listener, timeout(1000)).connected(client, ConnectReturnCode.BAD_CREDENTIALS);
		verify(listener, timeout(1000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		verifyNoMoreInteractions(listener);
		verifyZeroInteractions(reconnectionStrategy);
	}

	@Test
	public void testConnect_Credentials() throws Exception {

		fail("not implemented");
	}

	@Test
	public void testConnect_Will_NoRetain_Subscribed() throws Exception {

		// connect and subscribe a client to get the will message
		AsyncClientListener listener2 = mock(AsyncClientListener.class);
		AsyncMqttClient client2 = new AsyncMqttClient("tcp://test.mosquitto.org:1883", listener2, reconnectionStrategy, 5, 5);
		client2.connect("testclient2", true, 90);
		verify(listener2, timeout(1000)).connected(client2, ConnectReturnCode.ACCEPTED);
		client2.subscribe(new Subscription[] { new Subscription("my/will/topic", QoS.AT_LEAST_ONCE) });
		verify(listener2, timeout(1000)).subscribed(same(client2), any(Subscription[].class), any(Subscription[].class), eq(true));

		// connect and close a client to generate the will message
		client = new AsyncMqttClient("tcp://test.mosquitto.org:1883", listener, reconnectionStrategy, 5, 5);
		client.connect("testclient1", true, 90, "my/will/topic", "it died dude", QoS.AT_LEAST_ONCE, false);
		verify(listener, timeout(1000)).connected(client, ConnectReturnCode.ACCEPTED);
		client.close();
		verify(listener, timeout(1000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		// verify the will message
		verify(listener2, timeout(1000)).publishReceived(same(client2), messageCaptor.capture());
		PublishMessage message = messageCaptor.getValue();
		message.ack();
		assertEquals("my/will/topic", message.getTopic());
		assertEquals("it died dude", message.getPayloadString());
		assertEquals(QoS.AT_LEAST_ONCE, message.getQoS());
		assertFalse(message.isDuplicate());
		assertFalse(message.isRetain());

		client2.disconnect();
		verify(listener2, timeout(1000)).disconnected(same(client2), any(Throwable.class), eq(false));

		verifyNoMoreInteractions(listener, listener2);
	}

	@Test
	public void testConnect_Will_Retain_NotSubscribed() throws Exception {

		// connect and close a client to generate the will message
		client = new AsyncMqttClient("tcp://test.mosquitto.org:1883", listener, reconnectionStrategy, 5, 5);
		client.connect("testclient1", true, 90, "my/will/topic", "it died dude", QoS.AT_LEAST_ONCE, true);
		verify(listener, timeout(1000)).connected(client, ConnectReturnCode.ACCEPTED);
		client.close();
		verify(listener, timeout(1000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		// connect and subscribe a client to get the will message
		AsyncClientListener listener2 = mock(AsyncClientListener.class);
		AsyncMqttClient client2 = new AsyncMqttClient("tcp://test.mosquitto.org:1883", listener2, reconnectionStrategy, 5, 5);
		client2.connect("testclient2", true, 90);
		verify(listener2, timeout(1000)).connected(client2, ConnectReturnCode.ACCEPTED);
		client2.subscribe(new Subscription[] { new Subscription("my/will/topic", QoS.AT_LEAST_ONCE) });
		verify(listener2, timeout(1000)).subscribed(same(client2), any(Subscription[].class), any(Subscription[].class), eq(true));
		client2.disconnect();
		verify(listener2, timeout(1000)).disconnected(same(client2), any(Throwable.class), eq(false));

		// verify the will message
		verify(listener2, timeout(1000)).publishReceived(same(client2), messageCaptor.capture());
		PublishMessage message = messageCaptor.getValue();
		message.ack();
		assertEquals("my/will/topic", message.getTopic());
		assertEquals("it died dude", message.getPayloadString());
		assertEquals(QoS.AT_LEAST_ONCE, message.getQoS());
		assertFalse(message.isDuplicate());
		assertTrue(message.isRetain());

		verifyNoMoreInteractions(listener, listener2);
	}

	@Test
	public void testConnect_Will_NoRetain_NotSubscribed() throws Exception {

		// connect and close a client to generate the will message
		client = new AsyncMqttClient("tcp://test.mosquitto.org:1883", listener, reconnectionStrategy, 5, 5);
		client.connect("testclient1", true, 90, "my/will/topic", "it died dude", QoS.AT_LEAST_ONCE, false);
		verify(listener, timeout(1000)).connected(client, ConnectReturnCode.ACCEPTED);
		client.close();
		verify(listener, timeout(1000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		// connect and subscribe a client to get the will message
		AsyncClientListener listener2 = mock(AsyncClientListener.class);
		AsyncMqttClient client2 = new AsyncMqttClient("tcp://test.mosquitto.org:1883", listener2, reconnectionStrategy, 5, 5);
		client2.connect("testclient2", true, 90);
		verify(listener2, timeout(1000)).connected(client2, ConnectReturnCode.ACCEPTED);
		client2.subscribe(new Subscription[] { new Subscription("my/will/topic", QoS.AT_LEAST_ONCE) });
		verify(listener2, timeout(1000)).subscribed(same(client2), any(Subscription[].class), any(Subscription[].class), eq(true));
		client2.disconnect();
		verify(listener2, timeout(1000)).disconnected(same(client2), any(Throwable.class), eq(false));

		Thread.sleep(1000);

		// verify the will message
		verify(listener2, never()).publishReceived(same(client2), any(PublishMessage.class));

		verifyNoMoreInteractions(listener, listener2);
	}

	@Test
	public void testConnect_CredentialsAndWill() throws Exception {

		fail("not implemented");
	}

	@Test
	public void testSubscribeUnsubscribe_Array() throws Exception {

		// connect client
		client = new AsyncMqttClient("tcp://test.mosquitto.org:1883", listener, reconnectionStrategy, 5, 5);
		client.connect("testclient1", true, 90);
		verify(listener, timeout(1000)).connected(client, ConnectReturnCode.ACCEPTED);

		// test subscribing
		Subscription[] requestedSubscriptions = new Subscription[] { new Subscription("my/topic1", QoS.AT_LEAST_ONCE),
				new Subscription("my/topic2", QoS.AT_MOST_ONCE) };
		Subscription[] grantedSubscriptions = requestedSubscriptions;
		client.subscribe(requestedSubscriptions);
		verify(listener, timeout(1000)).subscribed(same(client), same(requestedSubscriptions), aryEq(grantedSubscriptions), eq(true));

		// test unsubscribing
		client.unsubscribe(new String[] { "my/topic1", "my/topic2" });
		verify(listener, timeout(1000)).unsubscribed(same(client), aryEq(new String[] { "my/topic1", "my/topic2" }));

		// disconnect
		client.disconnect();
		verify(listener, timeout(1000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		verifyNoMoreInteractions(listener);
	}

	@Test
	public void testSubscribeUnsubscribe_List() throws Exception {

		// connect client
		client = new AsyncMqttClient("tcp://test.mosquitto.org:1883", listener, reconnectionStrategy, 5, 5);
		client.connect("testclient1", true, 90);
		verify(listener, timeout(1000)).connected(client, ConnectReturnCode.ACCEPTED);

		// test subscribing
		Subscription[] requestedSubscriptions = new Subscription[] { new Subscription("my/topic1", QoS.AT_LEAST_ONCE),
				new Subscription("my/topic2", QoS.AT_MOST_ONCE) };
		List<Subscription> requestedSubscriptionsList = Arrays.asList(requestedSubscriptions);
		Subscription[] grantedSubscriptions = requestedSubscriptions;
		client.subscribe(requestedSubscriptionsList);
		verify(listener, timeout(1000)).subscribed(same(client), aryEq(requestedSubscriptions), aryEq(grantedSubscriptions), eq(true));

		// test unsubscribing
		client.unsubscribe(new String[] { "my/topic1", "my/topic2" });
		verify(listener, timeout(1000)).unsubscribed(same(client), aryEq(new String[] { "my/topic1", "my/topic2" }));

		// disconnect
		client.disconnect();
		verify(listener, timeout(1000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		verifyNoMoreInteractions(listener);
	}

	@Test
	public void testPublish() throws Exception {

		fail("not implemented");
	}

	@Test
	public void testClose() throws Exception {

		fail("not implemented");
	}
}
