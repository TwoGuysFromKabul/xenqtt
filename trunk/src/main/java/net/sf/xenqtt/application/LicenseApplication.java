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

import java.io.InputStream;

import net.sf.xenqtt.AppContext;
import net.sf.xenqtt.Xenqtt;

public final class LicenseApplication extends AbstractXenqttApplication {

	/**
	 * @see net.sf.xenqtt.application.XenqttApplication#start(net.sf.xenqtt.AppContext)
	 */
	@Override
	public void start(AppContext appContext) {

		String license = loadResourceFile("/LICENSE.txt");
		if (license == null) {
			System.err.println("Unable to load the license file. This is a bug!");
			return;
		}

		System.out.println(license);

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
		return "";
	}

	/**
	 * @see net.sf.xenqtt.application.XenqttApplication#getOptsUsageText()
	 */
	@Override
	public String getOptsUsageText() {
		return "";
	}

	/**
	 * @see net.sf.xenqtt.application.XenqttApplication#getSummary()
	 */
	@Override
	public String getSummary() {
		return "Displays the XenQTT license";
	}

	/**
	 * @see net.sf.xenqtt.application.XenqttApplication#getDescription()
	 */
	@Override
	public String getDescription() {
		return getSummary();
	}

	private String loadResourceFile(String resourceName) {
		resourceName = resourceName.charAt(0) == '/' ? resourceName : String.format("/%s", resourceName);
		InputStream in = Xenqtt.class.getResourceAsStream(resourceName);
		if (in == null) {
			System.err.println("Unable to load the requested resource. This is a bug!");
			return null;
		}

		StringBuilder resource = new StringBuilder();
		byte[] buffer = new byte[8192];
		int bytesRead = -1;
		try {
			while ((bytesRead = in.read(buffer)) != -1) {
				resource.append(new String(buffer, 0, bytesRead));
			}
			in.close();
		} catch (Exception ex) {
			System.err.println("Unable to load the help documentation. This is a bug!");
			ex.printStackTrace();
			return null;
		}

		return resource.toString();
	}
}
