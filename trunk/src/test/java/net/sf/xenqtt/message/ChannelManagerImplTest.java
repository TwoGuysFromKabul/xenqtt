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
package net.sf.xenqtt.message;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.net.ConnectException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import net.sf.xenqtt.MqttCommandCancelledException;
import net.sf.xenqtt.MqttException;
import net.sf.xenqtt.mock.MockMessageHandler;
import net.sf.xenqtt.mock.MockServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class ChannelManagerImplTest {

	MqttChannelRef clientChannel;
	MqttChannelRef brokerChannel;

	MockServer server = new MockServer();
	MockMessageHandler clientHandler = new MockMessageHandler();
	MockMessageHandler brokerHandler = new MockMessageHandler();

	ChannelManagerImpl manager;

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
	}

	@After
	public void after() {

		manager.shutdown();
		server.close();
	}

	@Test
	public void testInit_IsRunning_Shutdown_NonBLocking() throws Exception {

		manager = new ChannelManagerImpl(2);
		manager.init();
		manager.shutdown();

		manager = new ChannelManagerImpl(10);
		assertFalse(manager.isRunning());
		manager.init();
		assertTrue(manager.isRunning());
		manager.shutdown();
		assertFalse(manager.isRunning());
	}

	@Test
	public void testInit_IsRunning_Shutdown_Blocking() throws Exception {

		manager = new ChannelManagerImpl(2, 0);
		manager.init();
		manager.shutdown();

		manager = new ChannelManagerImpl(10, 0);
		assertFalse(manager.isRunning());
		manager.init();
		assertTrue(manager.isRunning());
		manager.shutdown();
		assertFalse(manager.isRunning());
	}

	@Test
	public void testShutdownClosesAll_NonBlocking() throws Exception {

		manager = new ChannelManagerImpl(2);
		manager.init();

		clientChannel = manager.newClientChannel("localhost", server.getPort(), clientHandler);
		brokerChannel = manager.newBrokerChannel(server.nextClient(1000), brokerHandler);

		clientHandler.assertChannelClosedCount(0);
		brokerHandler.assertChannelClosedCount(0);

		manager.shutdown();

		clientHandler.assertChannelClosedCount(1);
		brokerHandler.assertChannelClosedCount(1);
	}

	@Test
	public void testShutdownClosesAll_Blocking() throws Exception {

		manager = new ChannelManagerImpl(2, 0);
		manager.init();

		clientChannel = manager.newClientChannel("localhost", server.getPort(), clientHandler);
		brokerChannel = manager.newBrokerChannel(server.nextClient(1000), brokerHandler);

		clientHandler.assertChannelClosedCount(0);
		brokerHandler.assertChannelClosedCount(0);

		manager.shutdown();

		clientHandler.assertChannelClosedCount(1);
		brokerHandler.assertChannelClosedCount(1);
	}

	@Test
	public void testCancelBlockingCommands_Blocking() throws Exception {
		manager = new ChannelManagerImpl(2, 0);
		manager.init();

		MqttChannel channel = mock(MqttChannel.class);
		manager.cancelBlockingCommands(channel);

		manager.shutdown();

		verify(channel).cancelBlockingCommands();
	}

	@Test
	public void testCancelBlockingCommands_NonBlocking() throws Exception {
		manager = new ChannelManagerImpl(2);
		manager.init();

		MqttChannel channel = mock(MqttChannel.class);
		manager.cancelBlockingCommands(channel);

		manager.shutdown();

		verify(channel, timeout(1000)).cancelBlockingCommands();
	}

	@Test
	public void testNewClientChannel_InvalidHost_NonBlocking() throws Exception {

		manager = new ChannelManagerImpl(2);
		manager.init();

		try {
			manager.newClientChannel("foo", 123, clientHandler);
			fail("Exception expected");
		} catch (RuntimeException e) {
			clientHandler.assertLastChannelClosedCause(e.getCause());
		}
	}

	@Test
	public void testNewClientChannel_InvalidHost_Blocking() throws Exception {

		manager = new ChannelManagerImpl(2, 0);
		manager.init();

		try {
			manager.newClientChannel("foo", 123, clientHandler);
			fail("Exception expected");
		} catch (RuntimeException e) {
			clientHandler.assertLastChannelClosedCause(e.getCause());
		}
	}

	@Test
	public void testNewClientChannel_UnableToConnect_NonBlocking() throws Exception {

		manager = new ChannelManagerImpl(2);
		manager.init();

		CountDownLatch trigger = new CountDownLatch(1);
		clientHandler.onChannelClosed(trigger);
		clientChannel = manager.newClientChannel("localhost", 19876, clientHandler);

		assertTrue(trigger.await(1000, TimeUnit.SECONDS));

		clientHandler.assertLastChannelClosedCause(ConnectException.class);
	}

	@Test
	public void testNewClientChannel_UnableToConnect_Blocking() throws Exception {

		manager = new ChannelManagerImpl(2, 0);
		manager.init();

		CountDownLatch trigger = new CountDownLatch(1);
		clientHandler.onChannelClosed(trigger);

		try {
			clientChannel = manager.newClientChannel("localhost", 19876, clientHandler);
			fail("expected exception");
		} catch (MqttCommandCancelledException e) {
		}

		assertTrue(trigger.await(1000, TimeUnit.SECONDS));

		clientHandler.assertLastChannelClosedCause(ConnectException.class);
	}

	@Test
	public void testNewClientChannel_HostPort_NonBlocking() throws Exception {

		manager = new ChannelManagerImpl(2);
		manager.init();

		CountDownLatch trigger = new CountDownLatch(1);
		clientHandler.onChannelOpened(trigger);

		clientChannel = manager.newClientChannel("localhost", server.getPort(), clientHandler);

		assertTrue(trigger.await(1000, TimeUnit.SECONDS));

		clientHandler.assertChannelClosedCount(0);
	}

	@Test
	public void testNewClientChannel_HostPort_Blocking() throws Exception {

		manager = new ChannelManagerImpl(2, 0);
		manager.init();

		CountDownLatch trigger = new CountDownLatch(1);
		clientHandler.onChannelOpened(trigger);

		clientChannel = manager.newClientChannel("localhost", server.getPort(), clientHandler);

		assertTrue(trigger.await(1000, TimeUnit.SECONDS));

		clientHandler.assertChannelClosedCount(0);
	}

	@Test
	public void testNewClientChannel_NonTcpUri_NonBlocking() throws Exception {

		manager = new ChannelManagerImpl(2);
		manager.init();

		CountDownLatch trigger = new CountDownLatch(1);
		clientHandler.onChannelOpened(trigger);

		try {
			clientChannel = manager.newClientChannel("http://localhost:3456", clientHandler);
			fail("expected exception");
		} catch (MqttException e) {
			assertEquals("Invalid broker URI (scheme must be 'tcp'): http://localhost:3456", e.getMessage());
		}

		clientHandler.assertChannelOpenedCount(0);
	}

	@Test
	public void testNewClientChannel_NonTcpUri_Blocking() throws Exception {

		manager = new ChannelManagerImpl(2, 0);
		manager.init();

		CountDownLatch trigger = new CountDownLatch(1);
		clientHandler.onChannelOpened(trigger);

		try {
			clientChannel = manager.newClientChannel("http://localhost:3456", clientHandler);
			fail("expected exception");
		} catch (MqttException e) {
			assertEquals("Invalid broker URI (scheme must be 'tcp'): http://localhost:3456", e.getMessage());
		}

		clientHandler.assertChannelOpenedCount(0);
	}

	@Test
	public void testNewClientChannel_UriAsString_NonBlocking() throws Exception {

		manager = new ChannelManagerImpl(2);
		manager.init();

		CountDownLatch trigger = new CountDownLatch(1);
		clientHandler.onChannelOpened(trigger);

		clientChannel = manager.newClientChannel("tcp://localhost:" + server.getPort(), clientHandler);

		assertTrue(trigger.await(1000, TimeUnit.SECONDS));

		clientHandler.assertChannelClosedCount(0);
	}

	@Test
	public void testNewClientChannel_UriAsString_Blocking() throws Exception {

		manager = new ChannelManagerImpl(2, 0);
		manager.init();

		CountDownLatch trigger = new CountDownLatch(1);
		clientHandler.onChannelOpened(trigger);

		clientChannel = manager.newClientChannel("tcp://localhost:" + server.getPort(), clientHandler);

		assertTrue(trigger.await(1000, TimeUnit.SECONDS));

		clientHandler.assertChannelClosedCount(0);
	}

	@Test
	public void testNewClientChannel_Uri_NonBlocking() throws Exception {

		manager = new ChannelManagerImpl(2);
		manager.init();

		CountDownLatch trigger = new CountDownLatch(1);
		clientHandler.onChannelOpened(trigger);

		clientChannel = manager.newClientChannel(new URI("tcp://localhost:" + server.getPort()), clientHandler);

		assertTrue(trigger.await(1000, TimeUnit.SECONDS));

		clientHandler.assertChannelClosedCount(0);
	}

	@Test
	public void testNewClientChannel_Uri_Blocking() throws Exception {

		manager = new ChannelManagerImpl(2, 0);
		manager.init();

		CountDownLatch trigger = new CountDownLatch(1);
		clientHandler.onChannelOpened(trigger);

		clientChannel = manager.newClientChannel(new URI("tcp://localhost:" + server.getPort()), clientHandler);

		assertTrue(trigger.await(1000, TimeUnit.SECONDS));

		clientHandler.assertChannelClosedCount(0);
	}

	@Test
	public void testNewBrokerChannel_NonBlocking() throws Exception {

		manager = new ChannelManagerImpl(2);
		manager.init();

		clientChannel = manager.newClientChannel("localhost", server.getPort(), clientHandler);
		brokerChannel = manager.newBrokerChannel(server.nextClient(1000), brokerHandler);

		clientHandler.assertChannelOpenedCount(1);
		brokerHandler.assertChannelOpenedCount(1);
		clientHandler.assertChannelClosedCount(0);
		brokerHandler.assertChannelClosedCount(0);
	}

	@Test
	public void testNewBrokerChannel_Blocking() throws Exception {

		manager = new ChannelManagerImpl(2, 0);
		manager.init();

		clientChannel = manager.newClientChannel("localhost", server.getPort(), clientHandler);
		brokerChannel = manager.newBrokerChannel(server.nextClient(1000), brokerHandler);

		clientHandler.assertChannelOpenedCount(1);
		brokerHandler.assertChannelOpenedCount(1);
		clientHandler.assertChannelClosedCount(0);
		brokerHandler.assertChannelClosedCount(0);
	}

	@Test
	public void testSend_NonBlocking() throws Exception {

		manager = new ChannelManagerImpl(2);
		manager.init();

		CountDownLatch trigger = new CountDownLatch(1);
		brokerHandler.onMessage(MessageType.PUBACK, trigger);

		clientChannel = manager.newClientChannel("localhost", server.getPort(), clientHandler);
		brokerChannel = manager.newBrokerChannel(server.nextClient(1000), brokerHandler);

		assertNull(manager.send(clientChannel, new PubAckMessage(1)));

		assertTrue(trigger.await(1, TimeUnit.SECONDS));

		brokerHandler.assertMessages(new PubAckMessage(1));
	}

	@Test
	public void testSend_Blocking_NonAckableMessage() throws Exception {

		manager = new ChannelManagerImpl(2, 0);
		manager.init();

		CountDownLatch trigger = new CountDownLatch(1);
		brokerHandler.onMessage(MessageType.PUBACK, trigger);

		clientChannel = manager.newClientChannel("localhost", server.getPort(), clientHandler);
		brokerChannel = manager.newBrokerChannel(server.nextClient(1000), brokerHandler);

		assertNull(manager.send(clientChannel, new PubAckMessage(1)));

		assertTrue(trigger.await(1, TimeUnit.SECONDS));

		brokerHandler.assertMessages(new PubAckMessage(1));
	}

	@Test
	public void testSend_Blocking_AckableMessage() throws Exception {

		manager = new ChannelManagerImpl(2, 0);
		manager.init();

		brokerHandler = mock(MockMessageHandler.class);
		doAnswer(new Answer<Void>() {

			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {

				MqttChannel channel = (MqttChannel) invocation.getArguments()[0];
				SubscribeMessage msg = (SubscribeMessage) invocation.getArguments()[1];
				channel.send(new SubAckMessage(msg.getMessageId(), msg.getRequestedQoSes()), null);
				return null;
			}
		}).when(brokerHandler).subscribe(isA(MqttChannel.class), isA(SubscribeMessage.class));

		clientChannel = manager.newClientChannel("localhost", server.getPort(), clientHandler);
		brokerChannel = manager.newBrokerChannel(server.nextClient(1000), brokerHandler);

		SubAckMessage message = manager.send(clientChannel, new SubscribeMessage(1, new String[] { "foo" }, new QoS[] { QoS.AT_LEAST_ONCE }));
		assertEquals(1, message.getMessageId());
		assertArrayEquals(new QoS[] { QoS.AT_LEAST_ONCE }, message.getGrantedQoses());
	}

	@Test
	public void testGetUnsentMessages_Blocking() throws Exception {

		manager = new ChannelManagerImpl(2, 0);
		manager.init();

		MqttChannel channel = mock(MqttChannel.class);
		List<MqttMessage> unsentMessages = new ArrayList<MqttMessage>();
		unsentMessages.add(new PubAckMessage(1));
		unsentMessages.add(new PubAckMessage(2));
		unsentMessages.add(new PubAckMessage(3));
		when(channel.getUnsentMessages()).thenReturn(unsentMessages);

		List<MqttMessage> messages = manager.getUnsentMessages(channel);
		assertEquals(unsentMessages.size(), messages.size());
		for (MqttMessage message : messages) {
			assertTrue(unsentMessages.contains(message));
		}
	}

	@Test
	public void testGetUnsentMessages_NonBlocking() throws Exception {

		manager = new ChannelManagerImpl(2);
		manager.init();

		MqttChannel channel = mock(MqttChannel.class);
		List<MqttMessage> unsentMessages = new ArrayList<MqttMessage>();
		unsentMessages.add(new PubAckMessage(1));
		unsentMessages.add(new PubAckMessage(2));
		unsentMessages.add(new PubAckMessage(3));
		when(channel.getUnsentMessages()).thenReturn(unsentMessages);

		List<MqttMessage> messages = manager.getUnsentMessages(channel);
		assertEquals(unsentMessages.size(), messages.size());
		for (MqttMessage message : messages) {
			assertTrue(unsentMessages.contains(message));
		}
	}

	@Test
	public void testTransfer() throws Exception {

		manager = new ChannelManagerImpl(2000);
		manager.init();

		// create first client and broker and send a message
		CountDownLatch trigger = new CountDownLatch(1);
		brokerHandler.onMessage(MessageType.UNSUBSCRIBE, trigger);

		clientChannel = manager.newClientChannel("localhost", server.getPort(), clientHandler);
		brokerChannel = manager.newBrokerChannel(server.nextClient(1000), brokerHandler);

		UnsubscribeMessage message = new UnsubscribeMessage(1, new String[] { "foo" });
		assertNull(manager.send(clientChannel, message));
		assertTrue(trigger.await(10, TimeUnit.SECONDS));
		brokerHandler.assertMessages(message);

		// create the second client and broker
		MockMessageHandler brokerHandler2 = new MockMessageHandler();
		trigger = new CountDownLatch(1);
		brokerHandler2.onMessage(MessageType.UNSUBSCRIBE, trigger);
		MockMessageHandler clientHandler2 = new MockMessageHandler();
		MqttChannelRef clientChannel2 = manager.newClientChannel("localhost", server.getPort(), clientHandler2);
		manager.newBrokerChannel(server.nextClient(1000), brokerHandler2);

		// verify the message gets resent on the new channel
		manager.transfer(clientChannel, clientChannel2);
		assertTrue(trigger.await(1000, TimeUnit.SECONDS));
		brokerHandler2.assertMessages(message);

		// new messages sent from the old channel should go through the new channel
		trigger = new CountDownLatch(1);
		brokerHandler2.onMessage(MessageType.UNSUBSCRIBE, trigger);
		assertNull(manager.send(clientChannel, message));
		assertTrue(trigger.await(1, TimeUnit.SECONDS));
		brokerHandler2.assertMessages(message, message);

		// new messages sent from the new channel should go through the new channel
		trigger = new CountDownLatch(1);
		brokerHandler2.onMessage(MessageType.UNSUBSCRIBE, trigger);
		assertNull(manager.send(clientChannel2, message));
		assertTrue(trigger.await(1, TimeUnit.SECONDS));
		brokerHandler2.assertMessages(message, message, message);
	}

	@Test
	public void testClose_NoCause_NonBlocking() throws Exception {

		manager = new ChannelManagerImpl(2);
		manager.init();

		clientChannel = manager.newClientChannel("localhost", server.getPort(), clientHandler);
		brokerChannel = manager.newBrokerChannel(server.nextClient(1000), brokerHandler);

		clientHandler.assertChannelClosedCount(0);
		brokerHandler.assertChannelClosedCount(0);

		manager.close(clientChannel);
		clientHandler.assertChannelClosedCount(1);
		clientHandler.assertLastChannelClosedCause((Throwable) null);
		brokerHandler.assertChannelClosedCount(0);

		manager.close(brokerChannel);
		clientHandler.assertChannelClosedCount(1);
		brokerHandler.assertChannelClosedCount(1);
		brokerHandler.assertLastChannelClosedCause((Throwable) null);
	}

	@Test
	public void testClose_NoCause_Blocking() throws Exception {

		manager = new ChannelManagerImpl(2, 0);
		manager.init();

		clientChannel = manager.newClientChannel("localhost", server.getPort(), clientHandler);
		brokerChannel = manager.newBrokerChannel(server.nextClient(1000), brokerHandler);

		clientHandler.assertChannelClosedCount(0);
		brokerHandler.assertChannelClosedCount(0);

		manager.close(clientChannel);
		clientHandler.assertChannelClosedCount(1);
		clientHandler.assertLastChannelClosedCause((Throwable) null);
		brokerHandler.assertChannelClosedCount(0);

		manager.close(brokerChannel);
		clientHandler.assertChannelClosedCount(1);
		brokerHandler.assertChannelClosedCount(1);
		brokerHandler.assertLastChannelClosedCause((Throwable) null);
	}

	@Test
	public void testClose_WithCause_NonBlocking() throws Exception {

		manager = new ChannelManagerImpl(2);
		manager.init();

		clientChannel = manager.newClientChannel("localhost", server.getPort(), clientHandler);
		brokerChannel = manager.newBrokerChannel(server.nextClient(1000), brokerHandler);

		clientHandler.assertChannelClosedCount(0);
		brokerHandler.assertChannelClosedCount(0);

		Exception cause = new Exception();

		manager.close(clientChannel, cause);
		clientHandler.assertChannelClosedCount(1);
		clientHandler.assertLastChannelClosedCause(cause);
		brokerHandler.assertChannelClosedCount(0);

		manager.close(brokerChannel);
		clientHandler.assertChannelClosedCount(1);
		brokerHandler.assertChannelClosedCount(1);
	}

	@Test
	public void testClose_WithCause_Blocking() throws Exception {

		manager = new ChannelManagerImpl(2, 0);
		manager.init();

		clientChannel = manager.newClientChannel("localhost", server.getPort(), clientHandler);
		brokerChannel = manager.newBrokerChannel(server.nextClient(1000), brokerHandler);

		clientHandler.assertChannelClosedCount(0);
		brokerHandler.assertChannelClosedCount(0);

		Exception cause = new Exception();

		manager.close(clientChannel, cause);
		clientHandler.assertChannelClosedCount(1);
		clientHandler.assertLastChannelClosedCause(cause);
		brokerHandler.assertChannelClosedCount(0);

		manager.close(brokerChannel);
		clientHandler.assertChannelClosedCount(1);
		brokerHandler.assertChannelClosedCount(1);
	}

	@Test
	public void testKeepAlive_NonBlocking() throws Exception {

		manager = new ChannelManagerImpl(2);
		doTestKeepAlive();
	}

	@Test
	public void testKeepAlive_Blocking() throws Exception {

		manager = new ChannelManagerImpl(2, 0);
		doTestKeepAlive();
	}

	@Test
	public void testMessageResend_NonBlocking() throws Exception {
		manager = new ChannelManagerImpl(2);
		doTestMessageResend();
	}

	@Test
	public void testMessageResend_Blocking() throws Exception {
		manager = new ChannelManagerImpl(2, 0);
		doTestMessageResend();
	}

	private void doTestKeepAlive() throws Exception {

		manager.init();

		clientHandler = new MockMessageHandler() {
			@Override
			public void channelOpened(MqttChannel channel) {
				super.channelOpened(channel);
				channel.send(new ConnectMessage("abc", false, 1), null);
			}
		};

		brokerHandler = new MockMessageHandler() {
			@Override
			public void connect(MqttChannel channel, ConnectMessage message) throws Exception {
				super.connect(channel, message);
				channel.send(new ConnAckMessage(ConnectReturnCode.ACCEPTED), null);
			}
		};

		CountDownLatch ackTrigger = new CountDownLatch(1);
		clientHandler.onMessage(MessageType.CONNACK, ackTrigger);

		CountDownLatch closedTrigger = new CountDownLatch(2);
		clientHandler.onChannelClosed(closedTrigger);
		brokerHandler.onChannelClosed(closedTrigger);

		clientChannel = manager.newClientChannel("localhost", server.getPort(), clientHandler);
		brokerChannel = manager.newBrokerChannel(server.nextClient(1000), brokerHandler);

		assertTrue(ackTrigger.await(1, TimeUnit.SECONDS));

		// give time for pings, it would be nice to track the pings
		Thread.sleep(2000);

		clientHandler.assertChannelClosedCount(0);
		brokerHandler.assertChannelClosedCount(0);

		manager.send(clientChannel, new DisconnectMessage());

		assertTrue(closedTrigger.await(1, TimeUnit.SECONDS));

		brokerHandler.assertMessages(new ConnectMessage("abc", false, 1), new DisconnectMessage());
		clientHandler.assertMessages(new ConnAckMessage(ConnectReturnCode.ACCEPTED));

		clientHandler.assertChannelClosedCount(1);
		brokerHandler.assertChannelClosedCount(1);
	}

	private void doTestMessageResend() throws Exception {

		manager.init();

		clientHandler = new MockMessageHandler() {
			@Override
			public void channelOpened(MqttChannel channel) {
				super.channelOpened(channel);
				channel.send(new ConnectMessage("abc", false, 1), null);
			}
		};

		brokerHandler = new MockMessageHandler() {

			int publishCount;

			@Override
			public void connect(MqttChannel channel, ConnectMessage message) throws Exception {
				super.connect(channel, message);
				channel.send(new ConnAckMessage(ConnectReturnCode.ACCEPTED), null);
			}

			@Override
			public void publish(MqttChannel channel, PubMessage message) throws Exception {

				if (++publishCount == 3) {
					channel.send(new PubAckMessage(message.getMessageId()), null);
				}

				super.publish(channel, message);
			}
		};

		CountDownLatch ackTrigger = new CountDownLatch(1);
		clientHandler.onMessage(MessageType.CONNACK, ackTrigger);

		CountDownLatch closedTrigger = new CountDownLatch(2);
		clientHandler.onChannelClosed(closedTrigger);
		brokerHandler.onChannelClosed(closedTrigger);

		clientChannel = manager.newClientChannel("localhost", server.getPort(), clientHandler);
		brokerChannel = manager.newBrokerChannel(server.nextClient(1000), brokerHandler);

		assertTrue(ackTrigger.await(1, TimeUnit.SECONDS));

		manager.send(clientChannel, new PubMessage(QoS.AT_LEAST_ONCE, false, "foo", 123, new byte[] { 1, 2, 3 }));
		// give time for 3 resends but there should only be 2 since we ack the second resend
		Thread.sleep(3500);

		clientHandler.assertChannelClosedCount(0);
		brokerHandler.assertChannelClosedCount(0);

		manager.send(clientChannel, new DisconnectMessage());

		assertTrue(closedTrigger.await(1, TimeUnit.SECONDS));

		PubMessage dupMsg = new PubMessage(QoS.AT_LEAST_ONCE, false, "foo", 123, new byte[] { 1, 2, 3 });
		dupMsg.setDuplicateFlag();
		brokerHandler.assertMessages( //
				new ConnectMessage("abc", false, 1), //
				new PubMessage(QoS.AT_LEAST_ONCE, false, "foo", 123, new byte[] { 1, 2, 3 }), //
				dupMsg, //
				dupMsg, //
				new DisconnectMessage());
		clientHandler.assertMessages(new ConnAckMessage(ConnectReturnCode.ACCEPTED), new PubAckMessage(123));

		clientHandler.assertChannelClosedCount(1);
		brokerHandler.assertChannelClosedCount(1);
	}

}
