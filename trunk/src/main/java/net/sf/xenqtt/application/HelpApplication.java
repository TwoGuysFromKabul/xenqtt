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
import net.sf.xenqtt.Xenqtt;
import net.sf.xenqtt.XenqttUtil;

/**
 * Displays help text
 */
public final class HelpApplication extends AbstractXenqttApplication {

	/**
	 * @see net.sf.xenqtt.application.XenqttApplication#start(net.sf.xenqtt.AppContext)
	 */
	@Override
	public void start(AppContext appContext) {

		String desiredHelpMode = appContext.getArgAsString("-m", null);
		if (desiredHelpMode == null) {
			displayGeneralHelpInformation();
		} else {
			displayApplicationSpecificHelpInformation(desiredHelpMode);
		}

		appContext.applicationDone();
	}

	/**
	 * @see net.sf.xenqtt.application.XenqttApplication#stop()
	 */
	@Override
	public void stop() {
		// ignore
	}

	/**
	 * @see net.sf.xenqtt.application.XenqttApplication#getOptsText()
	 */
	@Override
	public String getOptsText() {
		return "app";
	}

	/**
	 * @see net.sf.xenqtt.application.XenqttApplication#getOptsUsageText()
	 */
	@Override
	public String getOptsUsageText() {
		return "\n\tapp : Application to display help for";
	}

	/**
	 * @see net.sf.xenqtt.application.XenqttApplication#getSummary()
	 */
	@Override
	public String getSummary() {
		return "Displays help information.";
	}

	/**
	 * @see net.sf.xenqtt.application.XenqttApplication#getDescription()
	 */
	@Override
	public String getDescription() {
		return getSummary();
	}

	private void displayGeneralHelpInformation() {

		XenqttUtil.prettyPrintln(Xenqtt.getFullUsageText(), false);

		for (XenqttApplication application : Xenqtt.getApplications()) {
			System.out.println("----------------------------------------------------------------------------------------------------");
			System.out.printf("%s:\n", application.getName());
			displayApplicationSpecificHelpInformation(application);
		}
	}

	private void displayApplicationSpecificHelpInformation(String desiredHelpMode) {

		XenqttApplication application = Xenqtt.getApplication(desiredHelpMode);
		if (application == null) {
			System.out.println("Unrecognized application: " + desiredHelpMode);
			return;
		}

		displayApplicationSpecificHelpInformation(application);
	}

	private void displayApplicationSpecificHelpInformation(XenqttApplication application) {

		XenqttUtil.prettyPrintln(application.getDescription(), true);
		System.out.println();
		XenqttUtil.prettyPrintln(Xenqtt.getAppSpecificUsageText(application), true);
	}
}
