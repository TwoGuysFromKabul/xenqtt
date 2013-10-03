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
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * A {@link LoggingDelegate} that logs messages and related data to a log file called {@code xenqtt.log} that is created and rolled in the directory where the
 * {@code xenqtt.jar} file is being executed. The logging is done via the Java Util Logging API.
 */
public class JavaLoggingDelegate implements LoggingDelegate {

	static {
		if (!isJavaUtilLoggingEnabled()) {
			initializeLogging();
		}
	}

	private static boolean isJavaUtilLoggingEnabled() {
		return System.getProperty("java.util.logging.config.file") != null || System.getProperty("java.util.logging.config.class") != null;
	}

	private static void initializeLogging() {
		LogManager logManager = LogManager.getLogManager();
		try {
			Properties properties = new Properties();
			properties.setProperty("handlers", "java.util.logging.FileHandler");
			properties.setProperty(".level", "ALL"); // Actual logging levels will be controlled by the Log class.
			properties.setProperty("java.util.logging.FileHandler.limit", "5368709120");
			properties.setProperty("java.util.logging.FileHandler.count", "20");
			properties.setProperty("java.util.logging.FileHandler.formatter", "net.sf.xenqtt.XenqttLogFormatter");
			String jarDirectory = XenqttUtil.getDirectoryHostingRunningXenqttJar();
			String outputFile = Xenqtt.outputFile != null ? Xenqtt.outputFile : "xenqtt.log";
			if (jarDirectory != null) {
				properties.setProperty("java.util.logging.FileHandler.pattern", String.format("%s/%s", jarDirectory, outputFile));
			} else {
				properties.setProperty("java.util.logging.FileHandler.pattern", "%h/" + outputFile);
			}
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			properties.store(baos, null);

			ByteArrayInputStream bain = new ByteArrayInputStream(baos.toByteArray());
			logManager.readConfiguration(bain);
		} catch (Exception ex) {
			System.err.println("Unable to load the logging configuration for JUL.");
			ex.printStackTrace();
		}
	}

	private final Logger log = Logger.getLogger("xenqtt");

	/**
	 * @see net.sf.xenqtt.Log.LoggingDelegate#trace(java.lang.String)
	 */
	@Override
	public void trace(String message) {
		log.finer(message);
	}

	/**
	 * @see net.sf.xenqtt.Log.LoggingDelegate#trace(java.lang.Throwable, java.lang.String)
	 */
	@Override
	public void trace(Throwable t, String message) {
		log.log(Level.FINER, message, t);
	}

	/**
	 * @see net.sf.xenqtt.Log.LoggingDelegate#debug(java.lang.String)
	 */
	@Override
	public void debug(String message) {
		log.fine(message);
	}

	/**
	 * @see net.sf.xenqtt.Log.LoggingDelegate#debug(java.lang.Throwable, java.lang.String)
	 */
	@Override
	public void debug(Throwable t, String message) {
		log.log(Level.FINE, message, t);
	}

	/**
	 * @see net.sf.xenqtt.Log.LoggingDelegate#info(java.lang.String)
	 */
	@Override
	public void info(String message) {
		log.info(message);
	}

	/**
	 * @see net.sf.xenqtt.Log.LoggingDelegate#info(java.lang.Throwable, java.lang.String)
	 */
	@Override
	public void info(Throwable t, String message) {
		log.log(Level.INFO, message, t);
	}

	/**
	 * @see net.sf.xenqtt.Log.LoggingDelegate#warn(java.lang.String)
	 */
	@Override
	public void warn(String message) {
		log.warning(message);
	}

	/**
	 * @see net.sf.xenqtt.Log.LoggingDelegate#warn(java.lang.Throwable, java.lang.String)
	 */
	@Override
	public void warn(Throwable t, String message) {
		log.log(Level.WARNING, message, t);
	}

	/**
	 * @see net.sf.xenqtt.Log.LoggingDelegate#error(java.lang.String)
	 */
	@Override
	public void error(String message) {
		log.severe(message);
	}

	/**
	 * @see net.sf.xenqtt.Log.LoggingDelegate#error(java.lang.Throwable, java.lang.String)
	 */
	@Override
	public void error(Throwable t, String message) {
		log.log(Level.SEVERE, message, t);
	}

	/**
	 * @see net.sf.xenqtt.Log.LoggingDelegate#fatal(java.lang.String)
	 */
	@Override
	public void fatal(String message) {
		log.severe(message);
	}

	/**
	 * @see net.sf.xenqtt.Log.LoggingDelegate#fatal(java.lang.Throwable, java.lang.String)
	 */
	@Override
	public void fatal(Throwable t, String message) {
		log.log(Level.SEVERE, message, t);
	}

}
