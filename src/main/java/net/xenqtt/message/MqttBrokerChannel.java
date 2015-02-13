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

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import net.xenqtt.Log;

/**
 * An {@link MqttChannel} to use for the client side of the connection.
 */
public final class MqttBrokerChannel extends AbstractMqttChannel {

	private long maxMessageIntervalMillis;

	/**
	 * @param messageResendIntervalMillis
	 *            Millis between attempts to resend a message that {@link MqttMessage#isAckable()}. 0 to disable message resends
	 */
	public MqttBrokerChannel(SocketChannel channel, MessageHandler handler, Selector selector, long messageResendIntervalMillis, MutableMessageStats stats)
			throws IOException {
		super(channel, handler, selector, messageResendIntervalMillis, stats);
	}

	/**
	 * @see net.xenqtt.message.AbstractMqttChannel#connected(long)
	 */
	@Override
	void connected(long pingIntervalMillis) {

		maxMessageIntervalMillis = (long) Math.ceil(pingIntervalMillis * 1.5);
	}

	/**
	 * @see net.xenqtt.message.AbstractMqttChannel#disconnected()
	 */
	@Override
	void disconnected() {

		maxMessageIntervalMillis = 0;
	}

	/**
	 * @see net.xenqtt.message.AbstractMqttChannel#keepAlive(long, long, long)
	 */
	@Override
	long keepAlive(long now, long lastMessageReceived, long lastMessageSent) throws Exception {

		if (maxMessageIntervalMillis == 0) {
			return Long.MAX_VALUE;
		}

		long elapsed = now - lastMessageReceived;
		if (elapsed < maxMessageIntervalMillis) {
			return maxMessageIntervalMillis - elapsed;
		}

		Log.warn("%s lost communication with client", this);

		close();

		return -1;
	}

	/**
	 * @see net.xenqtt.message.AbstractMqttChannel#pingReq(long, net.xenqtt.message.PingReqMessage)
	 */
	@Override
	void pingReq(long now, PingReqMessage message) throws Exception {

		send(new PingRespMessage(), null);
	}

	/**
	 * @see net.xenqtt.message.AbstractMqttChannel#pingResp(long, net.xenqtt.message.PingRespMessage)
	 */
	@Override
	void pingResp(long now, PingRespMessage message) throws Exception {
		// This should never happen. If it does just ignore it.
	}
}
