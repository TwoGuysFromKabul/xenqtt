package net.sf.xenqtt.message;

/**
 * Handles received {@link MqttMessage}s. There is a method for each message type.
 */
public interface MessageHandler {

	/**
	 * Called when a {@link ConnectMessage} is received through the specified channel
	 */
	void handle(MqttChannel channel, ConnectMessage message);

	/**
	 * Called when a {@link ConnAckMessage} is received through the specified channel
	 */
	void handle(MqttChannel channel, ConnAckMessage message);

	/**
	 * Called when a {@link PublishMessage} is received through the specified channel
	 */
	void handle(MqttChannel channel, PublishMessage message);

	/**
	 * Called when a {@link PubAckMessage} is received through the specified channel
	 */
	void handle(MqttChannel channel, PubAckMessage message);

	/**
	 * Called when a {@link PubRecMessage} is received through the specified channel
	 */
	void handle(MqttChannel channel, PubRecMessage message);

	/**
	 * Called when a {@link PubRelMessage} is received through the specified channel
	 */
	void handle(MqttChannel channel, PubRelMessage message);

	/**
	 * Called when a {@link PubCompMessage} is received through the specified channel
	 */
	void handle(MqttChannel channel, PubCompMessage message);

	/**
	 * Called when a {@link SubscribeMessage} is received through the specified channel
	 */
	void handle(MqttChannel channel, SubscribeMessage message);

	/**
	 * Called when a {@link SubAckMessage} is received through the specified channel
	 */
	void handle(MqttChannel channel, SubAckMessage message);

	/**
	 * Called when a {@link UnsubscribeMessage} is received through the specified channel
	 */
	void handle(MqttChannel channel, UnsubscribeMessage message);

	/**
	 * Called when a {@link UnsubAckMessage} is received through the specified channel
	 */
	void handle(MqttChannel channel, UnsubAckMessage message);

	/**
	 * Called when a {@link PingReqMessage} is received through the specified channel
	 */
	void handle(MqttChannel channel, PingReqMessage message);

	/**
	 * Called when a {@link PingRespMessage} is received through the specified channel
	 */
	void handle(MqttChannel channel, PingRespMessage message);

	/**
	 * Called when a {@link DisconnectMessage} is received through the specified channel
	 */
	void handle(MqttChannel channel, DisconnectMessage message);
}
