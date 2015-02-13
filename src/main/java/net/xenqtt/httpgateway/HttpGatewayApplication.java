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
package net.xenqtt.httpgateway;

import net.xenqtt.AppContext;
import net.xenqtt.application.AbstractXenqttApplication;

import org.eclipse.jetty.server.Server;

/**
 * Runs an HTTP gateway as a XenQTT command line application
 */
public class HttpGatewayApplication extends AbstractXenqttApplication {

	private Server server;

	/**
	 * @see net.xenqtt.application.XenqttApplication#start(net.xenqtt.AppContext)
	 */
	@Override
	public void start(AppContext appContext) throws Exception {

		int port = appContext.getArgAsInt("p", 1880);
		server = new Server(port);

		HttpGatewayHandler gatewayHandler = new HttpGatewayHandler();
		server.setHandler(gatewayHandler);

		server.start();
	}

	/**
	 * @see net.xenqtt.application.XenqttApplication#stop()
	 */
	@Override
	public void stop() throws Exception {

		if (server != null) {
			server.stop();
			server.join();
		}
	}

	/**
	 * @see net.xenqtt.application.XenqttApplication#getOptsText()
	 */
	@Override
	public String getOptsText() {
		return "[-p port]";
	}

	/**
	 * @see net.xenqtt.application.XenqttApplication#getOptsUsageText()
	 */
	@Override
	public String getOptsUsageText() {
		return "\n\t-p port : Port to listen on. Defaults to 1880." //
		;
	}

	/**
	 * @see net.xenqtt.application.XenqttApplication#getSummary()
	 */
	@Override
	public String getSummary() {
		return "Run the HTTP gateway.";
	}

	/**
	 * @see net.xenqtt.application.XenqttApplication#getDescription()
	 */
	@Override
	public String getDescription() {
		// FIXME [jim] - update to detail services
		return "The HTTP gateway allows communication with an MQTT broker via RESTful HTTP services. There are " //
				+ "services for connecting, subscribing, unsubscribing, and publishing (QoS 0 and QoS 1) messages."//
		;
	}

}
