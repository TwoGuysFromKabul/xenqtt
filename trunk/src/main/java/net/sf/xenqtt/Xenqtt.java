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

	private static final Class<?>[] APPLICATIONS = new Class<?>[] { MockBrokerApplication.class };
	private static final String USAGE = "usage: java -jar xenqtt.jar [-v[v]] proxy|gateway|mockbroker|help [args.or.flags]"
			+ "\n\tproxy - Run the MQTT proxy for clustered clients\n\tgateway - Run the MQTT gateway that facilitates HTTP <-> MQTT communication"
			+ "\n\tmockbroker - Run a mock MQTT broker. Useful in testing and debugging\n\thelp - Display information on xenqtt and how it can be used"
			+ "\n\n\t-v: Increase logging verbosity. v = info, vv = debug";

	static {
		System.setProperty("xenqtt.logging.async", "true");
	}

	private static final CountDownLatch shutdownLatch = new CountDownLatch(1);
	private static XenqttApplication application;

	/**
	 * The entry point into Xenqtt.
	 * 
	 * @param args
	 *            The arguments specified by the user at the command-line. The valid arguments include the following:
	 * 
	 *            <table cellspacing="0" cellpadding="0" border="0" style="margin-top: 1em; border: 1px solid rgb(0, 0, 0);">
	 *            <tr>
	 *            <td style="border-right: 1px solid rgb(0, 0, 0); font-weight: bold; padding: 0.2em;">Argument</td>
	 *            <td style="border-right: 1px solid rgb(0, 0, 0); font-weight: bold; padding: 0.2em;">Description</td>
	 *            <td style="font-weight: bold; padding: 0.2em;">Valid Values</td>
	 *            </tr>
	 *            <tr>
	 *            <td style="border-top: 1px solid rgb(0, 0, 0); border-right: 1px solid rgb(0, 0, 0); padding: 0.2em;">{mode}</td>
	 *            <td style="border-top: 1px solid rgb(0, 0, 0); border-right: 1px solid rgb(0, 0, 0); padding: 0.2em;">Defines the mode in which Xenqtt should
	 *            run</td>
	 *            <td style="border-top: 1px solid rgb(0, 0, 0); padding: 0.2em;">
	 *            <ul>
	 *            <li>proxy</li>
	 *            <li>gateway</li>
	 *            <li>mockbroker</li>
	 *            <li>help</li>
	 *            </ul>
	 *            </td>
	 *            </tr>
	 *            <tr>
	 *            <td style="border-top: 1px solid rgb(0, 0, 0); border-right: 1px solid rgb(0, 0, 0); padding: 0.2em;">-v</td>
	 *            <td style="border-top: 1px solid rgb(0, 0, 0); border-right: 1px solid rgb(0, 0, 0); padding: 0.2em;">Switch that specifies verbose logging at
	 *            the INFO level</td>
	 *            <td style="border-top: 1px solid rgb(0, 0, 0); padding: 0.2em;">N/A</td>
	 *            </tr>
	 *            <tr>
	 *            <td style="border-top: 1px solid rgb(0, 0, 0); border-right: 1px solid rgb(0, 0, 0); padding: 0.2em;">-vv</td>
	 *            <td style="border-top: 1px solid rgb(0, 0, 0); border-right: 1px solid rgb(0, 0, 0); padding: 0.2em;">Switch that specifies verbose logging at
	 *            the DEBUG level</td>
	 *            <td style="border-top: 1px solid rgb(0, 0, 0); padding: 0.2em;">N/A</td>
	 *            </tr>
	 *            </table>
	 */
	public static void main(String... args) throws InterruptedException {
		Arguments arguments = ArgumentExtractor.extractArguments(args);
		if (arguments == null) {
			System.out.println(USAGE);

			return;
		}

		Log.setLoggingLevels(arguments.determineLoggingLevels());
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
				System.out.printf("USAGE: %s\n", application.getUsageText());
			}
		}
	}

	private static void displayHelpInformation() {
		InputStream in = Xenqtt.class.getResourceAsStream("/help-documentation.txt");
		if (in == null) {
			System.err.println("Unable to load the help documentation. This is a bug!");
			return;
		}

		StringBuilder helpDocumentation = new StringBuilder();
		byte[] buffer = new byte[8192];
		int bytesRead = -1;
		try {
			while ((bytesRead = in.read(buffer)) != -1) {
				helpDocumentation.append(new String(buffer, 0, bytesRead));
			}
			in.close();
		} catch (Exception ex) {
			System.err.println("Unable to load the help documentation. This is a bug!");
			ex.printStackTrace();
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

}
