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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.sf.xenqtt.MqttCommandCancelledException;
import net.sf.xenqtt.MqttException;
import net.sf.xenqtt.MqttInvocationException;
import net.sf.xenqtt.MqttTimeoutException;
import net.sf.xenqtt.client.MqttClient;
import net.sf.xenqtt.client.MqttClientConfig;
import net.sf.xenqtt.client.MqttClientDebugListener;
import net.sf.xenqtt.client.MqttClientListener;
import net.sf.xenqtt.client.PublishMessage;
import net.sf.xenqtt.client.ReconnectionStrategy;
import net.sf.xenqtt.client.Subscription;
import net.sf.xenqtt.client.SyncMqttClient;
import net.sf.xenqtt.message.ConnAckMessage;
import net.sf.xenqtt.message.ConnectMessage;
import net.sf.xenqtt.message.ConnectReturnCode;
import net.sf.xenqtt.message.DisconnectMessage;
import net.sf.xenqtt.message.MqttMessage;
import net.sf.xenqtt.message.PubAckMessage;
import net.sf.xenqtt.message.PubMessage;
import net.sf.xenqtt.message.QoS;
import net.sf.xenqtt.message.SubAckMessage;
import net.sf.xenqtt.message.SubscribeMessage;
import net.sf.xenqtt.message.UnsubAckMessage;
import net.sf.xenqtt.message.UnsubscribeMessage;
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

public class SyncMqttClientIT {

	MqttClientDebugListener clientDebugListener = mock(MqttClientDebugListener.class);
	List<Class<? extends MqttMessage>> sentMessages = Collections.synchronizedList(new ArrayList<Class<? extends MqttMessage>>());
	List<Class<? extends MqttMessage>> receivedMessages = Collections.synchronizedList(new ArrayList<Class<? extends MqttMessage>>());
	MqttClientConfig config = new MqttClientConfig().setConnectTimeoutSeconds(0).setMessageResendIntervalSeconds(5).setBlockingTimeoutSeconds(10)
			.setClientDebugListener(clientDebugListener);

	String brokerUri = "tcp://test.mosquitto.org:1883";

	@Mock MockBrokerHandler mockHandler;
	@Mock MqttClientListener listener;
	@Mock ReconnectionStrategy reconnectionStrategy;
	@Captor ArgumentCaptor<PublishMessage> messageCaptor;

	MockBroker mockBroker;
	SyncMqttClient client;
	SyncMqttClient client2;

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		config.setReconnectionStrategy(reconnectionStrategy);
		when(reconnectionStrategy.clone()).thenReturn(reconnectionStrategy);

