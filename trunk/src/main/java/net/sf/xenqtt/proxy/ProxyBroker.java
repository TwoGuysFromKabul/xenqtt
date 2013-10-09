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

import net.sf.xenqtt.SimpleBroker;

public final class ProxyBroker extends SimpleBroker {

	private final String brokerUri;

	/**
	 * @param port
	 *            The port for the server to listen on. 0 will choose an arbitrary available port which you can get from {@link #getPort()} after calling
	 *            {@link #init()}.
	 */
	public ProxyBroker(String brokerUri, int port) {
		super(0, port);
		this.brokerUri = brokerUri;
	}

	/**
	 * Initializes the broker
	 */
	public void init() {

		ServerMessageHandler serverMessageHandler = new ServerMessageHandler(brokerUri, manager);
		super.init(serverMessageHandler, "ProxyServer");
	}
}
