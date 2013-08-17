package net.sf.xenqtt.gateway;

import java.util.HashMap;
import java.util.Map;

//FIXME [jim] - for the gateway we want to always invoke all read ops as they will generate writes. then call all write ops
public class GatewayServer {

	private final Map<String, GatewayServer> serversByClientId = new HashMap<String, GatewayServer>();

	public static void main(String[] args) {
		System.out.println("Hello World");
	}
}
