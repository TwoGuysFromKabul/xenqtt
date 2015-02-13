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
package net.xenqtt.integration;

import static org.junit.Assert.*;
import static org.mockito.AdditionalMatchers.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;

import net.xenqtt.MqttCommandCancelledException;
import net.xenqtt.client.AsyncClientListener;
import net.xenqtt.client.AsyncMqttClient;
import net.xenqtt.client.MqttClientConfig;
import net.xenqtt.client.PublishMessage;
import net.xenqtt.client.ReconnectionStrategy;
import net.xenqtt.client.Subscription;
import net.xenqtt.message.ConnectReturnCode;
import net.xenqtt.message.QoS;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Common tests for {@link AsyncMqttClientIT} and {@link MockBrokerIT}
 */
public abstract class AbstractAsyncMqttClientIT {

	MqttClientConfig config = new MqttClientConfig().setConnectTimeoutSeconds(0).setMessageResendIntervalSeconds(5);

	String validBrokerUri = "tcp://test.mosquitto.org:1883";

	@Mock AsyncClientListener listener;
	@Mock ReconnectionStrategy reconnectionStrategy;
	@Captor ArgumentCaptor<Subscription[]> subscriptionCaptor;;
	@Captor ArgumentCaptor<PublishMessage> messageCaptor;;

