package net.sf.xenqtt.mockbroker;

import net.sf.xenqtt.message.ConnectMessage;
import net.sf.xenqtt.message.MqttMessage;

/**
 * An event that hqppened on the broker (message sent, message received, etc)
 */
public final class BrokerEvent {

	private final Client client;
	private final BrokerEventType eventType;
	private final MqttMessage message;

	BrokerEvent(BrokerEventType eventType, Client client, MqttMessage message) {
		this.eventType = eventType;
		this.client = client;
		this.message = message;
	}

	/**
	 * @return The client associated with this event
	 */
	public Client getClient() {
		return client;
	}

	/**
	 * @return The client ID of the client associated with this event. Null if no {@link ConnectMessage} has been received.
	 */
	public String getClientId() {
		return client.clientId;
	}

	/**
	 * Sends the message to the client associated with this event
	 */
	public void send(MqttMessage message) {
		client.send(message);
	}

	/**
	 * @return The type of event that occurred
	 */
	public BrokerEventType getEventType() {
		return eventType;
	}

	/**
	 * @return The message that triggered the event. Only applicable for {@link BrokerEventType#MESSAGE_RECEIVED} and {@link BrokerEventType#MESSAGE_SENT}
	 */
	public <T extends MqttMessage> T getMessage() {
		@SuppressWarnings("unchecked")
		T m = (T) message;
		return m;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String clientId = client == null ? null : client.clientId;
		return "BrokerEvent [clientId=" + clientId + ", eventType=" + eventType + ", message=" + message + "]";
	}
}
