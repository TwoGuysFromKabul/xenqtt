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
package net.sf.xenqtt.proxy;

import net.sf.xenqtt.AppContext;
import net.sf.xenqtt.XenqttApplication;

/**
 * A {@link XenqttApplication} is that acts as an MQTT proxy to allow a cluster of servers to act as a single MQTT proxy. All connections to this proxy that
 * have the same client ID will share a connection to a broker and appear as a single client to it.
 */
public final class ProxyApplication implements XenqttApplication {

	private static String USAGE_TEXT = "-b brokerUri [-p port]" //
			+ "\n\tb brokerUri : The URI of the broker to connect to. For example: tcp://q.m2m.io:1883. This is required." //
			+ "\n\tp port : Port to listen on. Defaults to 1883." //
	;

	private ProxyBroker broker;

	/**
	 * @see net.sf.xenqtt.XenqttApplication#start(net.sf.xenqtt.AppContext)
	 */
	@Override
	public void start(AppContext arguments) {

		String brokerUri = arguments.getArgAsString("b");
		int port = arguments.getArgAsInt("p", 1883);

		broker = new ProxyBroker(brokerUri, port);
		broker.init();
	}

	/**
	 * @see net.sf.xenqtt.XenqttApplication#stop()
	 */
	@Override
	public void stop() {

		if (broker != null) {
			broker.shutdown(15000);
		}
	}

	/**
	 * @see net.sf.xenqtt.XenqttApplication#getUsageText()
	 */
	@Override
	public String getUsageText() {
		return USAGE_TEXT;
	}
}
