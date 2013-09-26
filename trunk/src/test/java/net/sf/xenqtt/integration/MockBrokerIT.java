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
import static org.mockito.AdditionalMatchers.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import net.sf.xenqtt.client.AsyncClientListener;
import net.sf.xenqtt.client.AsyncMqttClient;
import net.sf.xenqtt.client.PublishMessage;
import net.sf.xenqtt.client.Subscription;
import net.sf.xenqtt.message.ConnectReturnCode;
import net.sf.xenqtt.message.QoS;
import net.sf.xenqtt.mockbroker.MockBroker;
import net.sf.xenqtt.mockbroker.MockBrokerHandler;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class MockBrokerIT extends AbstractAsyncMqttClientIT {

	@Mock MockBrokerHandler mockHandler;
	MockBroker mockBroker;

	// FIXME [jim] - need to add mock broker specific tests, if any
	@Override
	@Before
	public void before() {

		MockitoAnnotations.initMocks(this);

		mockBroker = new MockBroker(mockHandler, 15, 0, true);
		mockBroker.init();
		validBrokerUri = "tcp://localhost:" + mockBroker.getPort();
		badCredentialsUri = validBrokerUri;

		super.before();
	}

	@Test
	public void testConnect_Credentials_Accepted() throws Exception {

		if (mockBroker == null) {
			mockBroker = new MockBroker(null, 15, 0, true);
			mockBroker.init();
		}

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
	public void testSubscribeUnsubscribe_Wildcards() throws Exception {

		// connect publishing client
		AsyncClientListener listener2 = mock(AsyncClientListener.class);
		client2 = new AsyncMqttClient(validBrokerUri, listener2, reconnectionStrategy, 5, 0, 5);
		client2.connect("testclient21", true, 90);
		verify(listener2, timeout(5000)).connected(client2, ConnectReturnCode.ACCEPTED);

		// connect client
		client = new AsyncMqttClient(validBrokerUri, listener, reconnectionStrategy, 5, 0, 5);
		client.connect("testclient22", true, 90);
		verify(listener, timeout(5000)).connected(client, ConnectReturnCode.ACCEPTED);

		// test subscribing standard
		Subscription[] requestedSubscriptions = new Subscription[] { new Subscription("my/topic1", QoS.AT_LEAST_ONCE) };
		client.subscribe(requestedSubscriptions);
		verify(listener, timeout(5000)).subscribed(same(client), same(requestedSubscriptions), aryEq(requestedSubscriptions), eq(true));

		client2.publish(new PublishMessage("my/topic1", QoS.AT_LEAST_ONCE, "abc"));
		verify(listener, timeout(5000)).publishReceived(same(client), messageCaptor.capture());
		assertEquals(QoS.AT_LEAST_ONCE, messageCaptor.getValue().getQoS());
		messageCaptor.getValue().ack();

		// test subscription to same topic via wildcard
		messageCaptor = ArgumentCaptor.forClass(PublishMessage.class);
		requestedSubscriptions = new Subscription[] { new Subscription("my/+", QoS.AT_MOST_ONCE) };
		client.subscribe(requestedSubscriptions);
		verify(listener, timeout(5000)).subscribed(same(client), same(requestedSubscriptions), aryEq(requestedSubscriptions), eq(true));

		client2.publish(new PublishMessage("my/topic1", QoS.AT_LEAST_ONCE, "def"));
		verify(listener, timeout(5000).times(2)).publishReceived(same(client), messageCaptor.capture());
		assertEquals(QoS.AT_MOST_ONCE, messageCaptor.getValue().getQoS());
		messageCaptor.getValue().ack();

		// test unsubscribing wildcard
		client.unsubscribe(new String[] { "my/+" });
		verify(listener, timeout(5000)).unsubscribed(same(client), aryEq(new String[] { "my/+" }));

		messageCaptor = ArgumentCaptor.forClass(PublishMessage.class);
		client2.publish(new PublishMessage("my/topic1", QoS.AT_LEAST_ONCE, "ghi"));
		verify(listener, timeout(5000).times(3)).publishReceived(same(client), messageCaptor.capture());
		assertEquals(QoS.AT_LEAST_ONCE, messageCaptor.getValue().getQoS());
		messageCaptor.getValue().ack();

		// test unsubscribing standard
		client.unsubscribe(new String[] { "my/topic1" });
		verify(listener, timeout(5000)).unsubscribed(same(client), aryEq(new String[] { "my/topic1" }));

		// disconnect
		client.disconnect();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));

		verifyNoMoreInteractions(listener);
	}
}
