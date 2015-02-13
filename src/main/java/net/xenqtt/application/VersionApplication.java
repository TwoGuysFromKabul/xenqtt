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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import net.xenqtt.AppContext;

public final class VersionApplication extends AbstractXenqttApplication {

	/**
	 * @see net.xenqtt.application.XenqttApplication#start(net.xenqtt.AppContext)
	 */
	@Override
	public void start(AppContext appContext) {

		String pomPropsPath = "/META-INF/maven/net.sf.xenqtt/xenqtt/pom.properties";

		InputStream in = null;

		try {
			in = VersionApplication.class.getResourceAsStream(pomPropsPath);
			Properties props = new Properties();
			props.load(in);
			String version = props.getProperty("version");
			System.out.println("XenQTT version: " + version);
		} catch (Exception e) {
			throw new RuntimeException("Failed to open stream to resource: " + pomPropsPath, e);
		} finally {
			try {
				in.close();
			} catch (IOException ignore) {
			}
		}

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
		return "Displays the XenQTT version";
	}

	/**
	 * @see net.xenqtt.application.XenqttApplication#getDescription()
	 */
	@Override
	public String getDescription() {
		return getSummary();
	}
}