	AsyncMqttClient client;
	AsyncMqttClient client2;

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		config.setReconnectionStrategy(reconnectionStrategy);
		when(reconnectionStrategy.clone()).thenReturn(reconnectionStrategy);
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
	}

	@Test
	public final void testConnectDisconnect_NoCredentialsNoWill() throws Exception {

		client = new AsyncMqttClient(validBrokerUri, listener, 5, config);
		client.connect("testclient1", true);
		verify(listener, timeout(5000)).connected(client, ConnectReturnCode.ACCEPTED);
		verify(reconnectionStrategy, timeout(5000)).connectionEstablished();

		client.disconnect();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));
		verify(reconnectionStrategy).clone();

		verifyNoMoreInteractions(listener, reconnectionStrategy);
	}

	@Test
	public final void testConnect_Will_NoRetain_Subscribed() throws Exception {

		// connect and subscribe a client to get the will message
		AsyncClientListener listener2 = mock(AsyncClientListener.class);
		client2 = new AsyncMqttClient(validBrokerUri, listener2, 5, config);
		client2.connect("testclient3", true);
		verify(listener2, timeout(5000)).connected(client2, ConnectReturnCode.ACCEPTED);
		client2.subscribe(new Subscription[] { new Subscription("my/will/topic1", QoS.AT_LEAST_ONCE) });
		verify(listener2, timeout(5000)).subscribed(same(client2), any(Subscription[].class), any(Subscription[].class), eq(true));

		// connect and close a client to generate the will message
		client = new AsyncMqttClient(validBrokerUri, listener, 5, config);
		client.connect("testclient4", true, "my/will/topic1", "it died dude", QoS.AT_LEAST_ONCE, false);
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
	public final void testConnect_Will_NoRetain_NotSubscribed() throws Exception {

		// connect and close a client to generate the will message
		client = new AsyncMqttClient(validBrokerUri, listener, 5, config);
		client.connect("testclient5", true, "my/will/topic2", "it died dude", QoS.AT_LEAST_ONCE, false);
		verify(listener, timeout(5000)).connected(client, ConnectReturnCode.ACCEPTED);
		client.close();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		// connect and subscribe a client to get the will message
		AsyncClientListener listener2 = mock(AsyncClientListener.class);
		client2 = new AsyncMqttClient(validBrokerUri, listener2, 5, config);
		client2.connect("testclient6", true);
		verify(listener2, timeout(5000)).connected(client2, ConnectReturnCode.ACCEPTED);
		client2.subscribe(new Subscription[] { new Subscription("my/will/topic2", QoS.AT_LEAST_ONCE) });
		verify(listener2, timeout(5000)).subscribed(same(client2), any(Subscription[].class), any(Subscription[].class), eq(true));
		// verify no will message
		Thread.sleep(1000);
		verify(listener2, never()).publishReceived(same(client2), any(PublishMessage.class));
		client2.disconnect();
		verify(listener2, timeout(5000)).disconnected(same(client2), any(Throwable.class), eq(false));

		verifyNoMoreInteractions(listener, listener2);
	}

	@Test
	public final void testConnect_Will_Retain_NotSubscribed() throws Exception {

		// connect and close a client to generate the will message
		client = new AsyncMqttClient(validBrokerUri, listener, 5, config);
		client.connect("testclient7", true, "my/will/topic3", "it died dude", QoS.AT_LEAST_ONCE, true);
		verify(listener, timeout(5000)).connected(client, ConnectReturnCode.ACCEPTED);
		client.close();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		// connect and subscribe a client to get the will message
		AsyncClientListener listener2 = mock(AsyncClientListener.class);
		client2 = new AsyncMqttClient(validBrokerUri, listener2, 5, config);
		client2.connect("testclient8", true);
		verify(listener2, timeout(5000)).connected(client2, ConnectReturnCode.ACCEPTED);
		client2.subscribe(new Subscription[] { new Subscription("my/will/topic3", QoS.AT_LEAST_ONCE) });
		verify(listener2, timeout(5000)).subscribed(same(client2), any(Subscription[].class), any(Subscription[].class), eq(true));

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
		verify(listener2, timeout(5000)).published(same(client2), isA(PublishMessage.class));
		verify(listener2, timeout(5000)).publishReceived(same(client2), isA(PublishMessage.class));

		client2.disconnect();
		verify(listener2, timeout(5000)).disconnected(same(client2), any(Throwable.class), eq(false));

		verifyNoMoreInteractions(listener, listener2);
	}

	@Test
	public final void testConnect_Will_Retain_Subscribed() throws Exception {

		// connect and close a client to generate the will message
		client = new AsyncMqttClient(validBrokerUri, listener, 5, config);
		client.connect("testclient10", true, "my/will/topic4", "it died dude", QoS.AT_LEAST_ONCE, true);
		verify(listener, timeout(5000)).connected(client, ConnectReturnCode.ACCEPTED);
		client.close();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		// connect and subscribe a client to get the will message
		AsyncClientListener listener2 = mock(AsyncClientListener.class);
		client2 = new AsyncMqttClient(validBrokerUri, listener2, 5, config);
		client2.connect("testclient9", true);
		verify(listener2, timeout(5000)).connected(client2, ConnectReturnCode.ACCEPTED);
		client2.subscribe(new Subscription[] { new Subscription("my/will/topic4", QoS.AT_LEAST_ONCE) });
		verify(listener2, timeout(5000)).subscribed(same(client2), any(Subscription[].class), any(Subscription[].class), eq(true));

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
		verify(listener2, timeout(5000)).published(same(client2), isA(PublishMessage.class));
		verify(listener2, timeout(5000)).publishReceived(same(client2), isA(PublishMessage.class));

		client2.disconnect();
		verify(listener2, timeout(5000)).disconnected(same(client2), any(Throwable.class), eq(false));

		verifyNoMoreInteractions(listener, listener2);
	}

	@Test
	public final void testSubscribeUnsubscribe_Array() throws Exception {

		// connect client
		client = new AsyncMqttClient(validBrokerUri, listener, 5, config);
		client.connect("testclient11", true);
		verify(listener, timeout(5000)).connected(client, ConnectReturnCode.ACCEPTED);

		// test subscribing
		Subscription[] requestedSubscriptions = new Subscription[] { new Subscription("my/topic1", QoS.AT_LEAST_ONCE),
				new Subscription("my/topic2", QoS.AT_MOST_ONCE) };
		Subscription[] grantedSubscriptions = requestedSubscriptions;
		client.subscribe(requestedSubscriptions);
		verify(listener, timeout(5000)).subscribed(same(client), same(requestedSubscriptions), aryEq(grantedSubscriptions), eq(true));

		// test unsubscribing
		client.unsubscribe(new String[] { "my/topic1", "my/topic2" });
		verify(listener, timeout(5000)).unsubscribed(same(client), aryEq(new String[] { "my/topic1", "my/topic2" }));

		// disconnect
		client.disconnect();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		verifyNoMoreInteractions(listener);
	}

	@Test
	public final void testSubscribeUnsubscribe_List() throws Exception {

		// connect client
		client = new AsyncMqttClient(validBrokerUri, listener, 5, config);
		client.connect("testclient12", true);
		verify(listener, timeout(5000)).connected(client, ConnectReturnCode.ACCEPTED);

		// test subscribing
		Subscription[] requestedSubscriptions = new Subscription[] { new Subscription("my/topic3", QoS.AT_LEAST_ONCE),
				new Subscription("my/topic4", QoS.AT_MOST_ONCE) };
		List<Subscription> requestedSubscriptionsList = Arrays.asList(requestedSubscriptions);
		Subscription[] grantedSubscriptions = requestedSubscriptions;
		client.subscribe(requestedSubscriptionsList);
		verify(listener, timeout(5000)).subscribed(same(client), aryEq(requestedSubscriptions), aryEq(grantedSubscriptions), eq(true));

		// test unsubscribing
		client.unsubscribe(Arrays.asList(new String[] { "my/topic3", "my/topic4" }));
		verify(listener, timeout(5000)).unsubscribed(same(client), aryEq(new String[] { "my/topic3", "my/topic4" }));

		// disconnect
		client.disconnect();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		verifyNoMoreInteractions(listener);
	}

	@Test
	public final void testPublish_Qos1_NoRetain() throws Exception {

		// connect and subscribe a client to get the messages
		AsyncClientListener listener2 = mock(AsyncClientListener.class);
		client2 = new AsyncMqttClient(validBrokerUri, listener2, 5, config);
		client2.connect("testclient13", true);
		verify(listener2, timeout(5000)).connected(client2, ConnectReturnCode.ACCEPTED);
		client2.subscribe(new Subscription[] { new Subscription("my/topic5", QoS.AT_LEAST_ONCE) });
		verify(listener2, timeout(5000)).subscribed(same(client2), any(Subscription[].class), any(Subscription[].class), eq(true));

		// connect a client and generate the messages
		client = new AsyncMqttClient(validBrokerUri, listener, 5, config);
		client.connect("testclient14", true);
		verify(listener, timeout(5000)).connected(client, ConnectReturnCode.ACCEPTED);
		for (int i = 0; i < 10; i++) {
			client.publish(new PublishMessage("my/topic5", QoS.AT_LEAST_ONCE, "my message " + i));
		}
		verify(listener, timeout(5000).times(10)).published(same(client), isA(PublishMessage.class));
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

		verifyNoMoreInteractions(listener, listener2);
	}

	@Test
	public final void testPublish_Qos1_Retain() throws Exception {

		// connect a client and generate the message
		client = new AsyncMqttClient(validBrokerUri, listener, 5, config);
		client.connect("testclient16", true);
		verify(listener, timeout(5000)).connected(client, ConnectReturnCode.ACCEPTED);
		client.publish(new PublishMessage("my/topic6", QoS.AT_LEAST_ONCE, "my message", true));
		verify(listener, timeout(5000)).published(same(client), isA(PublishMessage.class));
		client.disconnect();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		// connect and subscribe a client to get the messages
		AsyncClientListener listener2 = mock(AsyncClientListener.class);
		client2 = new AsyncMqttClient(validBrokerUri, listener2, 5, config);
		client2.connect("testclient15", true);
		verify(listener2, timeout(5000)).connected(client2, ConnectReturnCode.ACCEPTED);
		client2.subscribe(new Subscription[] { new Subscription("my/topic6", QoS.AT_LEAST_ONCE) });
		verify(listener2, timeout(5000)).subscribed(same(client2), any(Subscription[].class), any(Subscription[].class), eq(true));

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
		verify(listener2, timeout(5000)).published(same(client2), isA(PublishMessage.class));
		verify(listener2, timeout(5000)).publishReceived(same(client2), isA(PublishMessage.class));

		client2.disconnect();
		verify(listener2, timeout(5000)).disconnected(same(client2), any(Throwable.class), eq(false));

		verifyNoMoreInteractions(listener, listener2);
	}

	@Test
	public final void testPublish_Qos0_NoRetain() throws Exception {

		// connect and subscribe a client to get the messages
		AsyncClientListener listener2 = mock(AsyncClientListener.class);
		client2 = new AsyncMqttClient(validBrokerUri, listener2, 5, config);
		client2.connect("testclient15", true);
		verify(listener2, timeout(5000)).connected(client2, ConnectReturnCode.ACCEPTED);
		client2.subscribe(new Subscription[] { new Subscription("my/topic7", QoS.AT_LEAST_ONCE) });
		verify(listener2, timeout(5000)).subscribed(same(client2), any(Subscription[].class), any(Subscription[].class), eq(true));

		// connect a client and generate the messages
		client = new AsyncMqttClient(validBrokerUri, listener, 5, config);
		client.connect("testclient16", true);
		verify(listener, timeout(5000)).connected(client, ConnectReturnCode.ACCEPTED);
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

		verifyNoMoreInteractions(listener, listener2);
	}

	// this test can take up to 30 seconds to run
	@Test
	public final void testPublish_DuplicateMessageReceived() throws Exception {

		// connect and subscribe a client to get the message
		AsyncClientListener listener2 = mock(AsyncClientListener.class);
		client2 = new AsyncMqttClient(validBrokerUri, listener2, 5, config);
		client2.connect("testclient18", true);
		verify(listener2, timeout(5000)).connected(client2, ConnectReturnCode.ACCEPTED);
		client2.subscribe(new Subscription[] { new Subscription("my/topic7", QoS.AT_LEAST_ONCE) });
		verify(listener2, timeout(5000)).subscribed(same(client2), any(Subscription[].class), any(Subscription[].class), eq(true));

		// connect a client and generate the message
		client = new AsyncMqttClient(validBrokerUri, listener, 5, config);
		client.connect("testclient17", true);
		verify(listener, timeout(5000)).connected(client, ConnectReturnCode.ACCEPTED);
		client.publish(new PublishMessage("my/topic7", QoS.AT_LEAST_ONCE, "my message"));
		verify(listener, timeout(5000)).published(same(client), isA(PublishMessage.class));
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

		verifyNoMoreInteractions(listener, listener2);
	}
}
