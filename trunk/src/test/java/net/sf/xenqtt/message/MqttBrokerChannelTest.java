package net.sf.xenqtt.message;

import static org.junit.Assert.*;

import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

public class MqttBrokerChannelTest extends MqttChannelTestBase<MqttChannelTestBase<?, ?>.TestChannel, MqttBrokerChannel> {

	@Override
	TestChannel newClientChannel(CountDownLatch connectCompleteLatch) throws Exception {
		return new TestChannel("localhost", port, clientHandler, selector, 10000, connectCompleteLatch);
	}

	@Override
	MqttBrokerChannel newBrokerChannel(SocketChannel brokerSocketChannel) throws Exception {
		return new MqttBrokerChannel(brokerSocketChannel, brokerHandler, selector, 10000);
	}

	@Test
	public void testPingResponse() throws Exception {

		establishConnection();

		clientChannel.send(new PingReqMessage(), null);

		readWrite(1, 0);

		clientHandler.assertMessageTypes(MessageType.PINGRESP);
	}

	@Test
	public void testKeepAlive_NotYetConnected() throws Exception {

		establishConnection();

		assertEquals(Long.MAX_VALUE, brokerChannel.keepAlive(now, 0));

		assertTrue(brokerChannel.isOpen());
	}

	@Test
	public void testKeepAlive_Disconnected() throws Exception {

		establishConnection();

		brokerChannel.connected(1000);
		brokerChannel.disconnected();
		assertEquals(Long.MAX_VALUE, brokerChannel.keepAlive(now, 0));

		assertTrue(brokerChannel.isOpen());
	}

	@Test
	public void testKeepAlive_Connected_TimeNotExpired() throws Exception {

		establishConnection();

		brokerChannel.connected(1000);
		assertEquals(1400, brokerChannel.keepAlive(now, now - 100));

		assertTrue(brokerChannel.isOpen());
	}

	@Test
	public void testKeepAlive_Connected_TimeExpired() throws Exception {

		establishConnection();

		brokerChannel.connected(1000);
		assertEquals(-1, brokerChannel.keepAlive(now, now - 1500));

		assertFalse(brokerChannel.isOpen());
	}
}
