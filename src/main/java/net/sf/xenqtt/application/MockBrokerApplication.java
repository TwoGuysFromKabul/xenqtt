/**
    Copyright 2013 James McClure

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package net.sf.xenqtt.application;

import net.sf.xenqtt.AppContext;
import net.sf.xenqtt.XenqttUtil;
import net.sf.xenqtt.mockbroker.MockBroker;

/**
 * Runs the {@link MockBroker} as a xenqtt command line application
 */
public final class MockBrokerApplication extends AbstractXenqttApplication {

	private MockBroker broker;

	/**
	 * @see net.sf.xenqtt.application.XenqttApplication#start(net.sf.xenqtt.AppContext)
	 */
	@Override
	public void start(AppContext arguments) {

		int timeout = arguments.getArgAsInt("t", 15);
		int port = arguments.getArgAsInt("p", 1883);
		int maxInFlightMessages = arguments.getArgAsInt("m", 50);
		boolean allowAnonymousAccess = arguments.isFlagSpecified("a");
		boolean ignoreCredentials = arguments.isFlagSpecified("i");

		broker = new MockBroker(null, timeout, port, allowAnonymousAccess, ignoreCredentials, false, maxInFlightMessages);

		String credentials = arguments.getArgAsString("u", "");
		for (String creds : XenqttUtil.quickSplit(credentials, ',')) {
			String[] userpass = XenqttUtil.quickSplit(creds, ':');
			if (userpass.length != 2) {
				throw new IllegalArgumentException("Credentials could not be parsed: " + credentials);
			}
			broker.addCredentials(userpass[0], userpass[1]);
		}

		broker.init();
	}

	/**
	 * @see net.sf.xenqtt.application.XenqttApplication#stop()
	 */
	@Override
	public void stop() {

		broker.shutdown(15000);
	}

	/**
	 * @see net.sf.xenqtt.application.XenqttApplication#getOptsText()
	 */
	@Override
	public String getOptsText() {
		return "[-t timeout] [-p port] [-a] [-i] [-u user1:pass1,...usern:passn]";
	}

	/**
	 * @see net.sf.xenqtt.application.XenqttApplication#getOptsUsageText()
	 */
	@Override
	public String getOptsUsageText() {
		return "\n\t-t timeout : Seconds to wait for an ack to a message with QoS > 0. Defaults to 15." //
				+ "\n\t-p port : Port to listen on. Defaults to 1883." //
				+ "\n\t-a : Allow anonymous access. Allows clients to connect with no credentials. This does not ignore" //
				+ "\n       credentials that are specified in the connect message. Specify -i for that feature." //
				+ "\n\t-i : Ignore credentials. Allows clients to connect with any credentials. This does not allow " //
				+ "\n       access when the connect message contains no credentials. Specify -a for that feature." //
				+ "\n\t-m : Max in-flight messages to a client. Defaults to 50." //
				+ "\n\t-u user:pass... : Credentials (usernames and passwords) a client can use to connet." //
		;
	}

	/**
	 * @see net.sf.xenqtt.application.XenqttApplication#getSummary()
	 */
	@Override
	public String getSummary() {
		return "Run a mock MQTT broker. Useful in testing and debugging.";
	}

	/**
	 * @see net.sf.xenqtt.application.XenqttApplication#getDescription()
	 */
	@Override
	public String getDescription() {
		return "The mock broker is an MQTT broker that facilitates communication between disparate clients. " //
				+ "The mock broker supports the MQTT protocol out-of-the-box (save for QoS 2 which is not presently supported). " //
				+ "In addition to full protocol support the mock broker also provides hooks for users to customize the behavior "//
				+ "of various operations. These include publishing, subscribing, and so forth.";
	}
}
