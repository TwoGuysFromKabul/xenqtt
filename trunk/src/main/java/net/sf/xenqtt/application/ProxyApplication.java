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
import net.sf.xenqtt.proxy.ProxyBroker;

/**
 * A {@link XenqttApplication} is that acts as an MQTT proxy to allow a cluster of servers to act as a single MQTT proxy. All connections to this proxy that
 * have the same client ID will share a connection to a broker and appear as a single client to it.
 */
public final class ProxyApplication extends AbstractXenqttApplication {

	private ProxyBroker broker;

	/**
	 * @see net.sf.xenqtt.application.XenqttApplication#start(net.sf.xenqtt.AppContext)
	 */
	@Override
	public void start(AppContext arguments) {

		String brokerUri = arguments.getArgAsString("b");
		int port = arguments.getArgAsInt("p", 1883);

		broker = new ProxyBroker(brokerUri, port);
		broker.init();
	}

	/**
	 * @see net.sf.xenqtt.application.XenqttApplication#stop()
	 */
	@Override
	public void stop() {

		if (broker != null) {
			broker.shutdown(15000);
		}
	}

	/**
	 * @return The URI to connect to the proxy. Not valid until after {@link #start(AppContext)} is called.
	 */
	public String getProxyURI() {
		return broker.getURI();
	}

	/**
	 * @see net.sf.xenqtt.application.XenqttApplication#getOptsText()
	 */
	@Override
	public String getOptsText() {
		return "-b brokerUri [-p port]";
	}

	/**
	 * @see net.sf.xenqtt.application.XenqttApplication#getOptsUsageText()
	 */
	@Override
	public String getOptsUsageText() {
		return "\n\tb brokerUri : URI of the broker to connect to. For example: tcp://q.m2m.io:1883. Required." //
				+ "\n\tp port : Port to listen on. Defaults to 1883." //
		;
	}

	/**
	 * @see net.sf.xenqtt.application.XenqttApplication#getSummary()
	 */
	@Override
	public String getSummary() {
		return "Run the clustered proxy for supporting multiple applications as a single client.";
	}

	/**
	 * @see net.sf.xenqtt.application.XenqttApplication#getDescription()
	 */
	@Override
	public String getDescription() {
		return "The proxy allows for clustering one or more applications and treating them as a single client. The "//
				+ "proxy manages traffic between the clients and an MQTT broker and exposes all the applications as a "//
				+ "single client to the broker. In this way clustered systems that want to receive data once and only "//
				+ "once can connect through the proxy and the proxy will deliver messages to just one client rather "//
				+ "than all of them.";
	}
}
