package net.sf.xenqtt.proxy;

import net.sf.xenqtt.message.MqttChannel;

/**
 * Contains an {@link MqttChannel} and client size message ID. Used as the value side of a map that is keyed by the broker side message ID.
 */
final class ChannelAndId {

	final MqttChannel channel;
	final int clientSideMessageId;

	public ChannelAndId(MqttChannel channel, int clientSideMessageId) {
		this.channel = channel;
		this.clientSideMessageId = clientSideMessageId;
	}
}