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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.sf.xenqtt.AppContext;
import net.sf.xenqtt.MqttCommandCancelledException;
import net.sf.xenqtt.application.ProxyApplication;
import net.sf.xenqtt.client.AsyncClientListener;
import net.sf.xenqtt.client.AsyncMqttClient;
import net.sf.xenqtt.client.MqttClient;
import net.sf.xenqtt.client.PublishMessage;
import net.sf.xenqtt.client.Subscription;
import net.sf.xenqtt.message.ConnectReturnCode;
import net.sf.xenqtt.message.MqttMessage;
import net.sf.xenqtt.message.PubAckMessage;
import net.sf.xenqtt.message.PubMessage;
import net.sf.xenqtt.message.QoS;
import net.sf.xenqtt.message.UnsubscribeMessage;
import net.sf.xenqtt.mockbroker.Client;
import net.sf.xenqtt.mockbroker.MockBroker;
import net.sf.xenqtt.mockbroker.MockBrokerHandler;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ProxyIT {

	@Mock MockBrokerHandler handler;
	@Mock AsyncClientListener listener;
	@Captor ArgumentCaptor<PublishMessage> pubMsgCaptor;
	@Captor ArgumentCaptor<MqttMessage> mqttMsgCaptor;

	MockBroker broker;

	ProxyApplication proxy;

	AsyncMqttClient client1;
	AsyncMqttClient client2;
	AsyncMqttClient client3;

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		broker = new MockBroker(handler, 15, 0, true, true, 50);
		broker.init();

		proxy = new ProxyApplication();
		Map<String, String> args = new HashMap<String, String>();
		args.put("-b", broker.getURI());
		args.put("-p", "0");
		AppContext arguments = new AppContext(Collections.<String> emptyList(), args, null);
		proxy.start(arguments);
	}

	@After
	public void after() {

		if (client1 != null) {
			try {
				client1.close();
			} catch (MqttCommandCancelledException ignore) {
			}
		}
		if (client2 != null) {
			try {
				client2.close();
			} catch (MqttCommandCancelledException ignore) {
			}
		}
		if (client3 != null) {
			try {
				client3.close();
			} catch (MqttCommandCancelledException ignore) {
			}
		}

		broker.shutdown(5000);
	}

	@Test
	public void testConnect_ConnectionAccepted() {

		client1 = new AsyncMqttClient(proxy.getProxyURI(), listener, 1);
		client1.connect("client1", false);
		verify(listener, timeout(5000)).connected(client1, ConnectReturnCode.ACCEPTED);
	}

	@Test
	public void testConnect_ConnectionRejectedByProxy_CleanSessionIsTrue() {

		client1 = new AsyncMqttClient(proxy.getProxyURI(), listener, 1);
		client1.connect("client1", true);
		verify(listener, timeout(5000)).connected(client1, ConnectReturnCode.OTHER);
		verify(listener, timeout(5000)).disconnected(client1, null, false);
		verify(handler, timeout(5000)).channelClosed(any(Client.class), any(Throwable.class));
	}

	@Test
	public void testConnect_ConnectionRejectedByProxy_ConnectMessageDoesNotMatch() {

		client1 = new AsyncMqttClient(proxy.getProxyURI(), listener, 1);
		client1.connect("client1", false);
		verify(listener, timeout(5000)).connected(client1, ConnectReturnCode.ACCEPTED);

		client2 = new AsyncMqttClient(proxy.getProxyURI(), listener, 1);
		client2.connect("client1", false, "topic", "msg", QoS.AT_MOST_ONCE, false);
		verify(listener, timeout(5000)).connected(client2, ConnectReturnCode.OTHER);
		verify(listener, timeout(5000)).disconnected(client2, null, false);
	}

	@Test
	public void testConnect_ConnectionRejectedByBroker() {

		client1 = new AsyncMqttClient(proxy.getProxyURI(), listener, 1);
		client1.connect("client1", false, "foo", "bar");
		verify(listener, timeout(5000)).connected(client1, ConnectReturnCode.BAD_CREDENTIALS);

		verify(handler, timeout(5000)).channelClosed(any(Client.class), any(Throwable.class));
		verify(listener, timeout(5000)).disconnected(client1, null, false);
	}

	@Test
	public void testConnect_BrokerConnectionReopenedOnNewClient() {

		client1 = new AsyncMqttClient(proxy.getProxyURI(), listener, 1);
		client1.connect("client1", false, "foo", "bar");
		verify(listener, timeout(5000)).connected(client1, ConnectReturnCode.BAD_CREDENTIALS);

		verify(handler, timeout(5000)).channelClosed(any(Client.class), any(Throwable.class));
		verify(listener, timeout(5000)).disconnected(client1, null, false);

		client2 = new AsyncMqttClient(proxy.getProxyURI(), listener, 1);
		client2.connect("client2", false);
		verify(listener, timeout(5000)).connected(client2, ConnectReturnCode.ACCEPTED);
	}

	@Test
	public void testDisconnect_LastClient() {

		client1 = new AsyncMqttClient(proxy.getProxyURI(), listener, 1);
		client1.connect("client1", false);
		verify(listener, timeout(5000)).connected(client1, ConnectReturnCode.ACCEPTED);

		client1.disconnect();

		verify(listener, timeout(5000)).disconnected(client1, null, false);
		verify(handler, timeout(5000)).channelClosed(any(Client.class), any(Throwable.class));
	}

	@Test
	public void testDisconnect_NotLastClient() throws Exception {

		client1 = new AsyncMqttClient(proxy.getProxyURI(), listener, 1);
		client1.connect("client1", false);
		client2 = new AsyncMqttClient(proxy.getProxyURI(), listener, 1);
		client2.connect("client2", false);
		verify(listener, timeout(5000)).connected(client1, ConnectReturnCode.ACCEPTED);
		verify(listener, timeout(5000)).connected(client2, ConnectReturnCode.ACCEPTED);

		client1.disconnect();

		verify(listener, timeout(5000)).disconnected(client1, null, false);

		client2.unsubscribe(new String[] { "foo" });
		verify(handler, timeout(5000)).unsubscribe(any(Client.class), any(UnsubscribeMessage.class));
	}

	@Test
	public void testSingleSubscriber() throws Exception {

		client1 = new AsyncMqttClient(proxy.getProxyURI(), listener, 1);
		client1.connect("client1", false);
		verify(listener, timeout(5000)).connected(client1, ConnectReturnCode.ACCEPTED);

		client1.subscribe(new Subscription[] { new Subscription("topic1", QoS.AT_LEAST_ONCE) });
		verify(listener, timeout(5000)).subscribed(same(client1), any(Subscription[].class), any(Subscription[].class), eq(true));

		client2 = new AsyncMqttClient(broker.getURI(), listener, 1);
		client2.connect("client2", false);
		verify(listener, timeout(5000)).connected(client2, ConnectReturnCode.ACCEPTED);

		for (int i = 0; i < 10; i++) {
			reset(listener, handler);

			PublishMessage sentMsg = new PublishMessage("topic1", QoS.AT_LEAST_ONCE, new byte[] { 1, 2, 3 });
			client2.publish(sentMsg);

			verify(handler, timeout(5000)).publish(any(Client.class), (PubMessage) mqttMsgCaptor.capture());
			PubMessage pubMessageAtBroker = (PubMessage) mqttMsgCaptor.getValue();

			verify(listener, timeout(5000)).publishReceived(same(client1), pubMsgCaptor.capture());

			PublishMessage rcvdMsg = pubMsgCaptor.getValue();
			rcvdMsg.ack();

			verify(handler, timeout(5000)).pubAck(any(Client.class), (PubAckMessage) mqttMsgCaptor.capture());
			PubAckMessage pubAckAtBroker = (PubAckMessage) mqttMsgCaptor.getValue();

			assertEquals(pubMessageAtBroker.getMessageId(), pubAckAtBroker.getMessageId());

			verify(listener).published(same(client2), same(sentMsg));
		}
	}

	@Test
	public void testMultipleSubscribers_SingleCluster() throws Exception {

		client1 = new AsyncMqttClient(proxy.getProxyURI(), listener, 1);
		client1.connect("client1", false);
		verify(listener, timeout(5000)).connected(client1, ConnectReturnCode.ACCEPTED);

		client1.subscribe(new Subscription[] { new Subscription("topic1", QoS.AT_LEAST_ONCE) });
		verify(listener, timeout(5000)).subscribed(same(client1), any(Subscription[].class), any(Subscription[].class), eq(true));

		client2 = new AsyncMqttClient(proxy.getProxyURI(), listener, 1);
		client2.connect("client1", false);
		verify(listener, timeout(5000)).connected(client2, ConnectReturnCode.ACCEPTED);

		client2.subscribe(new Subscription[] { new Subscription("topic1", QoS.AT_LEAST_ONCE) });
		verify(listener, timeout(5000)).subscribed(same(client2), any(Subscription[].class), any(Subscription[].class), eq(true));

		client3 = new AsyncMqttClient(broker.getURI(), listener, 1);
		client3.connect("client3", false);
		verify(listener, timeout(5000)).connected(client3, ConnectReturnCode.ACCEPTED);

		for (int i = 0; i < 10; i++) {
			reset(listener, handler);

			PublishMessage sentMsg = new PublishMessage("topic1", QoS.AT_LEAST_ONCE, new byte[] { 1, 2, 3 });
			client3.publish(sentMsg);

			verify(handler, timeout(5000)).publish(any(Client.class), (PubMessage) mqttMsgCaptor.capture());
			PubMessage pubMessageAtBroker = (PubMessage) mqttMsgCaptor.getValue();

			MqttClient client = i % 2 == 0 ? client1 : client2;
			verify(listener, timeout(5000)).publishReceived(same(client), pubMsgCaptor.capture());

			PublishMessage rcvdMsg = pubMsgCaptor.getValue();
			rcvdMsg.ack();

			verify(handler, timeout(5000)).pubAck(any(Client.class), (PubAckMessage) mqttMsgCaptor.capture());
			PubAckMessage pubAckAtBroker = (PubAckMessage) mqttMsgCaptor.getValue();

			assertEquals(pubMessageAtBroker.getMessageId(), pubAckAtBroker.getMessageId());

			verify(listener).published(same(client3), same(sentMsg));
		}
	}

	@Test
	public void testMultipleClusters() throws Exception {

		client1 = new AsyncMqttClient(proxy.getProxyURI(), listener, 1);
		client1.connect("client1", false);
		verify(listener, timeout(5000)).connected(client1, ConnectReturnCode.ACCEPTED);

		client1.subscribe(new Subscription[] { new Subscription("topic1", QoS.AT_LEAST_ONCE) });
		verify(listener, timeout(5000)).subscribed(same(client1), any(Subscription[].class), any(Subscription[].class), eq(true));

		client2 = new AsyncMqttClient(proxy.getProxyURI(), listener, 1);
		client2.connect("client2", false);
		verify(listener, timeout(5000)).connected(client2, ConnectReturnCode.ACCEPTED);

		client2.subscribe(new Subscription[] { new Subscription("topic1", QoS.AT_LEAST_ONCE) });
		verify(listener, timeout(5000)).subscribed(same(client2), any(Subscription[].class), any(Subscription[].class), eq(true));

		client3 = new AsyncMqttClient(broker.getURI(), listener, 1);
		client3.connect("client3", false);
		verify(listener, timeout(5000)).connected(client3, ConnectReturnCode.ACCEPTED);

		for (int i = 0; i < 10; i++) {
			reset(listener, handler);

			PublishMessage sentMsg = new PublishMessage("topic1", QoS.AT_LEAST_ONCE, new byte[] { 1, 2, 3 });
			client3.publish(sentMsg);

			verify(handler, timeout(5000)).publish(any(Client.class), (PubMessage) mqttMsgCaptor.capture());
			PubMessage pubMessageAtBroker = (PubMessage) mqttMsgCaptor.getValue();

			verify(listener, timeout(5000)).publishReceived(same(client1), pubMsgCaptor.capture());
			verify(listener, timeout(5000)).publishReceived(same(client2), pubMsgCaptor.capture());

			PublishMessage rcvdMsg = pubMsgCaptor.getValue();
			rcvdMsg.ack();

			verify(handler, timeout(5000)).pubAck(any(Client.class), (PubAckMessage) mqttMsgCaptor.capture());
			PubAckMessage pubAckAtBroker = (PubAckMessage) mqttMsgCaptor.getValue();

			assertEquals(pubMessageAtBroker.getMessageId(), pubAckAtBroker.getMessageId());

			verify(listener).published(same(client3), same(sentMsg));
		}
	}

	@Test
	public void testSinglePublisher() throws Exception {

		client1 = new AsyncMqttClient(proxy.getProxyURI(), listener, 1);
		client1.connect("client1", false);
		verify(listener, timeout(5000)).connected(client1, ConnectReturnCode.ACCEPTED);

		client2 = new AsyncMqttClient(broker.getURI(), listener, 1);
		client2.connect("client2", false);
		verify(listener, timeout(5000)).connected(client2, ConnectReturnCode.ACCEPTED);

		client2.subscribe(new Subscription[] { new Subscription("topic1", QoS.AT_LEAST_ONCE) });
		verify(listener, timeout(5000)).subscribed(same(client2), any(Subscription[].class), any(Subscription[].class), eq(true));

		for (int i = 0; i < 10; i++) {
			reset(listener, handler);

			PublishMessage sentMsg = new PublishMessage("topic1", QoS.AT_LEAST_ONCE, new byte[] { 1, 2, 3 });
			client1.publish(sentMsg);

			verify(handler, timeout(5000)).publish(any(Client.class), (PubMessage) mqttMsgCaptor.capture());
			PubMessage pubMessageAtBroker = (PubMessage) mqttMsgCaptor.getValue();

			verify(listener, timeout(5000)).publishReceived(same(client2), pubMsgCaptor.capture());

			PublishMessage rcvdMsg = pubMsgCaptor.getValue();
			rcvdMsg.ack();

			verify(handler, timeout(5000)).pubAck(any(Client.class), (PubAckMessage) mqttMsgCaptor.capture());
			PubAckMessage pubAckAtBroker = (PubAckMessage) mqttMsgCaptor.getValue();

			assertEquals(pubMessageAtBroker.getMessageId(), pubAckAtBroker.getMessageId());

			verify(listener).published(same(client1), same(sentMsg));
		}
	}

	@Test
	public void testMultiplePublishers_SingleCluster() throws Exception {

		client1 = new AsyncMqttClient(proxy.getProxyURI(), listener, 1);
		client1.connect("client1", false);
		verify(listener, timeout(5000)).connected(client1, ConnectReturnCode.ACCEPTED);

		client2 = new AsyncMqttClient(proxy.getProxyURI(), listener, 1);
		client2.connect("client1", false);
		verify(listener, timeout(5000)).connected(client2, ConnectReturnCode.ACCEPTED);

		client3 = new AsyncMqttClient(broker.getURI(), listener, 1);
		client3.connect("client3", false);
		verify(listener, timeout(5000)).connected(client3, ConnectReturnCode.ACCEPTED);

		client3.subscribe(new Subscription[] { new Subscription("topic1", QoS.AT_LEAST_ONCE) });
		verify(listener, timeout(5000)).subscribed(same(client3), any(Subscription[].class), any(Subscription[].class), eq(true));

		for (int i = 0; i < 10; i++) {
			reset(listener, handler);

			PublishMessage sentMsg = new PublishMessage("topic1", QoS.AT_LEAST_ONCE, new byte[] { 1, 2, 3 });
			MqttClient client = i % 2 == 0 ? client1 : client2;
			client.publish(sentMsg);

			verify(handler, timeout(5000)).publish(any(Client.class), (PubMessage) mqttMsgCaptor.capture());
			PubMessage pubMessageAtBroker = (PubMessage) mqttMsgCaptor.getValue();

			verify(listener, timeout(5000)).publishReceived(same(client3), pubMsgCaptor.capture());

			PublishMessage rcvdMsg = pubMsgCaptor.getValue();
			rcvdMsg.ack();

			verify(handler, timeout(5000)).pubAck(any(Client.class), (PubAckMessage) mqttMsgCaptor.capture());
			PubAckMessage pubAckAtBroker = (PubAckMessage) mqttMsgCaptor.getValue();

			assertEquals(pubMessageAtBroker.getMessageId(), pubAckAtBroker.getMessageId());

			verify(listener).published(same(client), same(sentMsg));
		}
	}
}
