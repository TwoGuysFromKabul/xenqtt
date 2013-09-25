package net.sf.xenqtt.mockbroker;

import net.sf.xenqtt.XenqttUtil;
import net.sf.xenqtt.message.ConnectMessage;
import net.sf.xenqtt.message.MqttChannel;
import net.sf.xenqtt.message.MqttMessage;

/**
 * Info on a client connected to the mock broker
 */
public final class Client {

	String clientId;
	boolean cleanSession;
	private int nextMessageId;
	private final MqttChannel channel;
	private final BrokerEvents events;

	Client(MqttChannel channel, BrokerEvents events) {
		this.channel = channel;
		this.events = events;
	}

	/**
	 * @return The client ID of this client. Null if no {@link ConnectMessage} has been received.
	 */
	public String getClientId() {
		return clientId;
	}

	/**
	 * Sends the message to this client
	 */
	public void send(MqttMessage message) {
		XenqttUtil.validateNotNull("message", message);

		channel.send(message, null);
		events.addEvent(BrokerEventType.MESSAGE_SENT, this, message);
	}

	/**
	 * @return The message ID to use for the next identifiable message sent to this client by the broker
	 */
	int getNextMessageId() {

		if (++nextMessageId > 0xffff) {
			nextMessageId = 1;
		}
		return nextMessageId;
	}

	/**
	 * Called whenever an {@link MqttMessage} is received
	 */
	void messageReceived(MqttMessage message) {
		events.addEvent(BrokerEventType.MESSAGE_RECEIVED, this, message);
	}

	/**
	 * @return The client's address
	 */
	String remoteAddress() {
		return channel.getRemoteAddress();
	}
}
