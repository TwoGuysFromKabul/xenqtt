package net.sf.xenqtt;

import net.sf.xenqtt.util.Log;
import net.sf.xenqtt.util.LoggingLevels;

/**
 * The entry point into the application when either the proxy or the gateway are run.
 */
public final class Xenqtt {

	private static final String USAGE = "usage: java -jar xenqtt.jar proxy_or_gateway [-v[v]]\r\n\t-v: Increase logging verbosity. v = info, vv = debug";
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
	 *            <td style="border-top: 1px solid rgb(0, 0, 0); border-right: 1px solid rgb(0, 0, 0); padding: 0.2em;">proxy OR gateway</td>
	 *            <td style="border-top: 1px solid rgb(0, 0, 0); border-right: 1px solid rgb(0, 0, 0); padding: 0.2em;">Defines the mode in which Xenqtt should
	 *            run</td>
	 *            <td style="border-top: 1px solid rgb(0, 0, 0); padding: 0.2em;">
	 *            <ul>
	 *            <li>proxy</li>
	 *            <li>gateway</li>
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
		// TODO [jeremy] - Startup either the proxy or the gateway.
	}

	private static Arguments extractArguments(String[] args) {
		if (args == null || args.length == 0) {
			return null;
		}

		String mode = args[0];
		int loggingLevels = 0x38;
		if (args.length > 1) {
			for (int i = 1; i < args.length; i++) {
				String arg = args[i];
				if (!arg.matches(ARG_REGEX)) {
					return null;
				}

				int toShift = arg.length() - 1;
				for (int j = 0; j < toShift; j++) {
					loggingLevels >>= 1;
					loggingLevels |= 0x20;
				}
			}
		}

		try {
			return new Arguments(Mode.lookup(mode), new LoggingLevels(loggingLevels));
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	private static enum Mode {

		PROXY("proxy"), GATEWAY("gateway");

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
