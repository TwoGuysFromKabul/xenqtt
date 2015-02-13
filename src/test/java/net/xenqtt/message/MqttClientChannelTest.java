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
package net.xenqtt.message;

import static org.junit.Assert.*;

import java.nio.channels.SocketChannel;

import net.xenqtt.message.BlockingCommand;
import net.xenqtt.message.MessageStatsImpl;
import net.xenqtt.message.MessageType;
import net.xenqtt.message.MqttClientChannel;
import net.xenqtt.message.PingRespMessage;
import net.xenqtt.message.PubAckMessage;

import org.junit.Test;

public class MqttClientChannelTest extends MqttChannelTestBase<MqttClientChannel, MqttChannelTestBase<?, ?>.TestChannel> {

	@Override
	MqttClientChannel newClientChannel(BlockingCommand<?> connectionCompleteCommand) throws Exception {
		return new MqttClientChannel("localhost", port, clientHandler, selector, 10000, connectionCompleteCommand, new MessageStatsImpl(null));
	}

	@Override
	TestChannel newBrokerChannel(SocketChannel brokerSocketChannel) throws Exception {
		return new TestChannel(brokerSocketChannel, brokerHandler, selector, 10000);
	}

	@Test
	public void testKeepAlive_NotYetConnected() throws Exception {

		establishConnection();

		assertEquals(Long.MAX_VALUE, clientChannel.keepAlive(now, 0, 0));
		assertFalse(checkForPing());
	}

	@Test
	public void testKeepAlive_Disconnected() throws Exception {

		establishConnection();

		clientChannel.connected(1000);
		clientChannel.disconnected();
		assertEquals(Long.MAX_VALUE, clientChannel.keepAlive(now, now - 1000, now - 1000));
		assertFalse(checkForPing());
	}

	@Test
	public void testKeepAlive_Connected_ReceivedTimeNotExpired_SentTimeNotExpired_NoPingPending() throws Exception {

		establishConnection();

		clientChannel.connected(1000);
		assertEquals(900, clientChannel.keepAlive(now, now - 100, now - 100));
		assertFalse(checkForPing());
	}

	@Test
	public void testKeepAlive_Connected_ReceivedTimeNotExpired_SentTimeExpired_NoPingPending() throws Exception {

		establishConnection();

		clientChannel.connected(1000);
		assertEquals(1000, clientChannel.keepAlive(now, now - 100, now - 2000));
		assertTrue(checkForPing());

		assertTrue(clientChannel.isOpen());
	}

	@Test
	public void testKeepAlive_Connected_ReceivedTimeExpired_SentTimeNotExpired_NoPingPending() throws Exception {

		establishConnection();

		clientChannel.connected(1000);
		assertEquals(900, clientChannel.keepAlive(now, now - 2000, now - 100));
		assertFalse(checkForPing());
	}

	@Test
	public void testKeepAlive_Connected_ReceivedTimeExpired_SentTimeExpired_NoPingPending() throws Exception {

		establishConnection();

		clientChannel.connected(1000);
		assertEquals(1000, clientChannel.keepAlive(now, now - 2000, now - 2000));
		assertTrue(checkForPing());

		assertTrue(clientChannel.isOpen());
	}

	@Test
	public void testKeepAlive_Connected_PingResponseTimeExpired() throws Exception {

		establishConnection();

		clientChannel.connected(1000);
		assertEquals(1000, clientChannel.keepAlive(now, now - 1000, now - 1000));
		assertEquals(-1, clientChannel.keepAlive(now + 1001, now, now));

		assertFalse(clientChannel.isOpen());
	}

	@Test
	public void testKeepAlive_Connected_ReceivedTimeExpired__Sent_TimeExpired_PingGotResponse() throws Exception {

		establishConnection();

		clientChannel.connected(1000);
		assertEquals(1000, clientChannel.keepAlive(now, now - 2000, now - 2000));
		assertTrue(checkForPing());

		// send the response
		brokerChannel.send(new PingRespMessage(), null);
		// send this message to force the ping to flush
		brokerChannel.send(new PubAckMessage(1), null);
		readWrite(1, 0);

		// validate another ping was sent instead of the channel being closed
		assertEquals(1000, clientChannel.keepAlive(now, now - 100, now - 2000));
		assertTrue(checkForPing());

		assertTrue(clientChannel.isOpen());
	}

	private boolean checkForPing() throws Exception {

		PubAckMessage msg = new PubAckMessage(1);
		clientChannel.send(msg, null);

		readWrite(0, 1);
		brokerHandler.assertMessageCount(1);

		return brokerHandler.message(0).getMessageType() == MessageType.PINGREQ;
	}
}
