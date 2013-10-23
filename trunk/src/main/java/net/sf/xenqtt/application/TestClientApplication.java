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
import net.sf.xenqtt.test.XenqttTestClient;

/**
 * Runs a test client application that can be used in load testing the Xenqtt client and the disparate applications embedded within.
 */
public final class TestClientApplication extends AbstractXenqttApplication {

	private XenqttTestClient testClient;

	/**
	 * @see net.sf.xenqtt.application.XenqttApplication#start(net.sf.xenqtt.AppContext)
	 */
	@Override
	public void start(AppContext arguments) {
		testClient = new XenqttTestClient(arguments);
		testClient.start();
	}

	/**
	 * @see net.sf.xenqtt.application.XenqttApplication#stop()
	 */
	@Override
	public void stop() {
		if (testClient != null) {
			testClient.stop();
		}
	}

	/**
	 * @see net.sf.xenqtt.application.XenqttApplication#getOptsText()
	 */
	@Override
	public String getOptsText() {
		return "[-c clientType] [-b brokerUri] [-u user;pass] [-s subscribeTopic] [-d publishTopic] [-p numPublishers] [-m messagesToPublish] [-r messagesToReceive] [-t duration]";
	}

	/**
	 * @see net.sf.xenqtt.application.XenqttApplication#getOptsUsageText()
	 */
	@Override
	public String getOptsUsageText() {
		return "\n\t-c : The client type to use. Can be sync or async" //
				+ "\n\t-b : The broker URI. Format: tcp://host:port" //
				+ "\n\t-u : Specifies the username and password to use. If omitted anonymous access is assumed" //
				+ "\n\t-s : The topic to subscribe to. Can be a topic filter" //
				+ "\n\t-d : The topic to publish to. Must be a standard topic" //
				+ "\n\t-p : The number of publishers to use. This is not the number of MQTT clients" //
				+ "\n\t-m : The number of messages to publish during the test per-publisher. Can be zero" //
				+ "\n\t-r : The number of messages to receive during the test" //
				+ "\n\t-t : The duration of the test. Format: hh:mm:ss.SSS" //
		;
	}

	/**
	 * @see net.sf.xenqtt.application.XenqttApplication#getSummary()
	 */
	@Override
	public String getSummary() {
		return "Runs the test client. Useful for load testing brokers.";
	}

	/**
	 * @see net.sf.xenqtt.application.XenqttApplication#getDescription()
	 */
	@Override
	public String getDescription() {
		return getSummary();
	}
}
