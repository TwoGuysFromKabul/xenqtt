package net.sf.xenqtt.gateway;

import java.util.ArrayList;
import java.util.List;

import net.sf.xenqtt.message.ConnectMessage;
import net.sf.xenqtt.message.MqttChannel;

/**
 * The connections to the broker and all the clients for one client ID is a gateway session.
 */
public class GatewaySessionImpl extends Thread implements GatewaySession {

	private static final List<ClientConnection> clients = new ArrayList<ClientConnection>();

	/**
	 * @param channel
	 *            The channel for the first client connection in the session
	 * @param message
	 *            The {@link ConnectMessage} received from the channel
	 */
	public GatewaySessionImpl(MqttChannel channel, ConnectMessage message) {
		super("GatewaySession-" + message.getClientId());
		setDaemon(true);
	}

	/**
	 * @see net.sf.xenqtt.gateway.GatewaySession#addClient(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.ConnectMessage)
	 */
	@Override
	public void addClient(MqttChannel channel, ConnectMessage message) {
		// TODO Auto-generated method stub

	}
}
