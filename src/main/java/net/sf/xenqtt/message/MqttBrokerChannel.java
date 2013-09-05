package net.sf.xenqtt.message;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * An {@link MqttChannel} to use for the client side of the connection.
 */
public final class MqttBrokerChannel extends AbstractMqttChannel {

	private long maxMessageIntervalMillis;

	/**
	 * @param messageResendIntervalMillis
	 *            Millis between attempts to resend a message that {@link MqttMessage#isAckable()}. 0 to disable message resends
	 */
	public MqttBrokerChannel(SocketChannel channel, MessageHandler handler, Selector selector, long messageResendIntervalMillis) throws IOException {
		super(channel, handler, selector, messageResendIntervalMillis);
	}

	/**
	 * @see net.sf.xenqtt.message.AbstractMqttChannel#connected(long)
	 */
	@Override
	void connected(long pingIntervalMillis) {

		maxMessageIntervalMillis = (long) Math.ceil(pingIntervalMillis * 1.5);
	}

	/**
	 * @see net.sf.xenqtt.message.AbstractMqttChannel#disconnected()
	 */
	@Override
	void disconnected() {

		maxMessageIntervalMillis = 0;
	}

	/**
	 * @see net.sf.xenqtt.message.AbstractMqttChannel#keepAlive(long, long)
	 */
	@Override
	long keepAlive(long now, long lastMessageReceived) throws Exception {

		if (maxMessageIntervalMillis == 0) {
			return Long.MAX_VALUE;
		}

		long elapsed = now - lastMessageReceived;
		if (elapsed < maxMessageIntervalMillis) {
			return maxMessageIntervalMillis - elapsed;
		}

		close();

		return -1;
	}

	/**
	 * @see net.sf.xenqtt.message.AbstractMqttChannel#pingReq(long, net.sf.xenqtt.message.PingReqMessage)
	 */
	@Override
	void pingReq(long now, PingReqMessage message) throws Exception {

		send(now, new PingRespMessage());
	}

	/**
	 * @see net.sf.xenqtt.message.AbstractMqttChannel#pingResp(long, net.sf.xenqtt.message.PingRespMessage)
	 */
	@Override
	void pingResp(long now, PingRespMessage message) throws Exception {
		// This should never happen. If it does just ignore it.
	}
}
