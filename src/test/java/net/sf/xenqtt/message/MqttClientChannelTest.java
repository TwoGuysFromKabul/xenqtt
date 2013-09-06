package net.sf.xenqtt.message;

import static org.junit.Assert.*;

import java.nio.channels.SocketChannel;

import org.junit.Test;

public class MqttClientChannelTest extends MqttChannelTestBase<MqttClientChannel, MqttChannelTestBase<?, ?>.TestChannel> {

	@Override
	MqttClientChannel newClientChannel() throws Exception {
		return new MqttClientChannel("localhost", port, clientHandler, selector, 10000);
	}

	@Override
	TestChannel newBrokerChannel(SocketChannel brokerSocketChannel) throws Exception {
		return new TestChannel(brokerSocketChannel, brokerHandler, selector, 10000);
	}

	@Test
	public void testKeepAlive_NotYetConnected() throws Exception {

		establishConnection();

		assertEquals(Long.MAX_VALUE, clientChannel.keepAlive(now, 0));
		assertFalse(checkForPing());
	}

	@Test
	public void testKeepAlive_Disconnected() throws Exception {

		establishConnection();

		clientChannel.connected(1000);
		clientChannel.disconnected();
		assertEquals(Long.MAX_VALUE, clientChannel.keepAlive(now, now - 1000));
		assertFalse(checkForPing());
	}

	@Test
	public void testKeepAlive_Connected_TimeNotExpired() throws Exception {

		establishConnection();

		clientChannel.connected(1000);
		assertEquals(900, clientChannel.keepAlive(now, now - 100));
		assertFalse(checkForPing());
	}

	@Test
	public void testKeepAlive_Connected_TimeExpired_NoPingPending() throws Exception {

		establishConnection();

		clientChannel.connected(1000);
		assertEquals(1000, clientChannel.keepAlive(now, now - 2000));
		assertTrue(checkForPing());

		assertTrue(clientChannel.isOpen());
	}

	@Test
	public void testKeepAlive_Connected_TimeExpired_PingPending() throws Exception {

		establishConnection();

		clientChannel.connected(1000);
		assertEquals(1000, clientChannel.keepAlive(now, now - 2000));
		assertEquals(-1, clientChannel.keepAlive(now, now - 2000));

		assertFalse(clientChannel.isOpen());
	}

	@Test
	public void testKeepAlive_Connected_TimeExpired_PingGotResponse() throws Exception {

		establishConnection();

		clientChannel.connected(1000);
		assertEquals(1000, clientChannel.keepAlive(now, now - 2000));
		assertTrue(checkForPing());

		// send the response
		brokerChannel.send(new PingRespMessage());
		// send this message to force the ping to flush
		brokerChannel.send(new PubAckMessage(1));
		readWrite(1, 0);

		// validate another ping was sent instead of the channel being closed
		assertEquals(1000, clientChannel.keepAlive(now, now - 2000));
		assertTrue(checkForPing());

		assertTrue(clientChannel.isOpen());
	}

	private boolean checkForPing() throws Exception {

		PubAckMessage msg = new PubAckMessage(1);
		clientChannel.send(msg);

		readWrite(0, 1);
		brokerHandler.assertMessageCount(1);

		return brokerHandler.message(0).getMessageType() == MessageType.PINGREQ;
	}
}
