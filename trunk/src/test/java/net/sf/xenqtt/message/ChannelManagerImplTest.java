package net.sf.xenqtt.message;

import static org.junit.Assert.*;

import java.net.ConnectException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import net.sf.xenqtt.MqttException;
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

	// FIXME [jim] - need to test blocking mode
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

		manager = new ChannelManagerImpl(2, false);
		manager.init();
		manager.shutdown();

		manager = new ChannelManagerImpl(10, false);
		assertFalse(manager.isRunning());
		manager.init();
		assertTrue(manager.isRunning());
		manager.shutdown();
		assertFalse(manager.isRunning());
	}

	@Test
	public void testInit_IsRunning_Shutdown_Blocking() throws Exception {

		manager = new ChannelManagerImpl(2, true);
		manager.init();
		manager.shutdown();

		manager = new ChannelManagerImpl(10, true);
		assertFalse(manager.isRunning());
		manager.init();
		assertTrue(manager.isRunning());
		manager.shutdown();
		assertFalse(manager.isRunning());
	}

	@Test
	public void testShutdownClosesAll_NonBlocking() throws Exception {

		manager = new ChannelManagerImpl(2, false);
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

		manager = new ChannelManagerImpl(2, true);
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
	public void testNewClientChannel_InvalidHost_NonBlocking() throws Exception {

		manager = new ChannelManagerImpl(2, false);
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

		manager = new ChannelManagerImpl(2, false);
		manager.init();

		CountDownLatch trigger = new CountDownLatch(1);
		clientHandler.onChannelClosed(trigger);
		clientChannel = manager.newClientChannel("localhost", 19876, clientHandler);

		assertTrue(trigger.await(1000, TimeUnit.SECONDS));

		clientHandler.assertLastChannelClosedCause(ConnectException.class);
	}

	@Test
	public void testNewClientChannel_HostPort_NonBlocking() throws Exception {

		manager = new ChannelManagerImpl(2, false);
		manager.init();

		CountDownLatch trigger = new CountDownLatch(1);
		clientHandler.onChannelOpened(trigger);

		clientChannel = manager.newClientChannel("localhost", server.getPort(), clientHandler);

		assertTrue(trigger.await(1000, TimeUnit.SECONDS));

		clientHandler.assertChannelClosedCount(0);
	}

	@Test
	public void testNewClientChannel_NonTcpUri_NonBlocking() throws Exception {

		manager = new ChannelManagerImpl(2, false);
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

		manager = new ChannelManagerImpl(2, false);
		manager.init();

		CountDownLatch trigger = new CountDownLatch(1);
		clientHandler.onChannelOpened(trigger);

		clientChannel = manager.newClientChannel("tcp://localhost:" + server.getPort(), clientHandler);

		assertTrue(trigger.await(1000, TimeUnit.SECONDS));

		clientHandler.assertChannelClosedCount(0);
	}

	@Test
	public void testNewClientChannel_Uri_NonBlocking() throws Exception {

		manager = new ChannelManagerImpl(2, false);
		manager.init();

		CountDownLatch trigger = new CountDownLatch(1);
		clientHandler.onChannelOpened(trigger);

		clientChannel = manager.newClientChannel(new URI("tcp://localhost:" + server.getPort()), clientHandler);

		assertTrue(trigger.await(1000, TimeUnit.SECONDS));

		clientHandler.assertChannelClosedCount(0);
	}

	@Test
	public void testNewBrokerChannel_NonBlocking() throws Exception {

		manager = new ChannelManagerImpl(2, false);
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

		manager = new ChannelManagerImpl(2, false);
		manager.init();

		CountDownLatch trigger = new CountDownLatch(1);
		brokerHandler.onMessage(MessageType.PUBACK, trigger);

		clientChannel = manager.newClientChannel("localhost", server.getPort(), clientHandler);
		brokerChannel = manager.newBrokerChannel(server.nextClient(1000), brokerHandler);

		manager.send(clientChannel, new PubAckMessage(1));

		assertTrue(trigger.await(1, TimeUnit.SECONDS));

		brokerHandler.assertMessages(new PubAckMessage(1));
	}

	@Test
	public void testSendToAll_NonBlocking() throws Exception {

		manager = new ChannelManagerImpl(2, false);
		manager.init();

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
	public void testClose_NonBlocking() throws Exception {

		manager = new ChannelManagerImpl(2, false);
		manager.init();

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
	public void testKeepAlive_NonBlocking() throws Exception {

		manager = new ChannelManagerImpl(2, false);
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

	@Test
	public void testMessageResend_NonBlocking() throws Exception {

		manager = new ChannelManagerImpl(2, false);
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
