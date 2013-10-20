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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;

import net.sf.xenqtt.ArgumentExtractor.Arguments;
import net.sf.xenqtt.ArgumentExtractor.Mode;
import net.sf.xenqtt.application.AbstractXenqttApplication;
import net.sf.xenqtt.application.XenqttApplication;

/**
 * The entry point into the application when either the proxy or the gateway are run.
 */
public final class Xenqtt {

	private static Map<String, XenqttApplication> APPS_BY_NAME;

	private static final String JAVA_OPTS_TEXT = "[java.opts]]";
	private static final String JAVA_OPTS_USAGE_TEXT = "\n\tjava.opts : Arguments to the JVM (-Xmx, -Xms, -server, etc)";
	private static final String GLOBAL_OPTS_TEXT = "[-v[v]]";
	private static final String GLOBAL_OPTS_USAGE_TEXT = "\n\t-v : Increase logging verbosity. v = info, vv = debug";

	static volatile LoggingLevels loggingLevels = new LoggingLevels(LoggingLevels.DEFAULT_LOGGING_LEVELS);
	static volatile String outputFile;

	static {
		System.setProperty("xenqtt.logging.async", "true");
	}

	private static final CountDownLatch shutdownLatch = new CountDownLatch(1);
	private static XenqttApplication application;

	/**
	 * The entry point into Xenqtt.
	 * 
	 * @param args
	 *            <p>
	 *            The arguments specified by the user at the command-line. You can supply global logging flags ({@code -v}, {@code -vv}, or {@code -v -v}) and a
	 *            mode to run Xenqtt in. The supported modes, at present, are:
	 *            </p>
	 * 
	 *            <ul>
	 *            <li>{@code proxy}</li>
	 *            <li>{@code gateway}</li>
	 *            <li>{@code mockbroker}</li>
	 *            <li>{@code testclient}</li>
	 *            <li>{@code help}</li>
	 *            </ul>
	 * 
	 *            <p>
	 *            To get mode-specific help information use the {@code help} mode and then specify the mode name (e.g. {@code help proxy})
	 *            </p>
	 */
	public static void main(String... args) {

		APPS_BY_NAME = Collections.unmodifiableMap(loadXenqttApplications());

		Arguments arguments = ArgumentExtractor.extractArguments(shutdownLatch, args);
		if (arguments == null) {
			XenqttUtil.prettyPrintln(getFullUsageText(), false);
			return;
		}

		loggingLevels = arguments.determineLoggingLevels();
		outputFile = String.format("xenqtt-%s.log", arguments.mode.getMode().toLowerCase());
		application = APPS_BY_NAME.get(arguments.mode.getMode().toLowerCase());

		Runtime.getRuntime().addShutdownHook(new Thread() {

			@Override
			public void run() {
				if (application != null) {
					application.stop();
				}

				shutdownLatch.countDown();
			}
		});

		try {
			runApplication(arguments.mode, arguments.applicationArguments);
			shutdownLatch.await();
		} catch (Exception ex) {
			System.err.printf("Unable to launch the application. Details: %s\n", ex.getMessage());
			ex.printStackTrace();
			Class<?> exceptionClass = ex.getClass();
			if (exceptionClass == IllegalArgumentException.class || exceptionClass == IllegalStateException.class) {
				XenqttUtil.prettyPrintln("\nUSAGE: " + getAppSpecificUsageText(application), true);
			}
		} finally {
			Log.shutdown();
		}
	}

	/**
	 * @return All {@link XenqttApplication applications}
	 */
	public static Collection<XenqttApplication> getApplications() {
		return APPS_BY_NAME.values();
	}

	/**
	 * @return The {@link XenqttApplication application} with the specified name (case insensitive). Null if there is no app with the specified name.
	 */
	public static XenqttApplication getApplication(String appName) {

		return APPS_BY_NAME.get(appName.toLowerCase());
	}

	/**
	 * @return The usage text for a specific application
	 */
	public static String getAppSpecificUsageText(XenqttApplication application) {
		StringBuilder usage = new StringBuilder();
		usage.append("usage: java ");
		usage.append(JAVA_OPTS_TEXT);
		usage.append(" -jar xenqtt-version.jar ");
		usage.append(GLOBAL_OPTS_TEXT);
		usage.append(" ");
		usage.append(application.getName());
		usage.append(" ");
		usage.append(application.getOptsText());
		usage.append(JAVA_OPTS_USAGE_TEXT);
		usage.append(GLOBAL_OPTS_USAGE_TEXT);
		usage.append(application.getOptsUsageText());
		return usage.toString();
	}

	/**
	 * @return The full usage text for xenqtt
	 */
	public static String getFullUsageText() {

		StringBuilder appList = new StringBuilder();
		for (String appName : APPS_BY_NAME.keySet()) {
			if (appList.length() > 0) {
				appList.append('|');
			}
			appList.append(appName);
		}

		StringBuilder usage = new StringBuilder();
		usage.append("usage: java ");
		usage.append(JAVA_OPTS_TEXT);
		usage.append(" -jar xenqtt-version.jar ");
		usage.append(GLOBAL_OPTS_TEXT);
		usage.append(" ");
		usage.append(appList);
		usage.append(" [app.opts]\n");
		usage.append(JAVA_OPTS_USAGE_TEXT);
		usage.append(GLOBAL_OPTS_USAGE_TEXT);
		for (Map.Entry<String, XenqttApplication> entry : APPS_BY_NAME.entrySet()) {
			usage.append(String.format("\n\t%s : %s", entry.getKey(), entry.getValue().getSummary()));
		}
		usage.append("\n\tapp.opts : Application specific options");
		return usage.toString();
	}

	private static Map<String, XenqttApplication> loadXenqttApplications() {

		try {
			ClassLoader classLoader = Xenqtt.class.getClassLoader();
			Map<String, XenqttApplication> apps = new TreeMap<String, XenqttApplication>();
			for (String appClassName : getAppClassNames()) {
				Class<?> clazz = classLoader.loadClass(appClassName);
				XenqttApplication app = (XenqttApplication) clazz.newInstance();
				apps.put(app.getName(), app);
			}

			return apps;
		} catch (Exception ex) {
			throw new RuntimeException("Unable to instantiate XenQTT applications.", ex);
		}
	}

	private static List<String> getAppClassNames() throws Exception {

		List<String> classFiles = XenqttUtil.findFilesOnClassPath(XenqttApplication.class.getPackage().getName(), ".class");

		List<String> classNames = new ArrayList<String>(classFiles.size());

		for (String classFile : classFiles) {
			String className = classFile.replace('/', '.').substring(0, classFile.length() - ".class".length());
			if (!XenqttApplication.class.getName().equals(className) && !AbstractXenqttApplication.class.getName().equals(className)) {
				classNames.add(className);
			}
		}

		return classNames;
	}

	private static void runApplication(Mode mode, AppContext applicationArguments) {

		if (application == null) {
			Log.info("The following mode is not presently supported: %s", mode.getMode());
			return;
		}

		Log.info("Starting the following application: %s", application.getClass().getSimpleName());
		application.start(applicationArguments);
	}
}
