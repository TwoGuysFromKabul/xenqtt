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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.Charset;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * URL protocol handler for 'log4j:...' protocol. The actual URL is ignored by the handler. The log4j.xml and log4j.dtd files in the config package are put into
 * a 'virtual' jar that can then be added to the class path in a {@link URLClassLoader}.
 * <p>
 * String replacement will be performed on log4j.xml as follows:
 * <ul>
 * <li>${XENQTT_INSTALL_DIR}: The XenQTT install directory from {@link XenqttUtil#getXenqttInstallDirectory()}</li>
 * <li>${XENQTT_LOG_LEVEL}: The logging level from the {@link LoggingLevels} object passed to the constructor.</li>
 * <li>${XENQTT_APP_NAME}: The application name from the app name passed to the constructor.</li>
 * <li>${XENQTT_LOG_APPENDER_REF}: Placeholder for console logging. If the {@code -c} option is specified on the command-line logging goes to the console as
 * well as the file</li>
 * </ul>
 */
final class Log4jUrlStreamHandler extends URLStreamHandler {

	private static final Charset UTF8 = Charset.forName("UTF-8");

	// FIXME [jim] - update logging framework considering we now have log4j available. do we still need jul?
	private final byte[] bytes;

	public Log4jUrlStreamHandler(LoggingLevels loggingLevels, String appName, boolean consoleLogger) {

		try {
			String logLevel = loggingLevels.debugEnabled ? "debug" : loggingLevels.infoEnabled ? "info" : "warn";

			String dtd = XenqttUtil.loadResourceFile("config/log4j.dtd");
			String xml = XenqttUtil.loadResourceFile("config/log4j.xml");
			xml = xml.replace("${XENQTT_INSTALL_DIR}", XenqttUtil.getXenqttInstallDirectory().getAbsolutePath());
			xml = xml.replace("${XENQTT_LOG_LEVEL}", logLevel);
			xml = xml.replace("${XENQTT_APP_NAME}", appName);
			xml = consoleLogger ? xml.replace("${XENQTT_LOG_APPENDER_REF}", "console") : xml.replace("${XENQTT_LOG_APPENDER_REF}", "");

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			JarOutputStream out = new JarOutputStream(baos);
			addEntry(out, "log4j.dtd", dtd);
			addEntry(out, "log4j.xml", xml);
			out.close();
			bytes = baos.toByteArray();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Error creating log4j config. THIS IS A BUG!", e);
		}
	}

	private void addEntry(JarOutputStream out, String name, String content) throws Exception {

		out.putNextEntry(new JarEntry(name));
		InputStream in = new ByteArrayInputStream(content.getBytes(UTF8));
		for (int i = in.read(); i >= 0; i = in.read()) {
			out.write(i);
		}
		out.closeEntry();
	}

	/**
	 * @see java.net.URLStreamHandler#openConnection(java.net.URL)
	 */
	@Override
	protected URLConnection openConnection(URL u) throws IOException {

		return new URLConnection(u) {
			@Override
			public void connect() throws IOException {
			}

			@Override
			public InputStream getInputStream() throws IOException {

				return new ByteArrayInputStream(bytes);
			}
		};
	}
}
