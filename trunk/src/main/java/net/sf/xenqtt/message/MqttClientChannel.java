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
	private boolean pingPending;

	/**
	 * Starts an asynchronous connection to the specified host and port. When a {@link SelectionKey} for the specified selector has
	 * {@link SelectionKey#OP_CONNECT} as a ready op then {@link #finishConnect()} should be called.
	 * 
	 * @param messageResendIntervalMillis
	 *            Millis between attempts to resend a message that {@link MqttMessage#isAckable()}. 0 to disable message resends
	 */
	public MqttClientChannel(String host, int port, MessageHandler handler, Selector selector, long messageResendIntervalMillis) throws IOException {
		super(host, port, handler, selector, messageResendIntervalMillis);
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

		if (pingPending) {
			Log.warn("%s lost communication with broker, closing channel", this);
			close();
			return -1;
		}

		send(new PingReqMessage());
		pingPending = true;

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

		pingPending = false;
	}
}
