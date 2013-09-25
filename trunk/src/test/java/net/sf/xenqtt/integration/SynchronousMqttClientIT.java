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
import java.util.Arrays;
import java.util.List;

import net.sf.xenqtt.MqttCommandCancelledException;
import net.sf.xenqtt.MqttException;
import net.sf.xenqtt.MqttTimeoutException;
import net.sf.xenqtt.client.MqttClient;
import net.sf.xenqtt.client.MqttClientListener;
import net.sf.xenqtt.client.PublishMessage;
import net.sf.xenqtt.client.ReconnectionStrategy;
import net.sf.xenqtt.client.Subscription;
import net.sf.xenqtt.client.SynchronousMqttClient;
import net.sf.xenqtt.message.ConnectReturnCode;
import net.sf.xenqtt.message.QoS;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SynchronousMqttClientIT {

	String badCredentialsUri = "tcp://q.m2m.io:1883";
	String validBrokerUrl = "tcp://test.mosquitto.org:1883";

	@Mock MqttClientListener listener;
	@Mock ReconnectionStrategy reconnectionStrategy;
	@Captor ArgumentCaptor<PublishMessage> messageCaptor;;

	SynchronousMqttClient client;
	SynchronousMqttClient client2;

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
	}

	@After
	public void after() {

		if (client != null) {
			client.shutdown();
		}
		if (client2 != null) {
			client2.shutdown();
		}
	}

	@Test
	public void test() {
		fail("Not yet implemented");
	}

	@Test
	public void testConstructor_InvalidScheme() throws Exception {

		try {
			client = new SynchronousMqttClient("ftp://foo:1883", listener, reconnectionStrategy, 5, 0, 5, 10);
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
			client = new SynchronousMqttClient("tcp://foo:1883", listener, reconnectionStrategy, 5, 0, 5, 10);
			fail("expected exception");
		} catch (MqttException e) {
			thrown = e.getCause();
			assertEquals(UnresolvedAddressException.class, thrown.getClass());
		}

		verify(listener, timeout(5000)).disconnected(any(SynchronousMqttClient.class), same(thrown), eq(false));

		verifyNoMoreInteractions(listener);
		verifyZeroInteractions(reconnectionStrategy);
	}

	// This test can take over a minute to run so ignore it by default
	@Ignore
	@Test
	public void testConstructor_InvalidPort() throws Exception {

		try {
			client = new SynchronousMqttClient("tcp://test.mosquitto.org:1234", listener, reconnectionStrategy, 5, 0, 5, 1000);
			fail("expected exception");
		} catch (MqttCommandCancelledException e) {
			verifyZeroInteractions(listener, reconnectionStrategy);

			verify(listener, timeout(1000)).disconnected(any(MqttClient.class), any(ConnectException.class), eq(false));
			verifyNoMoreInteractions(listener);
			verifyZeroInteractions(reconnectionStrategy);
		}
	}

	@Test
	public void testConstructorTimesOut() throws Exception {

		try {
			client = new SynchronousMqttClient("tcp://test.mosquitto.org:1234", listener, reconnectionStrategy, 5, 0, 5, 1);
			fail("expected exception");
		} catch (MqttTimeoutException e) {
			verifyZeroInteractions(listener, reconnectionStrategy);
		}
	}

	@Test
	public void testConnectDisconnect_NoCredentialsNoWill() throws Exception {

		client = new SynchronousMqttClient(validBrokerUrl, listener, reconnectionStrategy, 5, 0, 5, 10);
		assertEquals(ConnectReturnCode.ACCEPTED, client.connect("testclient1", true, 90));
		verify(reconnectionStrategy, timeout(5000)).connectionEstablished();

		client.disconnect();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		verifyNoMoreInteractions(listener, reconnectionStrategy);
	}

	@Test
	public void testConnect_Credentials_BadCredentials() throws Exception {

		client = new SynchronousMqttClient(badCredentialsUri, listener, reconnectionStrategy, 5, 0, 5, 10);
		assertEquals(ConnectReturnCode.BAD_CREDENTIALS, client.connect("testclient2", true, 90, "not_a_user", "not_a_password"));

		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		verifyNoMoreInteractions(listener);
		verifyZeroInteractions(reconnectionStrategy);
	}

	@Test
	public void testConnect_Credentials_Accepted() throws Exception {

		fail("not implemented");
	}

	@Test
	public void testConnect_Will_NoRetain_Subscribed() throws Exception {

		// connect and subscribe a client to get the will message
		MqttClientListener listener2 = mock(MqttClientListener.class);
		client2 = new SynchronousMqttClient(validBrokerUrl, listener2, reconnectionStrategy, 5, 0, 5, 10);
		assertEquals(ConnectReturnCode.ACCEPTED, client2.connect("testclient3", true, 90));
		assertNotNull(client2.subscribe(new Subscription[] { new Subscription("my/will/topic1", QoS.AT_LEAST_ONCE) }));

		// connect and close a client to generate the will message
		client = new SynchronousMqttClient(validBrokerUrl, listener, reconnectionStrategy, 5, 0, 5, 10);
		assertEquals(ConnectReturnCode.ACCEPTED, client.connect("testclient4", true, 90, "my/will/topic1", "it died dude", QoS.AT_LEAST_ONCE, false));
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
	public void testConnect_Will_NoRetain_NotSubscribed() throws Exception {

		// connect and close a client to generate the will message
		client = new SynchronousMqttClient(validBrokerUrl, listener, reconnectionStrategy, 5, 0, 5, 10);
		assertEquals(ConnectReturnCode.ACCEPTED, client.connect("testclient5", true, 90, "my/will/topic2", "it died dude", QoS.AT_LEAST_ONCE, false));
		client.close();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		// connect and subscribe a client to get the will message
		MqttClientListener listener2 = mock(MqttClientListener.class);
		client2 = new SynchronousMqttClient(validBrokerUrl, listener2, reconnectionStrategy, 5, 0, 5, 10);
		assertEquals(ConnectReturnCode.ACCEPTED, client2.connect("testclient6", true, 90));
		assertNotNull(client2.subscribe(new Subscription[] { new Subscription("my/will/topic2", QoS.AT_LEAST_ONCE) }));
		// verify no will message
		Thread.sleep(1000);
		verify(listener2, never()).publishReceived(same(client2), any(PublishMessage.class));
		client2.disconnect();
		verify(listener2, timeout(5000)).disconnected(same(client2), any(Throwable.class), eq(false));

		verifyNoMoreInteractions(listener, listener2);
	}

	@Test
	public void testConnect_Will_Retain_NotSubscribed() throws Exception {

		// connect and close a client to generate the will message
		client = new SynchronousMqttClient(validBrokerUrl, listener, reconnectionStrategy, 5, 0, 5, 10);
		assertEquals(ConnectReturnCode.ACCEPTED, client.connect("testclient7", true, 90, "my/will/topic3", "it died dude", QoS.AT_LEAST_ONCE, true));

		client.close();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		// connect and subscribe a client to get the will message
		MqttClientListener listener2 = mock(MqttClientListener.class);
		client2 = new SynchronousMqttClient(validBrokerUrl, listener2, reconnectionStrategy, 5, 0, 5, 10);
		assertEquals(ConnectReturnCode.ACCEPTED, client2.connect("testclient8", true, 90));
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

		verifyNoMoreInteractions(listener, listener2);
	}

	@Test
	public void testConnect_Will_Retain_Subscribed() throws Exception {

		// connect and close a client to generate the will message
		client = new SynchronousMqttClient(validBrokerUrl, listener, reconnectionStrategy, 5, 0, 5, 10);
		assertEquals(ConnectReturnCode.ACCEPTED, client.connect("testclient10", true, 90, "my/will/topic4", "it died dude", QoS.AT_LEAST_ONCE, true));
		client.close();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		// connect and subscribe a client to get the will message
		MqttClientListener listener2 = mock(MqttClientListener.class);
		client2 = new SynchronousMqttClient(validBrokerUrl, listener2, reconnectionStrategy, 5, 0, 5, 10);
		assertEquals(ConnectReturnCode.ACCEPTED, client2.connect("testclient9", true, 90));
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

		verifyNoMoreInteractions(listener, listener2);
	}

	@Test
	public void testConnect_CredentialsAndWill_Accepted() throws Exception {

		fail("not implemented");
	}

	@Test
	public void testSubscribeUnsubscribe_Array() throws Exception {

		// connect client
		client = new SynchronousMqttClient(validBrokerUrl, listener, reconnectionStrategy, 5, 0, 5, 10);
		assertEquals(ConnectReturnCode.ACCEPTED, client.connect("testclient11", true, 90));

		// test subscribing
		Subscription[] requestedSubscriptions = new Subscription[] { new Subscription("my/topic1", QoS.AT_LEAST_ONCE),
				new Subscription("my/topic2", QoS.AT_MOST_ONCE) };
		Subscription[] grantedSubscriptions = requestedSubscriptions;
		assertArrayEquals(grantedSubscriptions, client.subscribe(requestedSubscriptions));

		// test unsubscribing
		// FIXME [jim] - how to verify this?
		client.unsubscribe(new String[] { "my/topic1", "my/topic2" });

		// disconnect
		client.disconnect();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		verifyNoMoreInteractions(listener);
	}

	@Test
	public void testSubscribeUnsubscribe_List() throws Exception {

		// connect client
		client = new SynchronousMqttClient(validBrokerUrl, listener, reconnectionStrategy, 5, 0, 5, 10);
		assertEquals(ConnectReturnCode.ACCEPTED, client.connect("testclient12", true, 90));

		// test subscribing
		List<Subscription> requestedSubscriptions = Arrays.asList(new Subscription[] { new Subscription("my/topic3", QoS.AT_LEAST_ONCE),
				new Subscription("my/topic4", QoS.AT_MOST_ONCE) });
		List<Subscription> grantedSubscriptions = requestedSubscriptions;
		assertEquals(grantedSubscriptions, client.subscribe(requestedSubscriptions));

		// test unsubscribing
		// FIXME [jim] - how to verify this?
		client.unsubscribe(Arrays.asList(new String[] { "my/topic3", "my/topic4" }));

		// disconnect
		client.disconnect();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		verifyNoMoreInteractions(listener);
	}

	@Test
	public void testPublish_Qos1_NoRetain() throws Exception {

		// connect and subscribe a client to get the messages
		MqttClientListener listener2 = mock(MqttClientListener.class);
		client2 = new SynchronousMqttClient(validBrokerUrl, listener2, reconnectionStrategy, 5, 0, 5, 10);
		assertEquals(ConnectReturnCode.ACCEPTED, client2.connect("testclient13", true, 90));
		assertNotNull(client2.subscribe(new Subscription[] { new Subscription("my/topic5", QoS.AT_LEAST_ONCE) }));

		// connect a client and generate the messages
		client = new SynchronousMqttClient(validBrokerUrl, listener, reconnectionStrategy, 5, 0, 5, 10);
		assertEquals(ConnectReturnCode.ACCEPTED, client.connect("testclient14", true, 90));
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

		verifyNoMoreInteractions(listener, listener2);
	}

	@Test
	public void testPublish_Qos1_Retain() throws Exception {

		// connect a client and generate the message
		client = new SynchronousMqttClient(validBrokerUrl, listener, reconnectionStrategy, 5, 0, 5, 10);
		assertEquals(ConnectReturnCode.ACCEPTED, client.connect("testclient16", true, 90));
		client.publish(new PublishMessage("my/topic6", QoS.AT_LEAST_ONCE, "my message", true));
		client.disconnect();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		// connect and subscribe a client to get the messages
		MqttClientListener listener2 = mock(MqttClientListener.class);
		client2 = new SynchronousMqttClient(validBrokerUrl, listener2, reconnectionStrategy, 5, 0, 5, 10);
		assertEquals(ConnectReturnCode.ACCEPTED, client2.connect("testclient15", true, 90));
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

		verifyNoMoreInteractions(listener, listener2);
	}

	@Test
	public void testPublish_Qos0_NoRetain() throws Exception {

		// connect and subscribe a client to get the messages
		MqttClientListener listener2 = mock(MqttClientListener.class);
		client2 = new SynchronousMqttClient(validBrokerUrl, listener2, reconnectionStrategy, 5, 0, 5, 10);
		assertEquals(ConnectReturnCode.ACCEPTED, client2.connect("testclient15", true, 90));
		assertNotNull(client2.subscribe(new Subscription[] { new Subscription("my/topic7", QoS.AT_LEAST_ONCE) }));

		// connect a client and generate the messages
		client = new SynchronousMqttClient(validBrokerUrl, listener, reconnectionStrategy, 5, 0, 5, 10);
		assertEquals(ConnectReturnCode.ACCEPTED, client.connect("testclient16", true, 90));
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
	public void testPublish_DuplicateMessageReceived() throws Exception {

		// connect and subscribe a client to get the message
		MqttClientListener listener2 = mock(MqttClientListener.class);
		client2 = new SynchronousMqttClient(validBrokerUrl, listener2, reconnectionStrategy, 5, 0, 5, 10);
		assertEquals(ConnectReturnCode.ACCEPTED, client2.connect("testclient18", true, 90));
		assertNotNull(client2.subscribe(new Subscription[] { new Subscription("my/topic7", QoS.AT_LEAST_ONCE) }));

		// connect a client and generate the message
		client = new SynchronousMqttClient(validBrokerUrl, listener, reconnectionStrategy, 5, 0, 5, 10);
		assertEquals(ConnectReturnCode.ACCEPTED, client.connect("testclient17", true, 90));
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

		verifyNoMoreInteractions(listener, listener2);
	}

	@Test
	public void testClose() throws Exception {

		fail("not implemented");
	}

	@Test
	public void testConnectionLost_ReconnectSucceeds() throws Exception {

		fail("not implemented");
	}

	@Test
	public void testConnectionLost_AllReconnectsFail() throws Exception {

		fail("not implemented");
	}

	@Test
	public void testDisconnect() throws Exception {

		fail("not implemented");
	}

	@Test
	public void testSendWhileReconnectionInProgress() throws Exception {

		fail("not implemented");
	}
}
