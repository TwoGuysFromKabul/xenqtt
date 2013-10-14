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
package net.sf.xenqtt;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.regex.Pattern;

import net.sf.xenqtt.client.MqttClient;
import net.sf.xenqtt.client.MqttClientListener;
import net.sf.xenqtt.client.Subscription;
import net.sf.xenqtt.client.SyncMqttClient;
import net.sf.xenqtt.message.MessageHandler;
import net.sf.xenqtt.message.MqttChannel;
import net.sf.xenqtt.message.QoS;
import net.sf.xenqtt.message.SubAckMessage;
import net.sf.xenqtt.message.SubscribeMessage;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class SimpleBrokerTest {

	@Mock MessageHandler messageHandler;
	@Mock MqttClientListener clientListener;

	SimpleBroker broker = new SimpleBroker(15, 0);

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_InvalidMessageResendIntervalSeconds() {
		new SimpleBroker(-1, 1234);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_InvalidPort_BelowRange() {
		new SimpleBroker(1, -1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_InvalidPort_AboveRange() {
		new SimpleBroker(1, 65536);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testShutdown_InvalidMillis() {
		broker.shutdown(-1L);
	}

	@Test
	public void testGetPort() throws Exception {

		assertEquals(24156, new SimpleBroker(15, 24156).getPort());
	}

	@Test
	public void testGetUri() throws Exception {

		String uri = new SimpleBroker(15, 24156).getURI();
		assertTrue(Pattern.matches("tcp://\\d+\\.\\d+\\.\\d+\\.\\d+:24156", uri));
	}

	@Test
	public void testInit_HandlerMessage_AndShutdown() throws Exception {

		doAnswer(new Answer<Void>() {

			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {

				MqttChannel channel = (MqttChannel) invocation.getArguments()[0];
				SubscribeMessage message = (SubscribeMessage) invocation.getArguments()[1];

				assertArrayEquals(new String[] { "foo" }, message.getTopics());
				channel.send(new SubAckMessage(message.getMessageId(), message.getRequestedQoSes()), null, System.currentTimeMillis());
				return null;
			}
		}).when(messageHandler).subscribe(isA(MqttChannel.class), isA(SubscribeMessage.class));

		broker.init(messageHandler, "SimpleBrokerTest");

		MqttClient client = new SyncMqttClient(broker.getURI(), clientListener, 1);

		client.subscribe(new Subscription[] { new Subscription("foo", QoS.AT_LEAST_ONCE) });

		verify(messageHandler).channelOpened(isA(MqttChannel.class));

		assertTrue(broker.shutdown(5000));

		verify(clientListener, timeout(5000)).disconnected(same(client), isNull(Throwable.class), anyBoolean());
	}
}
