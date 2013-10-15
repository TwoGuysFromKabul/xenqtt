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
package net.sf.xenqtt.proxy;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayDeque;
import java.util.Queue;

import net.sf.xenqtt.message.ConnectMessage;
import net.sf.xenqtt.message.MqttChannel;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

public class ProxyBrokerTest {

	String brokerUri = "tcp://localhost:2345";

	TestBroker broker = new TestBroker();
	Queue<ProxySession> sessions = new ArrayDeque<ProxySession>();

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		broker.init();
	}

	@After
	public void after() {
		assertTrue(broker.shutdown(5000));
	}

	@Test
	public void testShutdown_NoSessions() throws Exception {

		broker.shutdown(5000);
	}

	@Test
	public void testShutdown_WithSessions() throws Exception {

		ProxySession session1 = mock(ProxySession.class);
		when(session1.newConnection(any(MqttChannel.class), any(ConnectMessage.class))).thenReturn(true);
		ProxySession session2 = mock(ProxySession.class);
		when(session2.newConnection(any(MqttChannel.class), any(ConnectMessage.class))).thenReturn(true);

		sessions.add(session1);
		sessions.add(session2);

		MqttChannel channel = mock(MqttChannel.class);

		broker.connect(channel, newConnectMessage("foo"));
		broker.connect(channel, newConnectMessage("bar"));

		broker.shutdown(5000);

		verify(session1).shutdown();
		verify(session2).shutdown();
	}

	@Test
	public void testConnect_NewSession() throws Exception {

		ConnectMessage message = newConnectMessage("foo");
		MqttChannel channel = mock(MqttChannel.class);
		ProxySession session = mock(ProxySession.class);
		sessions.add(session);
		when(session.newConnection(channel, message)).thenReturn(true);

		broker.connect(channel, message);

		verify(session).init();
		verify(channel).deregister();
		verify(session).newConnection(channel, message);
		verify(session).isClosed();

		verifyNoMoreInteractions(session, channel);
	}

	@Test
	public void testConnect_ExistingSession_SessionAcceptsClient() throws Exception {

		ConnectMessage message1 = newConnectMessage("foo");
		MqttChannel channel1 = mock(MqttChannel.class);
		ConnectMessage message2 = newConnectMessage("foo");
		MqttChannel channel2 = mock(MqttChannel.class);
		ProxySession session = mock(ProxySession.class);
		sessions.add(session);
		when(session.newConnection(any(MqttChannel.class), any(ConnectMessage.class))).thenReturn(true);

		broker.connect(channel1, message1);
		verify(session).init();
		verify(channel1).deregister();
		verify(session).newConnection(channel1, message1);
		verify(session).isClosed();
		verifyNoMoreInteractions(session, channel1, channel2);

		broker.connect(channel2, message2);

		verify(channel2).deregister();
		verify(session).newConnection(channel2, message2);
		verify(session, times(2)).isClosed();
		verifyNoMoreInteractions(session, channel1, channel2);
	}

	@Test
	public void testConnect_ExistingSession_SessionDoesNotAcceptClient() throws Exception {

		ConnectMessage message1 = newConnectMessage("foo");
		MqttChannel channel1 = mock(MqttChannel.class);
		ConnectMessage message2 = newConnectMessage("foo");
		MqttChannel channel2 = mock(MqttChannel.class);
		ProxySession session1 = mock(ProxySession.class);
		sessions.add(session1);
		when(session1.newConnection(channel1, message1)).thenReturn(true);
		when(session1.newConnection(channel2, message2)).thenReturn(false);
		ProxySession session2 = mock(ProxySession.class);
		sessions.add(session2);
		when(session2.newConnection(channel2, message2)).thenReturn(true);

		broker.connect(channel1, message1);
		verify(session1).init();
		verify(channel1).deregister();
		verify(session1).newConnection(channel1, message1);
		verify(session1).isClosed();
		verifyNoMoreInteractions(session1, session2, channel1, channel2);

		broker.connect(channel2, message2);

		verify(channel2, times(2)).deregister();
		verify(session1).newConnection(channel2, message2);
		verify(session1).isClosed();
		verify(session1).shutdown();

		verify(session2).init();
		verify(session2).newConnection(channel2, message2);
		verify(session2).isClosed();
		verifyNoMoreInteractions(session1, session2, channel1, channel2);

		// this verifies that the session is actually removed from the proxy broker's session list
		broker.connect(channel2, message2);
		verifyNoMoreInteractions(session1);
	}

	@Test
	public void testConnect_HasClosedSessions() throws Exception {

		ConnectMessage message1 = newConnectMessage("foo");
		MqttChannel channel1 = mock(MqttChannel.class);
		ProxySession session1 = mock(ProxySession.class);
		sessions.add(session1);
		when(session1.newConnection(any(MqttChannel.class), any(ConnectMessage.class))).thenReturn(true);
		ConnectMessage message2 = newConnectMessage("bar");
		MqttChannel channel2 = mock(MqttChannel.class);
		ProxySession session2 = mock(ProxySession.class);
		sessions.add(session2);
		when(session2.newConnection(any(MqttChannel.class), any(ConnectMessage.class))).thenReturn(true);

		broker.connect(channel1, message1);
		verify(session1).init();
		verify(channel1).deregister();
		verify(session1).newConnection(channel1, message1);
		verify(session1).isClosed();
		verifyNoMoreInteractions(session1, session2, channel1, channel2);

		when(session1.isClosed()).thenReturn(true);
		broker.connect(channel2, message2);

		verify(session1, times(2)).isClosed();
		verify(session1).shutdown();

		verify(channel2).deregister();
		verify(session2).init();
		verify(session2).newConnection(channel2, message2);
		verify(session2).isClosed();

		verifyNoMoreInteractions(session1, session2, channel1, channel2);

		// this verifies that the session is actually removed from the proxy broker's session list
		broker.connect(channel2, message2);
		verifyNoMoreInteractions(session1);
	}

	private ConnectMessage newConnectMessage(String clientId) {
		return new ConnectMessage(clientId, false, 1000);
	}

	private class TestBroker extends ProxyBroker {

		public TestBroker() {
			super(brokerUri, 0);
		}

		@Override
		ProxySession newProxySession(String brokerUri, ConnectMessage message) {
			return sessions.poll();
		}
	}
}
