package net.sf.xenqtt;

import java.io.InputStream;

/**
 * The entry point into the application when either the proxy or the gateway are run.
 */
public final class Xenqtt {

	private static final String USAGE = "usage: java -jar xenqtt.jar [-v[v]] proxy|gateway|help\n\tproxy - Run the MQTT proxy for clustered clients\n\t"
			+ "gateway - Run the MQTT gateway that facilitates HTTP <-> MQTT communication\n\thelp - Display information on xenqtt and how it can be used\n\t"
			+ "-v: Increase logging verbosity. v = info, vv = debug";
	private static final String ARG_REGEX = "^\\-(?i:v){1,2}$";

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
	public static void main(String... args) {
		Arguments arguments = extractArguments(args);
		if (arguments == null) {
			System.out.println(USAGE);

			return;
		}

		Log.setLoggingLevels(arguments.levels);
		runInMode(arguments.mode);
	}

	private static Arguments extractArguments(String[] args) {
		if (args == null || args.length == 0) {
			return null;
		}

		try {
			String mode = extractMode(args);
			if (mode == null) {
				return null;
			}

			int loggingLevels = getLoggingLevels(args);
			return new Arguments(Mode.lookup(mode), new LoggingLevels(loggingLevels));
		} catch (Exception ex) {
			return null;
		}
	}

	private static String extractMode(String[] args) {
		for (String arg : args) {
			if (arg.indexOf('-') == -1) {
				return arg;
			}
		}

		return null;
	}

	private static int getLoggingLevels(String[] args) {
		int loggingLevels = 0x38;
		if (args.length > 1) {
			for (int i = 0; i < args.length; i++) {
				String arg = args[i];
				if (!arg.matches(ARG_REGEX) && !Mode.isValidMode(arg)) {
					throw new IllegalArgumentException(String.format("Invalid command-line option: %s", arg));
				}

				int toShift = arg.length() - 1;
				for (int j = 0; j < toShift; j++) {
					loggingLevels >>= 1;
					loggingLevels |= 0x20;
				}

			}
		}

		return loggingLevels;
	}

	private static void runInMode(Mode mode) {
		switch (mode) {
		case HELP:
			displayHelpInformation();
			return;
		default:
			Log.info("The following mode is not presently supported: %s", mode.mode);
		}

		try {
			Thread.currentThread().join();
		} catch (InterruptedException ignore) {
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

	private static enum Mode {

		PROXY("proxy"), GATEWAY("gateway"), HELP("help");

		private final String mode;

		private Mode(String mode) {
			this.mode = mode;
		}

		private static Mode lookup(String mode) {
			for (Mode m : values()) {
				if (m.mode.equalsIgnoreCase(mode)) {
					return m;
				}
			}

			throw new IllegalArgumentException(String.format("Invalid mode specified: %s", mode));
		}

		private static boolean isValidMode(String mode) {
			for (Mode m : values()) {
				if (m.mode.equalsIgnoreCase(mode)) {
					return true;
				}
			}

			return false;
		}

	}

	private static final class Arguments {

		private final Mode mode;
		private final LoggingLevels levels;

		private Arguments(Mode mode, LoggingLevels levels) {
			this.mode = mode;
			this.levels = levels;
		}

	}

}
