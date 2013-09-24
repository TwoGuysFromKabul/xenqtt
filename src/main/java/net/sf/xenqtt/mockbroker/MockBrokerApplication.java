package net.sf.xenqtt.mockbroker;

import net.sf.xenqtt.ApplicationArguments;
import net.sf.xenqtt.XenqttApplication;
import net.sf.xenqtt.XenqttUtil;

/**
 * Runs the {@link MockBroker} as a xenqtt command line application
 */
public class MockBrokerApplication implements XenqttApplication {

	private static String USAGE_TEXT = "[-t timeout] [-p port] [-a] [-u user1;pass1,...usern;passn]" //
			+ "\tt timeout : Seconds to wait for an ack to a message with QoS > 0. Defaults to 15." //
			+ "\tp port : Port to listen on. Defaults to 1883." //
			+ "\ta : Allow anonymous access. Allows clients to connect with no credentials." //
			+ "\tu user:pass... : Credentials (usernames and passwords) a client can use to connet." //
	;

	private MockBroker broker;

	/**
	 * @see net.sf.xenqtt.XenqttApplication#start(net.sf.xenqtt.ApplicationArguments)
	 */
	@Override
	public void start(ApplicationArguments arguments) {

		int timeout = arguments.getArgAsInt("t", 15);
		int port = arguments.getArgAsInt("p", 1883);
		boolean allowAnonymousAccess = arguments.isFlagSpecified("a");

		broker = new MockBroker(null, timeout, port, allowAnonymousAccess);

		String credentials = arguments.getArgAsString("u", "");
		for (String creds : XenqttUtil.quickSplit(credentials, ',')) {
			String[] userpass = XenqttUtil.quickSplit(creds, ';');
			if (userpass.length != 2) {
				throw new IllegalArgumentException("Credentials could not be parsed: " + credentials);
			}
			broker.addCredentials(userpass[0], userpass[1]);
		}

		broker.init();
	}

	/**
	 * @see net.sf.xenqtt.XenqttApplication#stop()
	 */
	@Override
	public void stop() {

		broker.shutdown(15000);
	}

	/**
	 * @see net.sf.xenqtt.XenqttApplication#getUsageText()
	 */
	@Override
	public String getUsageText() {
		return USAGE_TEXT;
	}
}
