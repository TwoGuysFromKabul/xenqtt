package net.sf.xenqtt.gateway;

import java.util.ArrayList;
import java.util.List;

/**
 * The connections to the broker and all the clients for one client ID is a gateway session.
 */
public class GatewaySessionImpl extends Thread implements GatewaySession {

	private static final List<ClientConnection> clients = new ArrayList<ClientConnection>();

	/**
	 * @param clientId
	 *            The clientId for this session
	 */
	public GatewaySessionImpl(String clientId) {
		super("GatewaySession-" + clientId);
		setDaemon(true);
	}
}
