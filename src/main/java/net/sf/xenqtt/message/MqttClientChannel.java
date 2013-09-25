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

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import net.sf.xenqtt.Log;

/**
 * An {@link MqttChannel} to use for the client side of the connection.
 */
public final class MqttClientChannel extends AbstractMqttChannel {

	private long pingIntervalMillis;
	private long pingTime = Long.MAX_VALUE;

	/**
	 * Starts an asynchronous connection to the specified host and port. When a {@link SelectionKey} for the specified selector has
	 * {@link SelectionKey#OP_CONNECT} as a ready op then {@link #finishConnect()} should be called.
	 * 
	 * @param messageResendIntervalMillis
	 *            Millis between attempts to resend a message that {@link MqttMessage#isAckable()}. 0 to disable message resends
	 * @param connectionCompleteCommand
	 *            If not null then this latch is {@link BlockingCommand#complete(Throwable) complete} when the {@link ConnAckMessage} is received.
	 */
	public MqttClientChannel(String host, int port, MessageHandler handler, Selector selector, long messageResendIntervalMillis,
			BlockingCommand<?> connectionCompleteCommand) throws IOException {
		super(host, port, handler, selector, messageResendIntervalMillis, connectionCompleteCommand);
	}

	/**
	 * @see net.sf.xenqtt.message.AbstractMqttChannel#connected(long)
	 */
	@Override
	void connected(long pingIntervalMillis) {

		this.pingIntervalMillis = pingIntervalMillis;
	}

	/**
	 * @see net.sf.xenqtt.message.AbstractMqttChannel#disconnected()
	 */
	@Override
	void disconnected() {

		pingIntervalMillis = 0;
	}

	/**
	 * @see net.sf.xenqtt.message.AbstractMqttChannel#keepAlive(long, long)
	 */
	@Override
	long keepAlive(long now, long lastMessageReceived) throws Exception {

		if (pingIntervalMillis == 0) {
			return Long.MAX_VALUE;
		}

		long elapsed = now - lastMessageReceived;
		if (elapsed < pingIntervalMillis) {
			return pingIntervalMillis - elapsed;
		}

		long millisSincePing = now - pingTime;
		if (millisSincePing > pingIntervalMillis) {
			Log.warn("%s lost communication with broker, closing channel", this);
			close();
			return -1;
		}

		if (pingTime == Long.MAX_VALUE) {
			send(new PingReqMessage(), null);
			pingTime = now;
		}

		return pingIntervalMillis;
	}

	/**
	 * @see net.sf.xenqtt.message.AbstractMqttChannel#pingReq(long, net.sf.xenqtt.message.PingReqMessage)
	 */
	@Override
	void pingReq(long now, PingReqMessage message) throws Exception {
		// This should never happen. If it does just ignore it.
	}

	/**
	 * @see net.sf.xenqtt.message.AbstractMqttChannel#pingResp(long, net.sf.xenqtt.message.PingRespMessage)
	 */
	@Override
	void pingResp(long now, PingRespMessage message) throws Exception {

		pingTime = Long.MAX_VALUE;
	}
}
