package net.sf.xenqtt.message;

/**
 * Handles received {@link MqttMessage}s. There is a method for each message type.
 */
public interface MessageHandler {

	/**
	 * Called when a {@link ConnectMessage} is received through the specified channel
	 */
	void handle(MqttChannel channel, ConnectMessage message) throws Exception;

	/**
	 * Called when a {@link ConnAckMessage} is received through the specified channel
	 */
	void handle(MqttChannel channel, ConnAckMessage message) throws Exception;

	/**
	 * Called when a {@link PublishMessage} is received through the specified channel
	 */
	void handle(MqttChannel channel, PublishMessage message) throws Exception;

	/**
	 * Called when a {@link PubAckMessage} is received through the specified channel
	 */
	void handle(MqttChannel channel, PubAckMessage message) throws Exception;

	/**
	 * Called when a {@link PubRecMessage} is received through the specified channel
	 */
	void handle(MqttChannel channel, PubRecMessage message) throws Exception;

	/**
	 * Called when a {@link PubRelMessage} is received through the specified channel
	 */
	void handle(MqttChannel channel, PubRelMessage message) throws Exception;

	/**
	 * Called when a {@link PubCompMessage} is received through the specified channel
	 */
	void handle(MqttChannel channel, PubCompMessage message) throws Exception;

	/**
	 * Called when a {@link SubscribeMessage} is received through the specified channel
	 */
	void handle(MqttChannel channel, SubscribeMessage message) throws Exception;

	/**
	 * Called when a {@link SubAckMessage} is received through the specified channel
	 */
	void handle(MqttChannel channel, SubAckMessage message) throws Exception;

	/**
	 * Called when a {@link UnsubscribeMessage} is received through the specified channel
	 */
	void handle(MqttChannel channel, UnsubscribeMessage message) throws Exception;

	/**
	 * Called when a {@link UnsubAckMessage} is received through the specified channel
	 */
	void handle(MqttChannel channel, UnsubAckMessage message) throws Exception;

	/**
	 * Called when a {@link PingReqMessage} is received through the specified channel
	 */
	void handle(MqttChannel channel, PingReqMessage message) throws Exception;

	/**
	 * Called when a {@link PingRespMessage} is received through the specified channel
	 */
	void handle(MqttChannel channel, PingRespMessage message) throws Exception;

	/**
	 * Called when a {@link DisconnectMessage} is received through the specified channel
	 */
	void handle(MqttChannel channel, DisconnectMessage message) throws Exception;

	/**
	 * Called when a {@link MqttChannel MQTT channel} is formally closed.
	 * 
	 * @param channel
	 *            The channel that was closed
	 */
	// FIXME [jeremy OR jim] - Call this or die, it is your choice.
	void channelClosed(MqttChannel channel);
}
