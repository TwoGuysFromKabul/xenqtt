package net.sf.xenqtt;

import static org.junit.Assert.*;

import java.net.ConnectException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import net.sf.xenqtt.message.ConnAckMessage;
import net.sf.xenqtt.message.ConnectMessage;
import net.sf.xenqtt.message.ConnectReturnCode;
import net.sf.xenqtt.message.DisconnectMessage;
import net.sf.xenqtt.message.MessageType;
import net.sf.xenqtt.message.MqttChannel;
import net.sf.xenqtt.message.MqttChannelRef;
import net.sf.xenqtt.message.PubAckMessage;
import net.sf.xenqtt.message.PublishMessage;
import net.sf.xenqtt.message.QoS;
import net.sf.xenqtt.mock.MockMessageHandler;
import net.sf.xenqtt.mock.MockServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

public class ChannelManagerImplTest {

	MqttChannelRef clientChannel;
	MqttChannelRef brokerChannel;

	MockServer server = new MockServer();
	MockMessageHandler clientHandler = new MockMessageHandler();
	MockMessageHandler brokerHandler = new MockMessageHandler();

	ChannelManagerImpl manager = new ChannelManagerImpl(2);

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);

		manager.init();
	}

	@After
	public void after() {

		manager.shutdown();
		server.close();
	}

	@Test
	public void testInit_IsRunning_Shutdown() throws Exception {

		manager.shutdown();

		manager = new ChannelManagerImpl(10);
		assertFalse(manager.isRunning());
		manager.init();
		assertTrue(manager.isRunning());
		manager.shutdown();
		assertFalse(manager.isRunning());
	}

	@Test
	public void testShutdownClosesAll() throws Exception {

		clientChannel = manager.newClientChannel("localhost", server.getPort(), clientHandler);
		brokerChannel = manager.newBrokerChannel(server.nextClient(1000), brokerHandler);

		clientHandler.assertChannelClosedCount(0);
		brokerHandler.assertChannelClosedCount(0);

		manager.shutdown();

		clientHandler.assertChannelClosedCount(1);
		brokerHandler.assertChannelClosedCount(1);
	}

	@Test
	public void testNewClientChannel_InvalidHost() throws Exception {

		try {
			manager.newClientChannel("foo", 123, clientHandler);
			fail("Exception expected");
		} catch (RuntimeException e) {
			clientHandler.assertLastChannelClosedCause(e.getCause());
		}
	}

	@Test
	public void testNewClientChannel_UnableToConnect() throws Exception {

		CountDownLatch trigger = new CountDownLatch(1);
		clientHandler.onChannelClosed(trigger);
		clientChannel = manager.newClientChannel("localhost", 19876, clientHandler);

		assertTrue(trigger.await(1000, TimeUnit.SECONDS));

		clientHandler.assertLastChannelClosedCause(ConnectException.class);
	}

	@Test
	public void testNewClientChannel_Success() throws Exception {

		CountDownLatch trigger = new CountDownLatch(1);
		clientHandler.onChannelOpened(trigger);

		clientChannel = manager.newClientChannel("localhost", server.getPort(), clientHandler);

		assertTrue(trigger.await(1000, TimeUnit.SECONDS));

		clientHandler.assertChannelClosedCount(0);
	}

	@Test
	public void testNewBrokerChannel() throws Exception {

		clientChannel = manager.newClientChannel("localhost", server.getPort(), clientHandler);
		brokerChannel = manager.newBrokerChannel(server.nextClient(1000), brokerHandler);

		clientHandler.assertChannelOpenedCount(1);
		brokerHandler.assertChannelOpenedCount(1);
		clientHandler.assertChannelClosedCount(0);
		brokerHandler.assertChannelClosedCount(0);
	}

	@Test
	public void testSend() throws Exception {

		CountDownLatch trigger = new CountDownLatch(1);
		brokerHandler.onMessage(MessageType.PUBACK, trigger);

		clientChannel = manager.newClientChannel("localhost", server.getPort(), clientHandler);
		brokerChannel = manager.newBrokerChannel(server.nextClient(1000), brokerHandler);

		manager.send(clientChannel, new PubAckMessage(1));

		assertTrue(trigger.await(1, TimeUnit.SECONDS));

		brokerHandler.assertMessages(new PubAckMessage(1));
	}

	@Test
	public void testSendToAll() throws Exception {

		CountDownLatch trigger = new CountDownLatch(2);
		clientHandler.onMessage(MessageType.PUBACK, trigger);
		brokerHandler.onMessage(MessageType.PUBACK, trigger);

		clientChannel = manager.newClientChannel("localhost", server.getPort(), clientHandler);
		brokerChannel = manager.newBrokerChannel(server.nextClient(1000), brokerHandler);

		manager.sendToAll(new PubAckMessage(1));

		assertTrue(trigger.await(1000, TimeUnit.SECONDS));

		clientHandler.assertMessages(new PubAckMessage(1));
		brokerHandler.assertMessages(new PubAckMessage(1));
	}

	@Test
	public void testClose() throws Exception {

		clientChannel = manager.newClientChannel("localhost", server.getPort(), clientHandler);
		brokerChannel = manager.newBrokerChannel(server.nextClient(1000), brokerHandler);

		clientHandler.assertChannelClosedCount(0);
		brokerHandler.assertChannelClosedCount(0);

		manager.close(clientChannel);
		clientHandler.assertChannelClosedCount(1);
		brokerHandler.assertChannelClosedCount(0);

		manager.close(brokerChannel);
		clientHandler.assertChannelClosedCount(1);
		brokerHandler.assertChannelClosedCount(1);
	}

	@Test
	public void testKeepAlive() throws Exception {
		clientHandler = new MockMessageHandler() {
			@Override
			public void channelOpened(MqttChannel channel) {
				super.channelOpened(channel);
				channel.send(new ConnectMessage("abc", false, 1));
			}
		};

		brokerHandler = new MockMessageHandler() {
			@Override
			public void connect(MqttChannel channel, ConnectMessage message) throws Exception {
				super.connect(channel, message);
				channel.send(new ConnAckMessage(ConnectReturnCode.ACCEPTED));
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

	@Test
	public void testMessageResend() throws Exception {

		clientHandler = new MockMessageHandler() {
			@Override
			public void channelOpened(MqttChannel channel) {
				super.channelOpened(channel);
				channel.send(new ConnectMessage("abc", false, 1));
			}
		};

		brokerHandler = new MockMessageHandler() {

			int publishCount;

			@Override
			public void connect(MqttChannel channel, ConnectMessage message) throws Exception {
				super.connect(channel, message);
				channel.send(new ConnAckMessage(ConnectReturnCode.ACCEPTED));
			}

			@Override
			public void publish(MqttChannel channel, PublishMessage message) throws Exception {

				if (++publishCount == 3) {
					channel.send(new PubAckMessage(message.getMessageId()));
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

		manager.send(clientChannel, new PublishMessage(false, QoS.AT_LEAST_ONCE, false, "foo", 123, new byte[] { 1, 2, 3 }));
		// give time for 3 resends but there should only be 2 since we ack the second resend
		Thread.sleep(3500);

		clientHandler.assertChannelClosedCount(0);
		brokerHandler.assertChannelClosedCount(0);

		manager.send(clientChannel, new DisconnectMessage());

		assertTrue(closedTrigger.await(1, TimeUnit.SECONDS));

		brokerHandler.assertMessages( //
				new ConnectMessage("abc", false, 1), //
				new PublishMessage(false, QoS.AT_LEAST_ONCE, false, "foo", 123, new byte[] { 1, 2, 3 }), //
				new PublishMessage(true, QoS.AT_LEAST_ONCE, false, "foo", 123, new byte[] { 1, 2, 3 }), //
				new PublishMessage(true, QoS.AT_LEAST_ONCE, false, "foo", 123, new byte[] { 1, 2, 3 }), //
				new DisconnectMessage());
		clientHandler.assertMessages(new ConnAckMessage(ConnectReturnCode.ACCEPTED), new PubAckMessage(123));

		clientHandler.assertChannelClosedCount(1);
		brokerHandler.assertChannelClosedCount(1);
	}
}
