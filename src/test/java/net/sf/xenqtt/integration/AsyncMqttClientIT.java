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

import java.lang.reflect.Field;
import java.net.ConnectException;
import java.nio.channels.UnresolvedAddressException;
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.xenqtt.MqttException;
import net.sf.xenqtt.MqttInvocationException;
import net.sf.xenqtt.MqttTimeoutException;
import net.sf.xenqtt.MqttTooManyMessagesInFlightException;
import net.sf.xenqtt.client.AsyncClientListener;
import net.sf.xenqtt.client.AsyncMqttClient;
import net.sf.xenqtt.client.MqttClient;
import net.sf.xenqtt.client.PublishMessage;
import net.sf.xenqtt.client.Subscription;
import net.sf.xenqtt.message.ConnectMessage;
import net.sf.xenqtt.message.ConnectReturnCode;
import net.sf.xenqtt.message.PubMessage;
import net.sf.xenqtt.message.QoS;
import net.sf.xenqtt.mockbroker.Client;
import net.sf.xenqtt.mockbroker.MockBroker;
import net.sf.xenqtt.mockbroker.MockBrokerHandler;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class AsyncMqttClientIT extends AbstractAsyncMqttClientIT {

	@Mock MockBrokerHandler mockHandler;

	MockBroker mockBroker;

	@Override
	@After
	public void after() {

		super.after();
		if (mockBroker != null) {
			mockBroker.shutdown(5000);
		}
	}

	@Test
	public void testConstructor_InvalidScheme() throws Exception {

		try {
			client = new AsyncMqttClient("ftp://foo:1883", listener, 5, config);
			fail("expected exception");
		} catch (MqttException e) {
			assertEquals("Invalid broker URI (scheme must be 'tcp'): ftp://foo:1883", e.getMessage());
		}

		verify(reconnectionStrategy).clone();
		verifyNoMoreInteractions(reconnectionStrategy);
		verifyZeroInteractions(listener);
	}

	@Test
	public void testConstructor_InvalidHost() throws Exception {

		Throwable thrown = null;
		try {
			client = new AsyncMqttClient("tcp://foo:1883", listener, 5, config);
			fail("expected exception");
		} catch (MqttInvocationException e) {
			thrown = e.getRootCause();
			assertEquals(UnresolvedAddressException.class, thrown.getClass());
		}

		verify(listener, timeout(5000)).disconnected(any(AsyncMqttClient.class), same(thrown), eq(false));
		verify(reconnectionStrategy).clone();

		verifyNoMoreInteractions(listener, reconnectionStrategy);
	}

	// This test can take over a minute to run so ignore it by default
	@Ignore
	@Test
	public void testConstructor_InvalidPort() throws Exception {

		client = new AsyncMqttClient("tcp://test.mosquitto.org:1234", listener, 5, config);

		verify(listener, timeout(100000)).disconnected(eq(client), any(ConnectException.class), eq(false));
		verify(reconnectionStrategy).clone();

		verifyNoMoreInteractions(listener, reconnectionStrategy);
	}

	@Test
	public void testConnect_Credentials_Accepted() throws Exception {

		mockBroker = new MockBroker(null, 15, 0, true, true, 50);
		mockBroker.init();
		mockBroker.addCredentials("user1", "password1");
		validBrokerUri = "tcp://localhost:" + mockBroker.getPort();

		client = new AsyncMqttClient(validBrokerUri, listener, 5, config);
		client.connect("testclient2", true, "user1", "password1");

		verify(listener, timeout(5000)).connected(client, ConnectReturnCode.ACCEPTED);

		client.disconnect();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));
	}

	@Test
	public void testConnect_CredentialsAndWill_Accepted() throws Exception {

		mockBroker = new MockBroker(null, 15, 0, true, true, 50);
		mockBroker.init();
		mockBroker.addCredentials("user1", "password1");
		validBrokerUri = "tcp://localhost:" + mockBroker.getPort();

		// connect and subscribe a client to get the will message
		AsyncClientListener listener2 = mock(AsyncClientListener.class);
		client2 = new AsyncMqttClient(validBrokerUri, listener2, 5, config);
		client2.connect("testclient3", true);
		verify(listener2, timeout(5000)).connected(client2, ConnectReturnCode.ACCEPTED);
		client2.subscribe(new Subscription[] { new Subscription("my/will/topic1", QoS.AT_LEAST_ONCE) });
		verify(listener2, timeout(5000)).subscribed(same(client2), any(Subscription[].class), any(Subscription[].class), eq(true));

		// connect and close a client to generate the will message
		client = new AsyncMqttClient(validBrokerUri, listener, 5, config);
		client.connect("testclient4", true, "user1", "password1", "my/will/topic1", "it died dude", QoS.AT_LEAST_ONCE, false);
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

		client = new AsyncMqttClient(validBrokerUri, listener, 5, config);
		client.connect("testclient19", true);
		verify(listener, timeout(5000)).connected(client, ConnectReturnCode.ACCEPTED);
		assertFalse(client.isClosed());

		client.close();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));
		assertTrue(client.isClosed());
	}

	@Test
	public void testDisconnect() throws Exception {

		client = new AsyncMqttClient(validBrokerUri, listener, 5, config);
		client.connect("testclient20", true);
		verify(listener, timeout(5000)).connected(client, ConnectReturnCode.ACCEPTED);
		assertFalse(client.isClosed());

		client.disconnect();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		assertTrue(client.isClosed());
	}

	@Test
	public void testConnectMessageTimesOut() throws Exception {

		mockBroker = new MockBroker(mockHandler, 15, 0, true, true, 50);
		mockBroker.init();
		mockBroker.addCredentials("user1", "password1");
		validBrokerUri = "tcp://localhost:" + mockBroker.getPort();

		when(reconnectionStrategy.connectionLost(isA(MqttClient.class), isA(MqttTimeoutException.class))).thenReturn(-1L);

		when(mockHandler.connect(isA(Client.class), isA(ConnectMessage.class))).thenReturn(true);

		config.setConnectTimeoutSeconds(1);
		client = new AsyncMqttClient(validBrokerUri, listener, 5, config);
		long start = System.currentTimeMillis();
		client.connect("testclient20", true);
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
		mockBroker = new MockBroker(mockHandler, 15, 0, true, true, 50);
		mockBroker.init();
		validBrokerUri = "tcp://localhost:" + mockBroker.getPort();

		// connect to the broker and send the pub message which will cause the channel to close
		client = new AsyncMqttClient(validBrokerUri, listener, 5, config);
		client.connect("testclient20", true);
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

		Answer<Boolean> answer = new Answer<Boolean>() {

			@Override
			public Boolean answer(InvocationOnMock invocation) throws Throwable {

				Client client = (Client) invocation.getArguments()[0];
				client.close();
				return true;
			}
		};
		// close the broker end of the channel when it receives a pub message
		doAnswer(answer).doAnswer(answer).doReturn(false).when(mockHandler).publish(isA(Client.class), isA(PubMessage.class));

		// configure reconnection strategy to try to reconnect
		when(reconnectionStrategy.connectionLost(isA(MqttClient.class), isNull(Throwable.class))).thenReturn(1000L);

		// create the broker
		mockBroker = new MockBroker(mockHandler, 15, 0, true, true, 50);
		mockBroker.init();
		validBrokerUri = "tcp://localhost:" + mockBroker.getPort();

		// connect to the broker and send the pub message which will cause the channel to close
		client = new AsyncMqttClient(validBrokerUri, listener, 5, config);
		client.connect("testclient20", true);
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

		// verify connection is lost again
		verify(reconnectionStrategy, timeout(5000).times(2)).connectionLost(isA(MqttClient.class), isNull(Throwable.class));
		verify(listener, timeout(5000).times(2)).disconnected(client, null, true);

		// verify reconnect in about 1 second
		start = System.currentTimeMillis();
		verify(reconnectionStrategy, timeout(1500).times(3)).connectionEstablished();
		assertTrue(System.currentTimeMillis() - start > 500);

		// verify the message we sent before closing the channel got published
		verify(listener, timeout(5000)).published(client, pubMessage);

		// disconnect the client
		client.disconnect();
		verify(listener, timeout(5000)).disconnected(client, null, false);
	}

	@Test
	public void testConnectionLost_AllReconnectsFail() throws Exception {

		// close the broker end of the channel when it receives a pub message
		doAnswer(new Answer<Boolean>() {

			@Override
			public Boolean answer(InvocationOnMock invocation) throws Throwable {

				Client client = (Client) invocation.getArguments()[0];
				client.close();
				return true;
			}
		}).when(mockHandler).publish(isA(Client.class), isA(PubMessage.class));

		// configure reconnection strategy to try to reconnect
		when(reconnectionStrategy.connectionLost(isA(MqttClient.class), isNull(Throwable.class))).thenReturn(1000L, 1000L, 1000L, 0L);

		// create the broker
		mockBroker = new MockBroker(mockHandler, 15, 0, true, true, 50);
		mockBroker.init();
		validBrokerUri = "tcp://localhost:" + mockBroker.getPort();

		// connect to the broker and send the pub message which will cause the channel to close
		client = new AsyncMqttClient(validBrokerUri, listener, 5, config);
		client.connect("testclient20", true);
		verify(reconnectionStrategy, timeout(5000)).connectionEstablished();
		PublishMessage pubMessage = new PublishMessage("foo", QoS.AT_LEAST_ONCE, "abc");
		client.publish(pubMessage);

		// verify connection is lost
		verify(reconnectionStrategy, timeout(5000)).connectionLost(isA(MqttClient.class), isNull(Throwable.class));
		verify(listener, timeout(5000)).disconnected(client, null, true);
		assertFalse(client.isClosed());

		// verify reconnect in about 1 second
		long start = System.currentTimeMillis();
		verify(reconnectionStrategy, timeout(1500).times(2)).connectionEstablished();
		assertTrue(System.currentTimeMillis() - start > 500);

		// verify connection is lost again
		verify(reconnectionStrategy, timeout(5000).times(2)).connectionLost(isA(MqttClient.class), isNull(Throwable.class));
		verify(listener, timeout(5000).times(2)).disconnected(client, null, true);
		assertFalse(client.isClosed());

		// verify reconnect in about 1 second
		start = System.currentTimeMillis();
		verify(reconnectionStrategy, timeout(1500).times(3)).connectionEstablished();
		assertTrue(System.currentTimeMillis() - start > 500);

		// verify connection is lost again
		verify(reconnectionStrategy, timeout(5000).times(3)).connectionLost(isA(MqttClient.class), isNull(Throwable.class));
		verify(listener, timeout(5000).times(3)).disconnected(client, null, true);
		assertFalse(client.isClosed());

		// verify reconnect in about 1 second
		start = System.currentTimeMillis();
		verify(reconnectionStrategy, timeout(1500).times(4)).connectionEstablished();
		assertTrue(System.currentTimeMillis() - start > 500);

		// verify connection is lost again
		verify(reconnectionStrategy, timeout(5000).times(4)).connectionLost(isA(MqttClient.class), isNull(Throwable.class));
		verify(listener, timeout(5000)).disconnected(client, null, false);
		assertTrue(client.isClosed());

		// verify the message we sent before closing the channel never got published
		verify(listener, timeout(1000).never()).published(client, pubMessage);

		// verify the client is disconnected
		verify(listener, timeout(5000)).disconnected(client, null, false);
	}

	@Test
	public final void testPublish_Qos0_MaxInFlightMessagesReached() throws Exception {

		config.setMaxInFlightMessages(0);
		testPublish_Qos0_NoRetain();
	}

	@Test
	public final void testPublish_Qos1_DuplicateMessageIdWouldBeUsed() throws Exception {

		when(mockHandler.publish(any(Client.class), any(PubMessage.class))).thenReturn(true);

		mockBroker = new MockBroker(mockHandler, 15, 0, true, true, 50);
		mockBroker.init();
		validBrokerUri = mockBroker.getURI();

		// connect a client and generate the messages
		client = new AsyncMqttClient(validBrokerUri, listener, 5, config);
		client.connect("testclient14", true);
		verify(listener, timeout(5000)).connected(client, ConnectReturnCode.ACCEPTED);

		Field field = Class.forName("net.sf.xenqtt.client.AbstractMqttClient").getDeclaredField("messageIdGenerator");
		field.setAccessible(true);
		AtomicInteger idGen = (AtomicInteger) field.get(client);

		client.publish(new PublishMessage("my/topic5", QoS.AT_LEAST_ONCE, "my message " + 1));
		ArgumentCaptor<PubMessage> captor = ArgumentCaptor.forClass(PubMessage.class);
		verify(mockHandler, timeout(5000)).publish(any(Client.class), captor.capture());
		assertEquals(1, captor.getValue().getMessageId());

		// decrement the message id generator so it will try to reuse the ID we just used then make sure it skips that one.
		idGen.decrementAndGet();

		reset(mockHandler);
		client.publish(new PublishMessage("my/topic5", QoS.AT_LEAST_ONCE, "my message " + 1));
		captor = ArgumentCaptor.forClass(PubMessage.class);
		verify(mockHandler, timeout(5000)).publish(any(Client.class), captor.capture());
		assertEquals(2, captor.getValue().getMessageId());

		client.disconnect();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));
	}

	@Test(expected = MqttTooManyMessagesInFlightException.class)
	public final void testPublish_Qos1_MaxInFLightMessagesReached() throws Exception {

		config.setMaxInFlightMessages(0);
		testPublish_Qos1_NoRetain();
	}
}
