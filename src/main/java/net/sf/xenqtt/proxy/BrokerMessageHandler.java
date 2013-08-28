package net.sf.xenqtt.proxy;

import net.sf.xenqtt.message.MessageHandler;
import net.sf.xenqtt.message.MqttChannel;

/**
 * Handles messages from the proxy's broker connection
 */
interface BrokerMessageHandler extends MessageHandler {

	/**
	 * Called by a {@link ProxySession} when a new client connects.
	 */
	void newClientChannel(MqttChannel clientChannel);

	/**
	 * Called by a {@link ProxySession} to close a client channel.
	 */
	void closeClientChannel(MqttChannel channel);
}
