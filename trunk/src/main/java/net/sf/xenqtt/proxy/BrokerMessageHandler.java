package net.sf.xenqtt.proxy;

import net.sf.xenqtt.message.MessageHandler;
import net.sf.xenqtt.message.MqttChannel;

/**
 * Handles messages from the gateway's broker connection
 */
interface BrokerMessageHandler extends MessageHandler {

	/**
	 * Called by a {@link GatewaySession} when a new client connects.
	 */
	void newClientChannel(MqttChannel clientChannel);

	/**
	 * Called by a {@link GatewaySession} to close a client channel.
	 */
	void closeClientChannel(MqttChannel channel);
}
