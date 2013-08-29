package net.sf.xenqtt.proxy;

import java.util.Map;

import net.sf.xenqtt.message.ConnAckMessage;
import net.sf.xenqtt.message.ConnectMessage;
import net.sf.xenqtt.message.DisconnectMessage;
import net.sf.xenqtt.message.IdentifiableMqttMessage;
import net.sf.xenqtt.message.MessageHandler;
import net.sf.xenqtt.message.MqttChannel;
import net.sf.xenqtt.message.PingReqMessage;
import net.sf.xenqtt.message.PingRespMessage;
import net.sf.xenqtt.message.PubAckMessage;
import net.sf.xenqtt.message.PubCompMessage;
import net.sf.xenqtt.message.PubRecMessage;
import net.sf.xenqtt.message.PubRelMessage;
import net.sf.xenqtt.message.PublishMessage;
import net.sf.xenqtt.message.SubAckMessage;
import net.sf.xenqtt.message.SubscribeMessage;
import net.sf.xenqtt.message.UnsubAckMessage;
import net.sf.xenqtt.message.UnsubscribeMessage;

/**
 * Handles messages from proxy client connections
 */
final class ClientMessageHandler implements MessageHandler {

	// FIXME [jim] - need to add pinging the broker

	// FIXME [jim] - need to deal with what happens if an ID for an inflight message becomes needed again because it is still outstanding after all other IDs
	// have been used.
	private int nextMessageId = 1;

	// FIXME [jim] - this needs to have the original message ID in the value as well
	private final Map<Integer, ChannelAndId> clientChannelsByMessageId;
	private final MqttChannel brokerChannel;

	public ClientMessageHandler(Map<Integer, ChannelAndId> clientChannelsByMessageId, MqttChannel brokerChannel) {
		this.clientChannelsByMessageId = clientChannelsByMessageId;
		this.brokerChannel = brokerChannel;
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#handle(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.ConnectMessage)
	 */
	@Override
	public void handle(MqttChannel channel, ConnectMessage message) throws Exception {
		// this should only happen if the message is getting resent so ignore it
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#handle(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.ConnAckMessage)
	 */
	@Override
	public void handle(MqttChannel channel, ConnAckMessage message) throws Exception {
		// should never be received from the client
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#handle(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.PublishMessage)
	 */
	@Override
	public void handle(MqttChannel channel, PublishMessage message) throws Exception {
		changeMessageId(channel, message);
		brokerChannel.send(message);
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#handle(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.PubAckMessage)
	 */
	@Override
	public void handle(MqttChannel channel, PubAckMessage message) throws Exception {
		brokerChannel.send(message);
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#handle(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.PubRecMessage)
	 */
	@Override
	public void handle(MqttChannel channel, PubRecMessage message) throws Exception {
		brokerChannel.send(message);
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#handle(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.PubRelMessage)
	 */
	@Override
	public void handle(MqttChannel channel, PubRelMessage message) throws Exception {
		brokerChannel.send(message);
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#handle(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.PubCompMessage)
	 */
	@Override
	public void handle(MqttChannel channel, PubCompMessage message) throws Exception {
		brokerChannel.send(message);
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#handle(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.SubscribeMessage)
	 */
	@Override
	public void handle(MqttChannel channel, SubscribeMessage message) throws Exception {
		changeMessageId(channel, message);
		brokerChannel.send(message);
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#handle(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.SubAckMessage)
	 */
	@Override
	public void handle(MqttChannel channel, SubAckMessage message) throws Exception {
		// should never be received from the subscriber
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#handle(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.UnsubscribeMessage)
	 */
	@Override
	public void handle(MqttChannel channel, UnsubscribeMessage message) throws Exception {
		changeMessageId(channel, message);
		brokerChannel.send(message);
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#handle(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.UnsubAckMessage)
	 */
	@Override
	public void handle(MqttChannel channel, UnsubAckMessage message) throws Exception {
		brokerChannel.send(message);
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#handle(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.PingReqMessage)
	 */
	@Override
	public void handle(MqttChannel channel, PingReqMessage message) throws Exception {
		brokerChannel.send(new PingRespMessage());
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#handle(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.PingRespMessage)
	 */
	@Override
	public void handle(MqttChannel channel, PingRespMessage message) throws Exception {
		// should never be received from the client
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#handle(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.DisconnectMessage)
	 */
	@Override
	public void handle(MqttChannel channel, DisconnectMessage message) throws Exception {
		// FIXME [jim] - need to close this channel and if the last client then need to disconnect from the broker
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#channelClosed(net.sf.xenqtt.message.MqttChannel)
	 */
	@Override
	public void channelClosed(MqttChannel channel) {
		// TODO [jeremy] - Implement this method.
	}

	private void changeMessageId(MqttChannel channel, IdentifiableMqttMessage message) {

		int newId = getNextMessageId();

		if (message.getQoSLevel() > 0) {

			clientChannelsByMessageId.put(newId, new ChannelAndId(channel, message.getMessageId()));
		}

		message.setMessageId(newId);
	}

	private int getNextMessageId() {

		int next = nextMessageId++;
		if (nextMessageId > 0xffff) {
			nextMessageId = 1;
		}

		return next;
	}
}
