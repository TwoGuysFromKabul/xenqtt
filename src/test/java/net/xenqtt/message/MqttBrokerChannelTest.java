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
import net.xenqtt.message.MessageType;
import net.xenqtt.message.MqttBrokerChannel;
import net.xenqtt.message.PingReqMessage;

import org.junit.Test;

public class MqttBrokerChannelTest extends MqttChannelTestBase<MqttChannelTestBase<?, ?>.TestChannel, MqttBrokerChannel> {

	@Override
	TestChannel newClientChannel(BlockingCommand<?> connectionCompleteCommand) throws Exception {
		return new TestChannel("localhost", port, clientHandler, selector, 10000, connectionCompleteCommand);
	}

	@Override
	MqttBrokerChannel newBrokerChannel(SocketChannel brokerSocketChannel) throws Exception {
		return new MqttBrokerChannel(brokerSocketChannel, brokerHandler, selector, 10000, null);
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

		assertEquals(Long.MAX_VALUE, brokerChannel.keepAlive(now, 0, 0));

		assertTrue(brokerChannel.isOpen());
	}

	@Test
	public void testKeepAlive_Disconnected() throws Exception {

		establishConnection();

		brokerChannel.connected(1000);
		brokerChannel.disconnected();
		assertEquals(Long.MAX_VALUE, brokerChannel.keepAlive(now, 0, 0));

		assertTrue(brokerChannel.isOpen());
	}

	@Test
	public void testKeepAlive_Connected_TimeNotExpired() throws Exception {

		establishConnection();

		brokerChannel.connected(1000);
		assertEquals(1400, brokerChannel.keepAlive(now, now - 100, 0));

		assertTrue(brokerChannel.isOpen());
	}

	@Test
	public void testKeepAlive_Connected_TimeExpired() throws Exception {

		establishConnection();

		brokerChannel.connected(1000);
		assertEquals(-1, brokerChannel.keepAlive(now, now - 1500, 0));

		assertFalse(brokerChannel.isOpen());
	}
}
