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
package net.xenqtt.application;

import net.xenqtt.AppContext;
import net.xenqtt.XenqttUtil;

public final class LicenseApplication extends AbstractXenqttApplication {

	/**
	 * @see net.xenqtt.application.XenqttApplication#start(net.xenqtt.AppContext)
	 */
	@Override
	public void start(AppContext appContext) {

		String license = XenqttUtil.loadResourceFile("/LICENSE.txt");
		if (license == null) {
			System.err.println("Unable to load the license file. This is a bug!");
			return;
		}

		System.out.println(license);

		appContext.applicationDone();
	}

	/**
	 * @see net.xenqtt.application.XenqttApplication#stop()
	 */
	@Override
	public void stop() {
		// ignore

	}

	/**
	 * @see net.xenqtt.application.XenqttApplication#getOptsText()
	 */
	@Override
	public String getOptsText() {
		return "";
	}

	/**
	 * @see net.xenqtt.application.XenqttApplication#getOptsUsageText()
	 */
	@Override
	public String getOptsUsageText() {
		return "";
	}

	/**
	 * @see net.xenqtt.application.XenqttApplication#getSummary()
	 */
	@Override
	public String getSummary() {
		return "Displays the XenQTT license";
	}

	/**
	 * @see net.xenqtt.application.XenqttApplication#getDescription()
	 */
	@Override
	public String getDescription() {
		return getSummary();
	}
}
