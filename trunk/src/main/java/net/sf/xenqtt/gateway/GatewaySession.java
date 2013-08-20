package net.sf.xenqtt.gateway;

import net.sf.xenqtt.message.ConnectMessage;
import net.sf.xenqtt.message.MqttChannel;

/**
 * A session in the clustered client gateway handles the interaction between the broker and all the clients in the same cluster; aka all the clients with the
 * same client ID. The {@link ConnectMessage} from the first client is used to connect to the broker. The {@link ConnectMessage}s from subsequent clients must
 * be exactly the same or the connection will be rejected.
 */
public interface GatewaySession {

	/**
	 * Adds a new client to this session. Do not call this for the very first client that is used to create the session, only for clients that connect after the
	 * session is created. This method is thread safe.
	 * 
	 * @param channel
	 *            The channel to the client
	 * @param message
	 *            The {@link ConnectMessage} received from the client
	 */
	void addClient(MqttChannel channel, ConnectMessage message);
}
