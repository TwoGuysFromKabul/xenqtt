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
package net.sf.xenqtt.mockbroker;

import net.sf.xenqtt.ApplicationArguments;
import net.sf.xenqtt.XenqttApplication;
import net.sf.xenqtt.XenqttUtil;

/**
 * Runs the {@link MockBroker} as a xenqtt command line application
 */
public class MockBrokerApplication implements XenqttApplication {

	private static String USAGE_TEXT = "[-t timeout] [-p port] [-c] [-a] [-u user1:pass1,...usern:passn]" //
			+ "\n\tt timeout : Seconds to wait for an ack to a message with QoS > 0. Defaults to 15." //
			+ "\n\tp port : Port to listen on. Defaults to 1883." //
			+ "\n\tc : Capture broker events. If omitted none will be saved and made available." //
			+ "\n\ta : Allow anonymous access. Allows clients to connect with no credentials." //
			+ "\n\tu user:pass... : Credentials (usernames and passwords) a client can use to connet." //
	;

	private MockBroker broker;

	@Override
	public void start(ApplicationArguments arguments) {

		int timeout = arguments.getArgAsInt("t", 15);
		int port = arguments.getArgAsInt("p", 1883);
		boolean allowAnonymousAccess = arguments.isFlagSpecified("a");
		boolean captureBrokerEvents = arguments.isFlagSpecified("c");

		broker = new MockBroker(null, timeout, port, allowAnonymousAccess, captureBrokerEvents);

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
