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
package net.sf.xenqtt;

/**
 * Runs a test client application that can be used in load testing the Xenqtt client and the disparate applications embedded within.
 */
final class TestClientApplication implements XenqttApplication {

	private static final String USAGE_TEXT = "";

	/**
	 * @see net.sf.xenqtt.XenqttApplication#start(net.sf.xenqtt.ApplicationArguments)
	 */
	@Override
	public void start(ApplicationArguments arguments) {
	}

	/**
	 * @see net.sf.xenqtt.XenqttApplication#stop()
	 */
	@Override
	public void stop() {
	}

	/**
	 * @see net.sf.xenqtt.XenqttApplication#getUsageText()
	 */
	@Override
	public String getUsageText() {
		return USAGE_TEXT;
	}

}
