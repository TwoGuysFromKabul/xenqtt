package net.sf.xenqtt.message;

/**
 * Handles received {@link MqttMessage}s. There is a method for each message type. This handler will always be called by the IO thread that owns the channel. It
 * is safe to invoke any method on the channel in these methods. Implementations must not block this thread.
 */
public interface MessageHandler {

	/**
	 * Called when a {@link ConnectMessage} is received through the specified channel
	 */
	void connect(MqttChannel channel, ConnectMessage message) throws Exception;

	/**
	 * Called when a {@link ConnAckMessage} is received through the specified channel
	 */
	void connAck(MqttChannel channel, ConnAckMessage message) throws Exception;

	/**
	 * Called when a {@link PublishMessage} is received through the specified channel
	 */
	void publish(MqttChannel channel, PublishMessage message) throws Exception;

	/**
	 * Called when a {@link PubAckMessage} is received through the specified channel
	 */
	void pubAck(MqttChannel channel, PubAckMessage message) throws Exception;

	/**
	 * Called when a {@link PubRecMessage} is received through the specified channel
	 */
	void pubRec(MqttChannel channel, PubRecMessage message) throws Exception;

	/**
	 * Called when a {@link PubRelMessage} is received through the specified channel
	 */
	void pubRel(MqttChannel channel, PubRelMessage message) throws Exception;

	/**
	 * Called when a {@link PubCompMessage} is received through the specified channel
	 */
	void pubComp(MqttChannel channel, PubCompMessage message) throws Exception;

	/**
	 * Called when a {@link SubscribeMessage} is received through the specified channel
	 */
	void subscribe(MqttChannel channel, SubscribeMessage message) throws Exception;

	/**
	 * Called when a {@link SubAckMessage} is received through the specified channel
	 */
	void subAck(MqttChannel channel, SubAckMessage message) throws Exception;

	/**
	 * Called when a {@link UnsubscribeMessage} is received through the specified channel
	 */
	void unsubscribe(MqttChannel channel, UnsubscribeMessage message) throws Exception;

	/**
	 * Called when a {@link UnsubAckMessage} is received through the specified channel
	 */
	void unsubAck(MqttChannel channel, UnsubAckMessage message) throws Exception;

	/**
	 * Called when a {@link DisconnectMessage} is received through the specified channel
	 */
	void disconnect(MqttChannel channel, DisconnectMessage message) throws Exception;

	/**
	 * Called when a {@link MqttChannel MQTT channel} is formally closed.
	 * 
	 * @param channel
	 *            The channel that was closed
	 */
	void channelClosed(MqttChannel channel);
}
