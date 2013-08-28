package net.sf.xenqtt.proxy;

import net.sf.xenqtt.message.ConnectMessage;
import net.sf.xenqtt.message.MqttChannel;

/**
 * A session in the clustered client gateway handles the interaction between the broker and all the clients in the same cluster; aka all the clients with the
 * same client ID. The {@link ConnectMessage} from the first client is used to connect to the broker. The {@link ConnectMessage}s from subsequent clients must
 * be exactly the same or the connection will be rejected. All implementations of this API must be thread safe.
 */
interface GatewaySession {

	/**
	 * Adds a new client to this session. Do not call this for the very first client that is used to create the session, only for clients that connect after the
	 * session is created. This method is thread safe.
	 * 
	 * @param channel
	 *            The channel to the client
	 * @param connectMessage
	 *            The {@link ConnectMessage} received from the client
	 * @return True if the client was added to the session. False if the client was not added to the session which means the session is not open.
	 */
	boolean addClient(MqttChannel channel, ConnectMessage connectMessage);

	/**
	 * @return True as long as this session is open. If false then this session should not be used any more. This method is thread safe.
	 */
	boolean isOpen();

	/**
	 * Closes the session. This will immediately and synchronously close all client connections and the broker connection without sending a disconnect message.
	 * This method is thread safe.
	 */
	void close();
}