		doAnswer(new Answer<Void>() {

			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				sentMessages.add(((MqttMessage) invocation.getArguments()[3]).getClass());

				return null;
			}

		}).when(clientDebugListener).messageSent(any(MqttClient.class), anyString(), anyString(), any(MqttMessage.class));
		doAnswer(new Answer<Void>() {

			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				receivedMessages.add(((MqttMessage) invocation.getArguments()[3]).getClass());

				return null;
			}

		}).when(clientDebugListener).messageReceived(any(MqttClient.class), anyString(), anyString(), any(MqttMessage.class));
	}

	@After
	public void after() {

		if (client != null) {
			try {
				client.close();
			} catch (MqttCommandCancelledException ignore) {
			}
		}
		if (client2 != null) {
			try {
				client2.close();
			} catch (MqttCommandCancelledException ignore) {
			}
		}
		if (mockBroker != null) {
			mockBroker.shutdown(5000);
		}
	}

	@Test
	public void testConstructor_InvalidScheme() throws Exception {

		try {
			client = new SyncMqttClient("ftp://foo:1883", listener, 5, config);
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
			client = new SyncMqttClient("tcp://foo:1883", listener, 5, config);
			fail("expected exception");
		} catch (MqttInvocationException e) {
			thrown = e.getRootCause();
			assertEquals(UnresolvedAddressException.class, thrown.getClass());
		}

		verify(listener, timeout(5000)).disconnected(any(SyncMqttClient.class), same(thrown), eq(false));
		verify(reconnectionStrategy).clone();

		verifyNoMoreInteractions(listener, reconnectionStrategy);
	}

	// This test can take over a minute to run so ignore it by default
	@Ignore
	@Test
	public void testConstructor_InvalidPort() throws Exception {

		try {
			config.setBlockingTimeoutSeconds(0);
			client = new SyncMqttClient("tcp://test.mosquitto.org:1234", listener, 5, config);
			fail("expected exception");
		} catch (MqttCommandCancelledException e) {

			assertTrue(e.getCause().getCause() instanceof ConnectException);
			verify(listener, timeout(1000)).disconnected(any(MqttClient.class), same(e.getCause().getCause()), eq(false));
			verify(reconnectionStrategy).clone();
			verifyNoMoreInteractions(listener, reconnectionStrategy);
		}
	}

	@Test
	public void testConstructorTimesOut() throws Exception {

		config.setBlockingTimeoutSeconds(1);
		try {
			client = new SyncMqttClient("tcp://test.mosquitto.org:1234", listener, 5, config);
			fail("expected exception");
		} catch (MqttTimeoutException e) {
			verify(reconnectionStrategy).clone();
			verifyNoMoreInteractions(reconnectionStrategy);
			verifyZeroInteractions(listener);
		}
	}

	@Test
	public void testConnectDisconnect_NoCredentialsNoWill() throws Exception {

		client = new SyncMqttClient(brokerUri, listener, 5, config);
		assertEquals(ConnectReturnCode.ACCEPTED, client.connect("testclient1", true));
		verify(reconnectionStrategy, timeout(5000)).connectionEstablished();

		client.disconnect();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));
		verify(reconnectionStrategy).clone();

		verifyOpenExchangeCloseCadence(new Class<?>[] { ConnectMessage.class, DisconnectMessage.class }, new Class<?>[] { ConnAckMessage.class });

		verifyNoMoreInteractions(listener, reconnectionStrategy);
	}

	@Test
	public void testConnect_Credentials_BadCredentials() throws Exception {

		mockBroker = new MockBroker(null, 15, 0, true, true, 50);
		mockBroker.init();
		brokerUri = "tcp://localhost:" + mockBroker.getPort();

		client = new SyncMqttClient(brokerUri, listener, 5, config);
		assertEquals(ConnectReturnCode.BAD_CREDENTIALS, client.connect("testclient2", true, "not_a_user", "not_a_password"));

		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));
		verify(reconnectionStrategy).clone();
		verifyOpenExchangeCloseCadence(new Class<?>[] { ConnectMessage.class }, new Class<?>[] { ConnAckMessage.class });

		verifyNoMoreInteractions(listener, reconnectionStrategy);
	}

	@Test
	public void testConnect_Credentials_Accepted() throws Exception {

		mockBroker = new MockBroker(null, 15, 0, true, true, 50);
		mockBroker.addCredentials("user1", "password1");
		mockBroker.init();
		brokerUri = "tcp://localhost:" + mockBroker.getPort();

		client = new SyncMqttClient(brokerUri, listener, 5, config);
		assertEquals(ConnectReturnCode.ACCEPTED, client.connect("testclient2", true, "user1", "password1"));

		client.disconnect();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));
		verifyOpenExchangeCloseCadence(new Class<?>[] { ConnectMessage.class, DisconnectMessage.class }, new Class<?>[] { ConnAckMessage.class });
	}

	@Test
	public void testConnect_Will_NoRetain_Subscribed() throws Exception {

		// connect and subscribe a client to get the will message
		MqttClientListener listener2 = mock(MqttClientListener.class);
		client2 = new SyncMqttClient(brokerUri, listener2, 5, config);
		assertEquals(ConnectReturnCode.ACCEPTED, client2.connect("testclient3", true));
		assertNotNull(client2.subscribe(new Subscription[] { new Subscription("my/will/topic1", QoS.AT_LEAST_ONCE) }));

		// connect and close a client to generate the will message
		client = new SyncMqttClient(brokerUri, listener, 5, config);
		assertEquals(ConnectReturnCode.ACCEPTED, client.connect("testclient4", true, "my/will/topic1", "it died dude", QoS.AT_LEAST_ONCE, false));
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

		verifyOpenExchangeCloseCadence(new Class<?>[] { ConnectMessage.class, SubscribeMessage.class, ConnectMessage.class, PubAckMessage.class,
				DisconnectMessage.class }, new Class<?>[] { ConnAckMessage.class, SubAckMessage.class, ConnAckMessage.class, PubMessage.class });

		verifyNoMoreInteractions(listener, listener2);
	}

	@Test
	public void testConnect_Will_NoRetain_NotSubscribed() throws Exception {

		// connect and close a client to generate the will message
		client = new SyncMqttClient(brokerUri, listener, 5, config);
		assertEquals(ConnectReturnCode.ACCEPTED, client.connect("testclient5", true, "my/will/topic2", "it died dude", QoS.AT_LEAST_ONCE, false));
		client.close();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		// connect and subscribe a client to get the will message
		MqttClientListener listener2 = mock(MqttClientListener.class);
		client2 = new SyncMqttClient(brokerUri, listener2, 5, config);
		assertEquals(ConnectReturnCode.ACCEPTED, client2.connect("testclient6", true));
		assertNotNull(client2.subscribe(new Subscription[] { new Subscription("my/will/topic2", QoS.AT_LEAST_ONCE) }));
		// verify no will message
		Thread.sleep(1000);
		verify(listener2, never()).publishReceived(same(client2), any(PublishMessage.class));
		client2.disconnect();
		verify(listener2, timeout(5000)).disconnected(same(client2), any(Throwable.class), eq(false));

		verifyOpenExchangeCloseCadence(new Class<?>[] { ConnectMessage.class, ConnectMessage.class, SubscribeMessage.class, DisconnectMessage.class },
				new Class<?>[] { ConnAckMessage.class, ConnAckMessage.class, SubAckMessage.class });

		verifyNoMoreInteractions(listener, listener2);
	}

	@Test
	public void testConnect_Will_Retain_NotSubscribed() throws Exception {

		// connect and close a client to generate the will message
		client = new SyncMqttClient(brokerUri, listener, 5, config);
		assertEquals(ConnectReturnCode.ACCEPTED, client.connect("testclient7", true, "my/will/topic3", "it died dude", QoS.AT_LEAST_ONCE, true));

		client.close();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		// connect and subscribe a client to get the will message
		MqttClientListener listener2 = mock(MqttClientListener.class);
		client2 = new SyncMqttClient(brokerUri, listener2, 5, config);
		assertEquals(ConnectReturnCode.ACCEPTED, client2.connect("testclient8", true));
		assertNotNull(client2.subscribe(new Subscription[] { new Subscription("my/will/topic3", QoS.AT_LEAST_ONCE) }));

		// verify the will message
		verify(listener2, timeout(5000)).publishReceived(same(client2), messageCaptor.capture());
		PublishMessage message = messageCaptor.getValue();
		message.ack();
		assertEquals("my/will/topic3", message.getTopic());
		assertEquals("it died dude", message.getPayloadString());
		assertEquals(QoS.AT_LEAST_ONCE, message.getQoS());
		assertFalse(message.isDuplicate());
		assertTrue(message.isRetain());

		// remove the message
		client2.publish(new PublishMessage("my/will/topic3", QoS.AT_LEAST_ONCE, new byte[0], true));
		verify(listener2, timeout(5000)).publishReceived(same(client2), isA(PublishMessage.class));

		client2.disconnect();
		verify(listener2, timeout(5000)).disconnected(same(client2), any(Throwable.class), eq(false));

		verifyOpenExchangeCloseCadence(new Class<?>[] { ConnectMessage.class, ConnectMessage.class, SubscribeMessage.class, PubAckMessage.class,
				PubMessage.class, DisconnectMessage.class }, new Class<?>[] { ConnAckMessage.class, ConnAckMessage.class, SubAckMessage.class,
				PubMessage.class, PubAckMessage.class });

		verifyNoMoreInteractions(listener, listener2);
	}

	@Test
	public void testConnect_Will_Retain_Subscribed() throws Exception {

		// connect and close a client to generate the will message
		client = new SyncMqttClient(brokerUri, listener, 5, config);
		assertEquals(ConnectReturnCode.ACCEPTED, client.connect("testclient10", true, "my/will/topic4", "it died dude", QoS.AT_LEAST_ONCE, true));
		client.close();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		// connect and subscribe a client to get the will message
		MqttClientListener listener2 = mock(MqttClientListener.class);
		client2 = new SyncMqttClient(brokerUri, listener2, 5, config);
		assertEquals(ConnectReturnCode.ACCEPTED, client2.connect("testclient9", true));
		assertNotNull(client2.subscribe(new Subscription[] { new Subscription("my/will/topic4", QoS.AT_LEAST_ONCE) }));

		// verify the will message
		verify(listener2, timeout(5000)).publishReceived(same(client2), messageCaptor.capture());
		PublishMessage message = messageCaptor.getValue();
		message.ack();
		assertEquals("my/will/topic4", message.getTopic());
		assertEquals("it died dude", message.getPayloadString());
		assertEquals(QoS.AT_LEAST_ONCE, message.getQoS());
		assertFalse(message.isDuplicate());
		assertTrue(message.isRetain());

		// remove the message
		client2.publish(new PublishMessage("my/will/topic4", QoS.AT_LEAST_ONCE, new byte[0], true));
		verify(listener2, timeout(5000)).publishReceived(same(client2), isA(PublishMessage.class));

		client2.disconnect();
		verify(listener2, timeout(5000)).disconnected(same(client2), any(Throwable.class), eq(false));

		verifyOpenExchangeCloseCadence(new Class<?>[] { ConnectMessage.class, ConnectMessage.class, SubscribeMessage.class, PubAckMessage.class,
				PubMessage.class, DisconnectMessage.class }, new Class<?>[] { ConnAckMessage.class, ConnAckMessage.class, SubAckMessage.class,
				PubMessage.class, PubAckMessage.class });

		verifyNoMoreInteractions(listener, listener2);
	}

	@Test
	public void testConnect_CredentialsAndWill_Accepted() throws Exception {

		mockBroker = new MockBroker(null, 15, 0, true, true, 50);
		mockBroker.addCredentials("user1", "password1");
		mockBroker.init();
		brokerUri = "tcp://localhost:" + mockBroker.getPort();

		// connect and subscribe a client to get the will message
		MqttClientListener listener2 = mock(MqttClientListener.class);
		client2 = new SyncMqttClient(brokerUri, listener2, 5, config);
		assertEquals(ConnectReturnCode.ACCEPTED, client2.connect("testclient3", true));
		client2.subscribe(new Subscription[] { new Subscription("my/will/topic1", QoS.AT_LEAST_ONCE) });

		// connect and close a client to generate the will message
		client = new SyncMqttClient(brokerUri, listener, 5, config);
		assertEquals(ConnectReturnCode.ACCEPTED,
				client.connect("testclient4", true, "user1", "password1", "my/will/topic1", "it died dude", QoS.AT_LEAST_ONCE, false));
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

		verifyOpenExchangeCloseCadence(new Class<?>[] { ConnectMessage.class, SubscribeMessage.class, ConnectMessage.class, PubAckMessage.class,
				DisconnectMessage.class }, new Class<?>[] { ConnAckMessage.class, SubAckMessage.class, ConnAckMessage.class, PubMessage.class });

		verifyNoMoreInteractions(listener, listener2);
	}

	@Test
	public void testSubscribeUnsubscribe_Array() throws Exception {

		// connect client
		client = new SyncMqttClient(brokerUri, listener, 5, config);
		assertEquals(ConnectReturnCode.ACCEPTED, client.connect("testclient11", true));

		// test subscribing
		Subscription[] requestedSubscriptions = new Subscription[] { new Subscription("my/topic1", QoS.AT_LEAST_ONCE),
				new Subscription("my/topic2", QoS.AT_MOST_ONCE) };
		Subscription[] grantedSubscriptions = requestedSubscriptions;
		assertArrayEquals(grantedSubscriptions, client.subscribe(requestedSubscriptions));

		// test unsubscribing
		client.unsubscribe(new String[] { "my/topic1", "my/topic2" });

		// disconnect
		client.disconnect();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		verifyOpenExchangeCloseCadence(new Class<?>[] { ConnectMessage.class, SubscribeMessage.class, UnsubscribeMessage.class, DisconnectMessage.class },
				new Class<?>[] { ConnAckMessage.class, SubAckMessage.class, UnsubAckMessage.class });

		verifyNoMoreInteractions(listener);
	}

	@Test
	public void testSubscribeUnsubscribe_List() throws Exception {

		// connect client
		client = new SyncMqttClient(brokerUri, listener, 5, config);
		assertEquals(ConnectReturnCode.ACCEPTED, client.connect("testclient12", true));

		// test subscribing
		List<Subscription> requestedSubscriptions = Arrays.asList(new Subscription[] { new Subscription("my/topic3", QoS.AT_LEAST_ONCE),
				new Subscription("my/topic4", QoS.AT_MOST_ONCE) });
		List<Subscription> grantedSubscriptions = requestedSubscriptions;
		assertEquals(grantedSubscriptions, client.subscribe(requestedSubscriptions));

		// test unsubscribing
		client.unsubscribe(Arrays.asList(new String[] { "my/topic3", "my/topic4" }));

		// disconnect
		client.disconnect();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		verifyOpenExchangeCloseCadence(new Class<?>[] { ConnectMessage.class, SubscribeMessage.class, UnsubscribeMessage.class, DisconnectMessage.class },
				new Class<?>[] { ConnAckMessage.class, SubAckMessage.class, UnsubAckMessage.class });

		verifyNoMoreInteractions(listener);
	}

	@Test
	public void testPublish_Qos1_NoRetain() throws Exception {

		// connect and subscribe a client to get the messages
		MqttClientListener listener2 = mock(MqttClientListener.class);
		client2 = new SyncMqttClient(brokerUri, listener2, 5, config);
		assertEquals(ConnectReturnCode.ACCEPTED, client2.connect("testclient13", true));
		assertNotNull(client2.subscribe(new Subscription[] { new Subscription("my/topic5", QoS.AT_LEAST_ONCE) }));

		// connect a client and generate the messages
		client = new SyncMqttClient(brokerUri, listener, 5, config);
		assertEquals(ConnectReturnCode.ACCEPTED, client.connect("testclient14", true));
		for (int i = 0; i < 10; i++) {
			client.publish(new PublishMessage("my/topic5", QoS.AT_LEAST_ONCE, "my message " + i));
		}
		client.disconnect();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		// verify the messages
		verify(listener2, timeout(5000).times(10)).publishReceived(same(client2), messageCaptor.capture());

		for (PublishMessage message : messageCaptor.getAllValues()) {
			message.ack();
			assertEquals("my/topic5", message.getTopic());
			assertTrue(message.getPayloadString().startsWith("my message "));
			assertEquals(QoS.AT_LEAST_ONCE, message.getQoS());
			assertFalse(message.isDuplicate());
			assertFalse(message.isRetain());
		}

		client2.disconnect();
		verify(listener2, timeout(5000)).disconnected(same(client2), any(Throwable.class), eq(false));

		Class<?>[] expectedSentMessages = new Class<?>[25];
		expectedSentMessages[0] = ConnectMessage.class;
		expectedSentMessages[1] = SubscribeMessage.class;
		expectedSentMessages[2] = ConnectMessage.class;
		for (int i = 3; i < 13; i++) {
			expectedSentMessages[i] = PubMessage.class;
		}
		for (int i = 13; i < 23; i++) {
			expectedSentMessages[i] = PubAckMessage.class;
		}
		expectedSentMessages[23] = DisconnectMessage.class;
		expectedSentMessages[24] = DisconnectMessage.class;

		Class<?>[] expectedReceivedMessages = new Class<?>[23];
		expectedReceivedMessages[0] = ConnAckMessage.class;
		expectedReceivedMessages[1] = SubAckMessage.class;
		expectedReceivedMessages[2] = ConnAckMessage.class;
		for (int i = 3; i < 13; i++) {
			expectedReceivedMessages[i] = PubMessage.class;
		}
		for (int i = 13; i < 23; i++) {
			expectedReceivedMessages[i] = PubAckMessage.class;
		}

		verifyOpenExchangeCloseCadence(expectedSentMessages, expectedReceivedMessages);

		verifyNoMoreInteractions(listener, listener2);
	}

	@Test
	public void testPublish_Qos1_Retain() throws Exception {

		// connect a client and generate the message
		client = new SyncMqttClient(brokerUri, listener, 5, config);
		assertEquals(ConnectReturnCode.ACCEPTED, client.connect("testclient16", true));
		client.publish(new PublishMessage("my/topic6", QoS.AT_LEAST_ONCE, "my message", true));
		client.disconnect();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		// connect and subscribe a client to get the messages
		MqttClientListener listener2 = mock(MqttClientListener.class);
		client2 = new SyncMqttClient(brokerUri, listener2, 5, config);
		assertEquals(ConnectReturnCode.ACCEPTED, client2.connect("testclient15", true));
		assertNotNull(client2.subscribe(new Subscription[] { new Subscription("my/topic6", QoS.AT_LEAST_ONCE) }));

		// verify the messages
		verify(listener2, timeout(5000)).publishReceived(same(client2), messageCaptor.capture());

		PublishMessage message = messageCaptor.getValue();
		message.ack();
		assertEquals("my/topic6", message.getTopic());
		assertEquals("my message", message.getPayloadString());
		assertEquals(QoS.AT_LEAST_ONCE, message.getQoS());
		assertFalse(message.isDuplicate());
		assertTrue(message.isRetain());

		// remove the message
		client2.publish(new PublishMessage("my/topic6", QoS.AT_LEAST_ONCE, new byte[0], true));
		verify(listener2, timeout(5000)).publishReceived(same(client2), isA(PublishMessage.class));

		client2.disconnect();
		verify(listener2, timeout(5000)).disconnected(same(client2), any(Throwable.class), eq(false));

		verifyOpenExchangeCloseCadence(new Class<?>[] { ConnectMessage.class, PubMessage.class, DisconnectMessage.class, ConnectMessage.class,
				SubscribeMessage.class, PubAckMessage.class, PubMessage.class, DisconnectMessage.class }, new Class<?>[] { ConnAckMessage.class,
				PubAckMessage.class, ConnAckMessage.class, SubAckMessage.class, PubMessage.class, PubAckMessage.class });

		verifyNoMoreInteractions(listener, listener2);
	}

	@Test
	public void testPublish_Qos0_NoRetain() throws Exception {

		// connect and subscribe a client to get the messages
		MqttClientListener listener2 = mock(MqttClientListener.class);
		client2 = new SyncMqttClient(brokerUri, listener2, 5, config);
		assertEquals(ConnectReturnCode.ACCEPTED, client2.connect("testclient15", true));
		assertNotNull(client2.subscribe(new Subscription[] { new Subscription("my/topic7", QoS.AT_LEAST_ONCE) }));

		// connect a client and generate the messages
		client = new SyncMqttClient(brokerUri, listener, 5, config);
		assertEquals(ConnectReturnCode.ACCEPTED, client.connect("testclient16", true));
		for (int i = 0; i < 10; i++) {
			client.publish(new PublishMessage("my/topic7", QoS.AT_MOST_ONCE, "my message " + i));
		}

		// verify the messages
		verify(listener2, timeout(5000).times(10)).publishReceived(same(client2), messageCaptor.capture());

		for (PublishMessage message : messageCaptor.getAllValues()) {
			message.ack();
			assertEquals("my/topic7", message.getTopic());
			assertTrue(message.getPayloadString().startsWith("my message "));
			assertEquals(QoS.AT_MOST_ONCE, message.getQoS());
			assertFalse(message.isDuplicate());
			assertFalse(message.isRetain());
		}

		client.disconnect();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		client2.disconnect();
		verify(listener2, timeout(5000)).disconnected(same(client2), any(Throwable.class), eq(false));

		Class<?>[] expectedSentMessages = new Class<?>[15];
		expectedSentMessages[0] = ConnectMessage.class;
		expectedSentMessages[1] = SubscribeMessage.class;
		expectedSentMessages[2] = ConnectMessage.class;
		for (int i = 0; i < 10; i++) {
			expectedSentMessages[i + 3] = PubMessage.class;
		}
		expectedSentMessages[13] = DisconnectMessage.class;
		expectedSentMessages[14] = DisconnectMessage.class;

		Class<?>[] expectedReceivedMessages = new Class<?>[13];
		expectedReceivedMessages[0] = ConnAckMessage.class;
		expectedReceivedMessages[1] = SubAckMessage.class;
		expectedReceivedMessages[2] = ConnAckMessage.class;
		for (int i = 3; i < 13; i++) {
			expectedReceivedMessages[i] = PubMessage.class;
		}

		verifyOpenExchangeCloseCadence(expectedSentMessages, expectedReceivedMessages);

		verifyNoMoreInteractions(listener, listener2);
	}

	// this test can take up to 30 seconds to run
	@Test
	public void testPublish_DuplicateMessageReceived() throws Exception {

		// connect and subscribe a client to get the message
		MqttClientListener listener2 = mock(MqttClientListener.class);
		client2 = new SyncMqttClient(brokerUri, listener2, 5, config);
		assertEquals(ConnectReturnCode.ACCEPTED, client2.connect("testclient18", true));
		assertNotNull(client2.subscribe(new Subscription[] { new Subscription("my/topic7", QoS.AT_LEAST_ONCE) }));

		// connect a client and generate the message
		client = new SyncMqttClient(brokerUri, listener, 5, config);
		assertEquals(ConnectReturnCode.ACCEPTED, client.connect("testclient17", true));
		client.publish(new PublishMessage("my/topic7", QoS.AT_LEAST_ONCE, "my message"));
		client.disconnect();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		// verify the messages
		verify(listener2, timeout(30000).times(2)).publishReceived(same(client2), messageCaptor.capture());

		PublishMessage message = messageCaptor.getAllValues().get(0);
		assertEquals("my/topic7", message.getTopic());
		assertEquals("my message", message.getPayloadString());
		assertEquals(QoS.AT_LEAST_ONCE, message.getQoS());
		assertFalse(message.isDuplicate());
		assertFalse(message.isRetain());
		message = messageCaptor.getAllValues().get(1);
		message.ack();
		assertEquals("my/topic7", message.getTopic());
		assertEquals("my message", message.getPayloadString());
		assertEquals(QoS.AT_LEAST_ONCE, message.getQoS());
		assertTrue(message.isDuplicate());
		assertFalse(message.isRetain());

		client2.disconnect();
		verify(listener2, timeout(5000)).disconnected(same(client2), any(Throwable.class), eq(false));

		verifyOpenExchangeCloseCadence(new Class<?>[] { ConnectMessage.class, SubscribeMessage.class, ConnectMessage.class, PubMessage.class,
				PubAckMessage.class, DisconnectMessage.class, DisconnectMessage.class }, new Class<?>[] { ConnAckMessage.class, SubAckMessage.class,
				ConnAckMessage.class, PubMessage.class, PubMessage.class, PubAckMessage.class });

		verifyNoMoreInteractions(listener, listener2);
	}

	@Test
	public void testClose() throws Exception {

		client = new SyncMqttClient(brokerUri, listener, 5, config);
		client.connect("testclient19", true);
		assertFalse(client.isClosed());
		client.close();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));
		verifyOpenExchangeCloseCadence(new Class<?>[] { ConnectMessage.class }, new Class<?>[] { ConnAckMessage.class });
		assertTrue(client.isClosed());
	}

	@Test
	public void testDisconnect() throws Exception {

		client = new SyncMqttClient(brokerUri, listener, 5, config);
		client.connect("testclient20", true);
		assertFalse(client.isClosed());

		client.disconnect();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));
		assertTrue(client.isClosed());

		verifyOpenExchangeCloseCadence(new Class<?>[] { ConnectMessage.class, DisconnectMessage.class }, new Class<?>[] { ConnAckMessage.class });
	}

	@Test
	public void testConnectMessageTimesOut() throws Exception {

		mockBroker = new MockBroker(mockHandler, 15, 0, true, true, 50);
		mockBroker.init();
		mockBroker.addCredentials("user1", "password1");
		brokerUri = "tcp://localhost:" + mockBroker.getPort();

		when(reconnectionStrategy.connectionLost(isA(MqttClient.class), isA(MqttTimeoutException.class))).thenReturn(-1L);

		when(mockHandler.connect(isA(Client.class), isA(ConnectMessage.class))).thenReturn(true);

		config.setConnectTimeoutSeconds(1);
		client = new SyncMqttClient(brokerUri, listener, 5, config);
		long start = System.currentTimeMillis();

		try {
			client.connect("testclient20", true);
			fail("expected exception");
		} catch (MqttInvocationException e) {
			assertEquals(MqttTimeoutException.class, e.getRootCause().getClass());
		}

		verify(listener, timeout(1500)).disconnected(eq(client), isA(MqttTimeoutException.class), eq(false));
		assertTrue(System.currentTimeMillis() - start > 500);

		verifyOpenExchangeCloseCadence(new Class<?>[] { ConnectMessage.class }, new Class<?>[] {});

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
		brokerUri = "tcp://localhost:" + mockBroker.getPort();

		config.setConnectTimeoutSeconds(5);
		// connect to the broker and send the pub message which will cause the channel to close
		client = new SyncMqttClient(brokerUri, listener, 5, config);
		client.connect("testclient20", true);
		verify(reconnectionStrategy, timeout(5000)).connectionEstablished();
		PublishMessage pubMessage = new PublishMessage("foo", QoS.AT_LEAST_ONCE, "abc");
		long start = System.currentTimeMillis();
		client.publish(pubMessage);

		// verify connection was lost
		verify(reconnectionStrategy).connectionLost(isA(MqttClient.class), isNull(Throwable.class));
		verify(listener).disconnected(client, null, true);

		// verify reconnect in about 1 second
		verify(reconnectionStrategy, times(2)).connectionEstablished();
		long elapsed = System.currentTimeMillis() - start;
		assertTrue(elapsed > 500);
		assertTrue(elapsed < 1500);

		// disconnect the client
		client.disconnect();
		verify(listener, timeout(5000)).disconnected(client, null, false);

		verifyOpenExchangeCloseCadence(2, 2, new Class<?>[] { ConnectMessage.class, PubMessage.class, ConnectMessage.class, PubMessage.class,
				DisconnectMessage.class }, new Class<?>[] { ConnAckMessage.class, ConnAckMessage.class, PubAckMessage.class });
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
		brokerUri = "tcp://localhost:" + mockBroker.getPort();

		config.setConnectTimeoutSeconds(5);
		// connect to the broker and send the pub message which will cause the channel to close
		client = new SyncMqttClient(brokerUri, listener, 5, config);
		client.connect("testclient20", true);
		verify(reconnectionStrategy, timeout(5000)).connectionEstablished();
		PublishMessage pubMessage = new PublishMessage("foo", QoS.AT_LEAST_ONCE, "abc");
		client.publish(pubMessage);

		// verify 3 connects and 2 disconnects
		verify(reconnectionStrategy, times(3)).connectionEstablished();
		verify(reconnectionStrategy, times(2)).connectionLost(isA(MqttClient.class), isNull(Throwable.class));
		verify(listener, times(2)).disconnected(client, null, true);

		// disconnect the client
		client.disconnect();
		verify(listener, timeout(5000)).disconnected(client, null, false);

		verifyOpenExchangeCloseCadence(3, 3, new Class<?>[] { ConnectMessage.class, PubMessage.class, ConnectMessage.class, PubMessage.class,
				ConnectMessage.class, PubMessage.class, DisconnectMessage.class }, new Class<?>[] { ConnAckMessage.class, ConnAckMessage.class,
				ConnAckMessage.class, PubAckMessage.class });
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
		brokerUri = "tcp://localhost:" + mockBroker.getPort();

		config.setConnectTimeoutSeconds(5);
		// connect to the broker and send the pub message which will cause the channel to close
		client = new SyncMqttClient(brokerUri, listener, 5, config);
		client.connect("testclient20", true);
		verify(reconnectionStrategy, timeout(5000)).connectionEstablished();
		assertFalse(client.isClosed());
		PublishMessage pubMessage = new PublishMessage("foo", QoS.AT_LEAST_ONCE, "abc");
		try {
			client.publish(pubMessage);
			fail("expected exception");
		} catch (MqttCommandCancelledException e) {

		}

		// verify 4 connects and 3 disconnects twice
		verify(reconnectionStrategy, times(4)).connectionEstablished();
		verify(reconnectionStrategy, times(4)).connectionLost(isA(MqttClient.class), isNull(Throwable.class));
		verify(listener, times(3)).disconnected(client, null, true);
		verify(listener, timeout(5000)).disconnected(client, null, false);
		verifyOpenExchangeCloseCadence(4, 4, new Class<?>[] { ConnectMessage.class, PubMessage.class, ConnectMessage.class, PubMessage.class,
				ConnectMessage.class, PubMessage.class, ConnectMessage.class, PubMessage.class }, new Class<?>[] { ConnAckMessage.class, ConnAckMessage.class,
				ConnAckMessage.class, ConnAckMessage.class });
		assertTrue(client.isClosed());
	}

	private void verifyOpenExchangeCloseCadence(Class<?>[] expectedSentMessages, Class<?>[] expectedReceivedMessages) {
		verifyOpenExchangeCloseCadence(1, 1, expectedSentMessages, expectedReceivedMessages);
	}

	private void verifyOpenExchangeCloseCadence(int expectedConnectionOpenCount, int expectedConnectionClosedCount, Class<?>[] expectedSentMessages,
			Class<?>[] expectedReceivedMessages) {
		verify(clientDebugListener, timeout(5000).times(expectedConnectionOpenCount)).connectionOpened(eq(client), anyString(), anyString());

		if (expectedSentMessages != null) {
			assertEquals(expectedSentMessages.length, sentMessages.size());
			for (Class<?> clazz : expectedSentMessages) {
				sentMessages.remove(clazz);
			}
			assertTrue(sentMessages.isEmpty());
		}

		if (expectedReceivedMessages != null) {
			assertEquals(expectedReceivedMessages.length, receivedMessages.size());
			for (Class<?> clazz : expectedReceivedMessages) {
				receivedMessages.remove(clazz);
			}
			assertTrue("Remaining: " + receivedMessages, receivedMessages.isEmpty());
		}

		verify(clientDebugListener, timeout(5000).times(expectedConnectionClosedCount)).connectionClosed(eq(client), anyString(), anyString());
	}

}
