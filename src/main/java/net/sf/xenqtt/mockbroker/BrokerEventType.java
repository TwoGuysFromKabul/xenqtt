package net.sf.xenqtt.mockbroker;

import net.sf.xenqtt.message.MqttMessage;

/**
 * Type of event for {@link BrokerEvent}.
 */
public enum BrokerEventType {

	/**
	 * An {@link MqttMessage} queued for sending from the broker to a client
	 */
	MSG_SENT,

	/**
	 * An {@link MqttMessage} was received by the broker from the client
	 */
	MSG_RECEIVED,

	/**
	 * A socket was opened on the broker from the client (a new client connected).
	 */
	CHANNEL_OPENED,

	/**
	 * The broker's socket to the client was closed.
	 */
	CHANNEL_CLOSED
}
