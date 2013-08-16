package net.sf.xenqtt.message;

/**
 * Handles received {@link MqttMessage}s. There is a method for each message type.
 */
public interface MessageHandler {

	/**
	 * Called when a {@link ConnectMessage} is received
	 */
	void handle(ConnectMessage message);

	/**
	 * Called when a {@link ConnAckMessage} is received
	 */
	void handle(ConnAckMessage message);

	/**
	 * Called when a {@link PublishMessage} is received
	 */
	void handle(PublishMessage message);

	/**
	 * Called when a {@link PubAckMessage} is received
	 */
	void handle(PubAckMessage message);

	/**
	 * Called when a {@link PubRecMessage} is received
	 */
	void handle(PubRecMessage message);

	/**
	 * Called when a {@link PubRelMessage} is received
	 */
	void handle(PubRelMessage message);

	/**
	 * Called when a {@link PubCompMessage} is received
	 */
	void handle(PubCompMessage message);

	/**
	 * Called when a {@link SubscribeMessage} is received
	 */
	void handle(SubscribeMessage message);

	/**
	 * Called when a {@link SubAckMessage} is received
	 */
	void handle(SubAckMessage message);

	/**
	 * Called when a {@link UnsubscribeMessage} is received
	 */
	void handle(UnsubscribeMessage message);

	/**
	 * Called when a {@link UnsubAckMessage} is received
	 */
	void handle(UnsubAckMessage message);

	/**
	 * Called when a {@link PingReqMessage} is received
	 */
	void handle(PingReqMessage message);

	/**
	 * Called when a {@link PingRespMessage} is received
	 */
	void handle(PingRespMessage message);

	/**
	 * Called when a {@link DisconnectMessage} is received
	 */
	void handle(DisconnectMessage message);
}
