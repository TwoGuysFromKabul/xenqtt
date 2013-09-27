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
import net.sf.xenqtt.client.MqttClientListener;
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

	int counter;
	@Mock MqttClientListener syncListener;
	@Mock MockBrokerHandler mockHandler;
	MockBroker mockBroker;

	// FIXME [jim] - need to add more tests
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
	public void testSubscribeUnsubscribe_MultipleSubscriptionsToSameTopic() throws Exception {

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

	@Test
	public void testSubscribePublish_WildCards_EmptyTopic() throws Exception {

		// connect client
		connect();

		subscribe("", QoS.AT_MOST_ONCE);
		publish("", true);
		publish("a", false);

		disconnect();
	}

	@Test
	public void testSubscribePublish_WildCards_OnlyPlus() throws Exception {

		// connect client
		connect();

		subscribe("+", QoS.AT_MOST_ONCE);
		publish("", true);
		publish("foo", true);
		publish("/foo", false);
		publish("bar/foo", false);

		disconnect();
	}

	@Test
	public void testSubscribePublish_WildCards_SlashPlus() throws Exception {

		connect();

		subscribe("/+", QoS.AT_MOST_ONCE);
		publish("", false);
		publish("foo", false);
		publish("/foo", true);
		publish("bar/foo", false);

		disconnect();
	}

	@Test
	public void testSubscribePublish_WildCards_PlusSlashPlus() throws Exception {

		connect();

		subscribe("+/+", QoS.AT_MOST_ONCE);
		publish("", false);
		publish("foo", false);
		publish("/foo", true);
		publish("bar/foo", true);

		disconnect();
	}

	@Test
	public void testSubscribePublish_WildCards_TrailingPlus() throws Exception {

		connect();

		subscribe("a/b/+", QoS.AT_MOST_ONCE);
		publish("a/b", false);
		publish("a/b/abc", true);
		publish("a/b/abc/d", false);

		disconnect();
	}

	@Test
	public void testSubscribePublish_WildCards_PlusInTheMiddle() throws Exception {

		connect();

		subscribe("a/+/c", QoS.AT_MOST_ONCE);
		publish("a/b", false);
		publish("a/b/c", true);
		publish("a/b/d", false);
		publish("a/b/c/d", false);

		disconnect();
	}

	@Test
	public void testSubscribePublish_WildCards_OnlyPound() throws Exception {

		connect();

		subscribe("#", QoS.AT_MOST_ONCE);
		publish("", true);
		publish("a", true);
		publish("/a", true);
		publish("a/b", true);
		publish("/a/b", true);

		disconnect();
	}

	@Test
	public void testSubscribePublish_WildCards_SlashPound() throws Exception {

		connect();

		subscribe("/#", QoS.AT_MOST_ONCE);
		publish("", true);
		publish("a", true);
		publish("/a", true);
		publish("a/b", true);
		publish("/a/b", true);

		disconnect();
	}

	@Test
	public void testSubscribePublish_WildCards_MoreThanPound_NoLeadingSlash() throws Exception {

		connect();

		subscribe("a/#", QoS.AT_MOST_ONCE);
		publish("a", true);
		publish("/a", false);
		publish("b", false);
		publish("/b", false);
		publish("a/b", true);
		publish("/a/b", false);

		disconnect();
	}

	@Test
	public void testSubscribePublish_WildCards_MoreThanPound_WithLeadingSlash() throws Exception {

		connect();

		subscribe("/a/#", QoS.AT_MOST_ONCE);
		publish("a", false);
		publish("/a", true);
		publish("b", false);
		publish("/b", false);
		publish("a/b", false);
		publish("/a/b", true);

		disconnect();
	}

	@Test
	public void testSubscribePublish_IllegalTopic_PlusInMiddleOfLevelString() throws Exception {

		connect();

		subscribe("a/a+c/c", QoS.AT_MOST_ONCE);
		publish("a/abc/c", false);
		publish("a/a+c/c", false);

		disconnect();
	}

	@Test
	public void testSubscribePublish_IllegalTopic_PlusAtEndOfLevelString() throws Exception {

		connect();

		subscribe("a/a+/c", QoS.AT_MOST_ONCE);
		publish("a/ab/c", false);
		publish("a/a+/c", false);

		disconnect();
	}

	@Test
	public void testSubscribePublish_IllegalTopic_PlusAtStartOfLevelString() throws Exception {

		connect();

		subscribe("a/+c/c", QoS.AT_MOST_ONCE);
		publish("a/bc/c", false);
		publish("a/+c/c", false);

		disconnect();
	}

	@Test
	public void testSubscribePublish_IllegalTopic_PoundAsPartOfLevelString() throws Exception {

		connect();

		subscribe("a/a#c/c", QoS.AT_MOST_ONCE);
		publish("a/abc/c", false);
		publish("a/a#c/c", false);

		disconnect();
	}

	@Test
	public void testSubscribePublish_IllegalTopic_PoundInTheMiddle() throws Exception {

		connect();

		subscribe("a/#/c", QoS.AT_MOST_ONCE);
		publish("a/abc/c", false);

		disconnect();
	}

	@Test
	public void testSubscribePublish_IllegalTopic_PublishTopicContainsWildcards() throws Exception {

		connect();

		subscribe("a/b/c", QoS.AT_MOST_ONCE);
		publish("a/+/c", false);
		publish("a/#", false);

		disconnect();
	}

	@Test
	public void testSubscribePublish_IllegalTopic_TrailingSlash() throws Exception {

		connect();

		subscribe("a/b/", QoS.AT_MOST_ONCE);
		publish("a/b/", false);
		publish("a/b", false);

		disconnect();
	}

	@Test
	public void testSubscribePublish_IllegalTopic_DoubleSlash() throws Exception {

		connect();

		subscribe("a//b", QoS.AT_MOST_ONCE);
		publish("a//b", false);
		publish("a/b", false);

		disconnect();
	}

	/**
	 * Opens a connection and assigns it to {@link AbstractAsyncMqttClientIT#client}. Resets the listener before waiting.
	 */
	private void connect() {

		reset(listener);
		client = new AsyncMqttClient(validBrokerUri, listener, reconnectionStrategy, 5, 0, 5);
		client.connect("testclient99", true, 90);
		verify(listener, timeout(5000)).connected(client, ConnectReturnCode.ACCEPTED);
	}

	/**
	 * Subscribes to the specified topic at the specified qos and waits for the ack. Resets the listener before waiting.
	 */
	private void subscribe(String topic, QoS qos) {
		subscribe(new String[] { topic }, new QoS[] { qos });
	}

	/**
	 * Subscribes to the specified topics at the specified qoses and waits for the ack. Resets the listener before waiting.
	 */
	private void subscribe(String[] topics, QoS[] qoses) {

		assertEquals("topics and qoses must be the same length", topics.length, qoses.length);
		reset(listener);
		Subscription[] subscriptions = new Subscription[topics.length];
		for (int i = 0; i < subscriptions.length; i++) {
			subscriptions[i] = new Subscription(topics[i], qoses[i]);
		}
		client.subscribe(subscriptions);
		verify(listener, timeout(5000)).subscribed(same(client), same(subscriptions), aryEq(subscriptions), eq(true));

	}

	/**
	 * Publishes a message at oqs 0 using the client, waits for the listener to receive it. Resets the listener before waiting.
	 * 
	 * @param shouldBeReceived
	 *            If true then the received message is verified. If false the we wait 1 second and verify that the message is never received.
	 */
	private void publish(String topic, boolean shouldBeReceived) throws Exception {

		String payload = "foo-" + counter++;

		reset(listener);
		client.publish(new PublishMessage(topic, QoS.AT_MOST_ONCE, payload));

		if (shouldBeReceived) {
			verify(listener, timeout(5000)).publishReceived(same(client), messageCaptor.capture());
			PublishMessage message = messageCaptor.getValue();
			assertEquals(topic, message.getTopic());
			assertEquals(QoS.AT_MOST_ONCE, message.getQoS());
			assertEquals(payload, message.getPayloadString());
		} else {
			Thread.sleep(1000);
			verify(listener, never()).publishReceived(same(client), any(PublishMessage.class));
		}
	}

	/**
	 * Disconnects the client and waits for completion. Resets the listener before waiting.
	 */
	private void disconnect() {

		reset(listener);
		client.disconnect();
		verify(listener, timeout(5000)).disconnected(eq(client), isNull(Throwable.class), eq(false));
	}
}
