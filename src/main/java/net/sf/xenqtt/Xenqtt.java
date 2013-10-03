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

import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

import net.sf.xenqtt.ArgumentExtractor.Arguments;
import net.sf.xenqtt.ArgumentExtractor.Mode;
import net.sf.xenqtt.mockbroker.MockBrokerApplication;

/**
 * The entry point into the application when either the proxy or the gateway are run.
 */
public final class Xenqtt {

	private static final Class<?>[] APPLICATIONS = new Class<?>[] { MockBrokerApplication.class, TestClientApplication.class };
	private static final String USAGE = "usage: java -jar xenqtt.jar [-v[v]] proxy|gateway|mockbroker|help [args.or.flags]"
			// + "\n\tproxy - Run the MQTT proxy for clustered clients" //
			// + "\n\tgateway - Run the MQTT gateway that facilitates HTTP <-> MQTT communication"
			+ "\n\tmockbroker - Run a mock MQTT broker. Useful in testing and debugging\n\thelp - Display information on xenqtt and how it can be used"
			+ "\n\n\t-v: Increase logging verbosity. v = info, vv = debug";

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
	public static void main(String... args) throws InterruptedException {
		Arguments arguments = ArgumentExtractor.extractArguments(args);
		if (arguments == null) {
			System.out.println(USAGE);

			return;
		}

		loggingLevels = arguments.determineLoggingLevels();
		outputFile = String.format("xenqtt-%s.log", arguments.mode.getMode().toLowerCase());
		application = loadXenqttApplication(arguments.mode);

		Runtime.getRuntime().addShutdownHook(new Thread() {

			@Override
			public void run() {
				if (application != null) {
					application.stop();
				}

				shutdownLatch.countDown();
			}

		});

		runApplication(arguments.mode, arguments.applicationArguments);
		shutdownLatch.await();
	}

	private static XenqttApplication loadXenqttApplication(Mode mode) {
		String modeName = mode.getMode().toLowerCase();
		for (Class<?> clazz : APPLICATIONS) {
			String className = clazz.getSimpleName().toLowerCase();
			if (className.startsWith(modeName)) {
				try {
					return (XenqttApplication) clazz.newInstance();
				} catch (Exception ex) {
					throw new RuntimeException(String.format("Unable to instantiate the Xenqtt application %s.", className));
				}
			}
		}

		return null;
	}

	private static void runApplication(Mode mode, ApplicationArguments applicationArguments) {
		if (mode == Mode.HELP) {
			displayHelpInformation();
			System.exit(0);
		}

		if (mode == Mode.LICENSE) {
			displayLicense();
			System.exit(0);
		}

		if (application == null) {
			Log.info("The following mode is not presently supported: %s", mode.getMode());
			System.exit(0);
		}

		try {
			Log.info("Starting the following application: %s", application.getClass().getSimpleName());
			application.start(applicationArguments);
		} catch (Exception ex) {
			System.err.printf("Unable to launch the application. Details: %s\n", ex.getMessage());
			ex.printStackTrace();
			Class<?> exceptionClass = ex.getClass();
			if (exceptionClass == IllegalArgumentException.class || exceptionClass == IllegalStateException.class) {
				System.out.printf("\nUSAGE: %s\n", application.getUsageText());
			}

			System.exit(0);
		}
	}

	private static void displayHelpInformation() {
		String helpDocumentation = loadResourceFile("/help-documentation.txt");
		if (helpDocumentation == null) {
			System.err.println("Unable to load the help documentation. This is a bug!");
			return;
		}

		System.out.println(wrap(helpDocumentation.toString()));
	}

	private static String wrap(String helpDocumentation) {
		StringBuilder wrappedHelpDocumentation = new StringBuilder();
		StringBuilder currentLine = new StringBuilder();
		int currentLineSize = 0;
		for (int i = 0; i < helpDocumentation.length(); i++) {
			char c = helpDocumentation.charAt(i);
			if (c != '\t') {
				currentLine.append(c);
				currentLineSize++;
			} else {
				currentLine.append("    ");
				currentLineSize += 4;
			}

			if (c == '\n') {
				wrappedHelpDocumentation.append(currentLine.toString());
				currentLine = new StringBuilder();
				currentLineSize = 0;
				continue;
			}

			if (currentLineSize > 100) {
				if (c == ' ') {
					wrappedHelpDocumentation.append(currentLine.toString());
					currentLine = new StringBuilder();
				} else {
					int lastWhitespace = currentLine.lastIndexOf(" ");
					String nextLine = currentLine.substring(lastWhitespace + 1);
					wrappedHelpDocumentation.append(currentLine.substring(0, lastWhitespace));
					currentLine = new StringBuilder(nextLine);
				}
				wrappedHelpDocumentation.append('\n');
				currentLineSize = 0;
			}
		}
		wrappedHelpDocumentation.append(currentLine.toString());

		return wrappedHelpDocumentation.toString();
	}

	private static void displayLicense() {
		String license = loadResourceFile("/LICENSE.txt");
		if (license == null) {
			System.err.println("Unable to load the license file. This is a bug!");
			return;
		}

		System.out.println(license);
	}

	private static String loadResourceFile(String resourceName) {
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
