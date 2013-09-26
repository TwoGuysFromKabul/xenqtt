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

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.net.ConnectException;
import java.nio.channels.UnresolvedAddressException;

import net.sf.xenqtt.MqttException;
import net.sf.xenqtt.MqttTimeoutException;
import net.sf.xenqtt.client.AsyncClientListener;
import net.sf.xenqtt.client.AsyncMqttClient;
import net.sf.xenqtt.client.MqttClient;
import net.sf.xenqtt.client.PublishMessage;
import net.sf.xenqtt.client.ReconnectionStrategy;
import net.sf.xenqtt.client.Subscription;
import net.sf.xenqtt.message.ConnectMessage;
import net.sf.xenqtt.message.ConnectReturnCode;
import net.sf.xenqtt.message.PubMessage;
import net.sf.xenqtt.message.QoS;
import net.sf.xenqtt.mockbroker.Client;
import net.sf.xenqtt.mockbroker.MockBroker;
import net.sf.xenqtt.mockbroker.MockBrokerHandler;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class AsyncMqttClientIT extends AbstractAsyncMqttClientIT {

	String badCredentialsUri = "tcp://q.m2m.io:1883";
	String validBrokerUri = "tcp://test.mosquitto.org:1883";

	@Mock MockBrokerHandler mockHandler;
	@Mock AsyncClientListener listener;
	@Mock ReconnectionStrategy reconnectionStrategy;
	@Captor ArgumentCaptor<Subscription[]> subscriptionCaptor;;
	@Captor ArgumentCaptor<PublishMessage> messageCaptor;;

	MockBroker mockBroker;
	AsyncMqttClient client;
	AsyncMqttClient client2;

	@Override
	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
	}

	@Override
	@After
	public void after() {

		if (client != null) {
			client.shutdown();
		}
		if (client2 != null) {
			client2.shutdown();
		}
		if (mockBroker != null) {
			mockBroker.shutdown(5000);
		}
	}

	@Test
	public void testConstructor_InvalidScheme() throws Exception {

		try {
			client = new AsyncMqttClient("ftp://foo:1883", listener, reconnectionStrategy, 5, 0, 5);
			fail("expected exception");
		} catch (MqttException e) {
			assertEquals("Invalid broker URI (scheme must be 'tcp'): ftp://foo:1883", e.getMessage());
		}

		verifyZeroInteractions(listener, reconnectionStrategy);
	}

	@Test
	public void testConstructor_InvalidHost() throws Exception {

		Throwable thrown = null;
		try {
			client = new AsyncMqttClient("tcp://foo:1883", listener, reconnectionStrategy, 5, 0, 5);
			fail("expected exception");
		} catch (MqttException e) {
			thrown = e.getCause();
			assertEquals(UnresolvedAddressException.class, thrown.getClass());
		}

		verify(listener, timeout(5000)).disconnected(any(AsyncMqttClient.class), same(thrown), eq(false));

		verifyNoMoreInteractions(listener);
		verifyZeroInteractions(reconnectionStrategy);
	}

	// This test can take over a minute to run so ignore it by default
	@Ignore
	@Test
	public void testConstructor_InvalidPort() throws Exception {

		client = new AsyncMqttClient("tcp://test.mosquitto.org:1234", listener, reconnectionStrategy, 5, 0, 5);

		verify(listener, timeout(100000)).disconnected(eq(client), any(ConnectException.class), eq(false));

		verifyNoMoreInteractions(listener);
		verifyZeroInteractions(reconnectionStrategy);
	}

	@Test
	public void testConnect_Credentials_Accepted() throws Exception {

		mockBroker = new MockBroker(null, 15, 0, true);
		mockBroker.init();
		mockBroker.addCredentials("user1", "password1");
		validBrokerUri = "tcp://localhost:" + mockBroker.getPort();

		client = new AsyncMqttClient(validBrokerUri, listener, reconnectionStrategy, 5, 0, 5);
		client.connect("testclient2", true, 90, "user1", "password1");

		verify(listener, timeout(5000)).connected(client, ConnectReturnCode.ACCEPTED);

		client.disconnect();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));
	}

	@Test
	public void testConnect_CredentialsAndWill_Accepted() throws Exception {

		mockBroker = new MockBroker(null, 15, 0, true);
		mockBroker.init();
		mockBroker.addCredentials("user1", "password1");
		validBrokerUri = "tcp://localhost:" + mockBroker.getPort();

		// connect and subscribe a client to get the will message
		AsyncClientListener listener2 = mock(AsyncClientListener.class);
		client2 = new AsyncMqttClient(validBrokerUri, listener2, reconnectionStrategy, 5, 0, 5);
		client2.connect("testclient3", true, 90);
		verify(listener2, timeout(5000)).connected(client2, ConnectReturnCode.ACCEPTED);
		client2.subscribe(new Subscription[] { new Subscription("my/will/topic1", QoS.AT_LEAST_ONCE) });
		verify(listener2, timeout(5000)).subscribed(same(client2), any(Subscription[].class), any(Subscription[].class), eq(true));

		// connect and close a client to generate the will message
		client = new AsyncMqttClient(validBrokerUri, listener, reconnectionStrategy, 5, 0, 5);
		client.connect("testclient4", true, 90, "user1", "password1", "my/will/topic1", "it died dude", QoS.AT_LEAST_ONCE, false);
		verify(listener, timeout(5000)).connected(client, ConnectReturnCode.ACCEPTED);
		client.close();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		// verify the will message
		verify(listener2, timeout(5000)).publishReceived(same(client2), messageCaptor.capture());
		PublishMessage message = messageCaptor.getValue();
		message.ack();
		assertEquals("my/will/topic1", message.getTopic());
		assertEquals("it died dude", message.getPayloadString());
		assertEquals(QoS.AT_LEAST_ONCE, message.getQoS());
		assertFalse(message.isDuplicate());
		assertFalse(message.isRetain());

		client2.disconnect();
		verify(listener2, timeout(5000)).disconnected(same(client2), any(Throwable.class), eq(false));

		verifyNoMoreInteractions(listener, listener2);
	}

	@Test
	public void testClose() throws Exception {

		client = new AsyncMqttClient(validBrokerUri, listener, reconnectionStrategy, 5, 0, 5);
		client.connect("testclient19", true, 90);
		verify(listener, timeout(5000)).connected(client, ConnectReturnCode.ACCEPTED);
		client.close();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));
	}

	@Test
	public void testDisconnect() throws Exception {

		client = new AsyncMqttClient(validBrokerUri, listener, reconnectionStrategy, 5, 0, 5);
		client.connect("testclient20", true, 90);
		verify(listener, timeout(5000)).connected(client, ConnectReturnCode.ACCEPTED);
		client.disconnect();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));
	}

	@Test
	public void testConnectMessageTimesOut() throws Exception {

		mockBroker = new MockBroker(mockHandler, 15, 0, true);
		mockBroker.init();
		mockBroker.addCredentials("user1", "password1");
		validBrokerUri = "tcp://localhost:" + mockBroker.getPort();

		when(reconnectionStrategy.connectionLost(isA(MqttClient.class), isA(MqttTimeoutException.class))).thenReturn(-1L);

		when(mockHandler.connect(isA(Client.class), isA(ConnectMessage.class))).thenReturn(true);

		client = new AsyncMqttClient(validBrokerUri, listener, reconnectionStrategy, 5, 1, 5);
		long start = System.currentTimeMillis();
		client.connect("testclient20", true, 90);
		verify(listener, timeout(1500)).disconnected(eq(client), isA(MqttTimeoutException.class), eq(false));
		assertTrue(System.currentTimeMillis() - start > 500);

		verifyNoMoreInteractions(listener);
	}

	@Test
	public void testConnectionLost_FirstReconnectSucceeds() throws Exception {

		// close the broker end of the channel when it receives a pub message
		doAnswer(new Answer<Boolean>() {

			@Override
			public Boolean answer(InvocationOnMock invocation) throws Throwable {

				Client client = (Client) invocation.getArguments()[0];
				client.close();
				return true;
			}
		}).doReturn(false).when(mockHandler).publish(isA(Client.class), isA(PubMessage.class));

		// configure reconnection strategy to try to reconnect
		when(reconnectionStrategy.connectionLost(isA(MqttClient.class), isNull(Throwable.class))).thenReturn(1000L);

		// create the broker
		mockBroker = new MockBroker(mockHandler, 15, 0, true);
		mockBroker.init();
		validBrokerUri = "tcp://localhost:" + mockBroker.getPort();

		// connect to the broker and send the pub message which will cause the channel to close
		client = new AsyncMqttClient(validBrokerUri, listener, reconnectionStrategy, 5, 5, 5);
		client.connect("testclient20", true, 90);
		verify(reconnectionStrategy, timeout(5000)).connectionEstablished();
		PublishMessage pubMessage = new PublishMessage("foo", QoS.AT_LEAST_ONCE, "abc");
		client.publish(pubMessage);

		// verify connection is lost
		verify(reconnectionStrategy, timeout(5000)).connectionLost(isA(MqttClient.class), isNull(Throwable.class));
		verify(listener, timeout(5000)).disconnected(client, null, true);

		// verify reconnect in about 1 second
		long start = System.currentTimeMillis();
		verify(reconnectionStrategy, timeout(1500).times(2)).connectionEstablished();
		assertTrue(System.currentTimeMillis() - start > 500);

		// verify the message we sent before closing the channel got published
		verify(listener, timeout(5000)).published(client, pubMessage);

		// disconnect the client
		client.disconnect();
		verify(listener, timeout(5000)).disconnected(client, null, false);
	}

	@Test
	public void testConnectionLost_NotFirstReconnectSucceeds() throws Exception {

		fail("not implemented");
	}

	@Test
	public void testConnectionLost_AllReconnectsFail() throws Exception {

		fail("not implemented");
	}

	@Test
	public void testSendWhileReconnectionInProgress() throws Exception {

		fail("not implemented");
	}
}
