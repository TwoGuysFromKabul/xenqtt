package net.sf.xenqtt;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Extracts arguments passed on the command-line for the invocation of a Xenqtt application. Arguments passed in must come in the following order:
 * </p>
 * 
 * <pre>
 * [global-options] mode [mode-arguments]
 * </pre>
 * 
 * <p>
 * More information on each of the components specified above follows:
 * </p>
 * 
 * <ul>
 * <li>{@code global-options} - Global options and switches that apply regardless of the chosen mode. Optional</li>
 * <li>{@code mode} - One of the operating modes for Xenqtt. Required</li>
 * <li>{@code mode-arguments} - The arguments that will be passed into the invocation of the mode's {@code main} method. Arguments are passed in the order they
 * are received. Optional</li>
 * </ul>
 */
final class ArgumentExtractor {

	private static final String LOGGING_LEVEL_COMMAND_LINE_FLAG_REGEX = "^\\-(?i:v){1,2}$";

	private ArgumentExtractor() {
	}

	static Arguments extractArguments(String... args) {
		if (args == null || args.length == 0) {
			return null;
		}

		List<String> globalOptions = new ArrayList<String>();
		String mode = null;
		List<String> modeArguments = new ArrayList<String>();
		for (String arg : args) {
			if (arg.startsWith("-") && mode == null) {
				globalOptions.add(arg);
			} else {
				if (mode == null) {
					mode = arg;
				} else {
					modeArguments.add(arg);
				}
			}
		}

		if (mode == null || !Mode.isValidMode(mode)) {
			return null;
		}

		return new Arguments(globalOptions, mode, modeArguments.toArray(new String[0]));
	}

	/**
	 * Defines the valid modes in which Xenqtt can operate. The user specifies which mode they'd like to use via a command-line argument. The mode argument is
	 * required.
	 */
	static enum Mode {

		/**
		 * A {@link Mode mode} that displays help information for using Xenqtt and exits.
		 */
		HELP("help"),

		/**
		 * A {@link Mode mode} that runs the MQTT proxy.
		 */
		PROXY("proxy"),

		/**
		 * A {@link Mode mode} that runs the MQTT gateway.
		 */
		GATEWAY("gateway"),

		/**
		 * A {@link Mode mode} that runs the mock broker used in testing.
		 */
		MOCK_BROKER("mockbroker");

		private final String mode;

		private Mode(String mode) {
			this.mode = mode;
		}

		/**
		 * @return The mode as a {@link String string}
		 */
		String getMode() {
			return mode;
		}

		/**
		 * Lookup a {@link Mode mode} based on a textual {@link String string}.
		 * 
		 * @param m
		 *            The string value specifying the mode
		 * 
		 * @return The {@code Mode} corresponding to the specified mode string. If no such mode is found an exception is raised
		 */
		static Mode lookup(String m) {
			for (Mode mode : values()) {
				if (mode.mode.equalsIgnoreCase(m)) {
					return mode;
				}
			}

			throw new IllegalArgumentException(String.format("Invalid mode specified: %s", m));
		}

		static boolean isValidMode(String m) {
			for (Mode mode : values()) {
				if (mode.mode.equalsIgnoreCase(m)) {
					return true;
				}
			}

			return false;
		}

	}

	/**
	 * Stores the arguments extracted from the command-line parameters specified by the user. In addition to storing the arguments this class provides behavior
	 * for extracting useful information about the arguments specified. This includes global options such as the desired logging level.
	 */
	static final class Arguments {

		/**
		 * The global options that were specified, if any. These are not required and can be {@code null}.
		 */
		final List<String> globalOptions;

		/**
		 * The specified {@link Mode mode}. This is required and will not be {@code null}.
		 */
		final Mode mode;

		/**
		 * The arguments to pass into the mode in the order they should be passed. These arguments are optional and can be {@code null}.
		 */
		final String[] modeArguments;

		/**
		 * Create a new instance of this class.
		 * 
		 * @param mode
		 *            The mode to run Xenqtt in. This is required and cannot be empty or {@code null}
		 * @param modeArguments
		 *            The arguments to pass to the application being started as specified by the {@code mode} given. These are optional and may be {@code null}
		 */
		Arguments(String mode, String[] modeArguments) {
			this(null, mode, modeArguments);
		}

		/**
		 * Create a new instance of this class.
		 * 
		 * @param globalOptions
		 *            The global options to use system-wide. These are optional and may be {@code null}
		 * @param mode
		 *            The mode to run Xenqtt in. This is required and cannot be empty or {@code null}
		 */
		Arguments(List<String> globalOptions, String mode) {
			this(globalOptions, mode, null);
		}

		/**
		 * Create a new instance of this class.
		 * 
		 * @param globalOptions
		 *            The global options to use system-wide. These are optional and may be {@code null}
		 * @param mode
		 *            The mode to run Xenqtt in. This is required and cannot be empty or {@code null}
		 * @param modeArguments
		 *            The arguments to pass to the application being started as specified by the {@code mode} given. These are optional and may be {@code null}
		 */
		Arguments(List<String> globalOptions, String mode, String[] modeArguments) {
			if (mode == null || mode.trim().equals("")) {
				throw new IllegalArgumentException("The mode cannot be empty or null.");
			}

			this.globalOptions = globalOptions;
			this.mode = Mode.lookup(mode);
			this.modeArguments = modeArguments;
		}

		/**
		 * Determine the {@link LoggingLevels logging levels} to use. Logging is defined globally and can be tuned via command-line arguments. By default if the
		 * user passes in nothing default logging (WARN or higher) is used. The specification of one {@code -v} switch, two {@code -v} switches, or the
		 * composite {@code -vv} switch will enable INFO, DEBUG, and DEBUG logging respectively.
		 * 
		 * @return The logging levels to use globally
		 */
		LoggingLevels determineLoggingLevels() {
			if (globalOptions == null) {
				return new LoggingLevels(LoggingLevels.DEFAULT_LOGGING_LEVELS);
			}

			int loggingLevels = LoggingLevels.DEFAULT_LOGGING_LEVELS;
			for (String globalOption : globalOptions) {
				if (globalOption.matches(LOGGING_LEVEL_COMMAND_LINE_FLAG_REGEX)) {
					int toShift = globalOption.length() - 1;
					for (int j = 0; j < toShift; j++) {
						loggingLevels >>= 1;
						loggingLevels |= 0x20;
					}

					if ((loggingLevels & LoggingLevels.DEBUG_FLAG) != 0) {
						break;
					}
				}
			}

			return new LoggingLevels(loggingLevels);
		}

	}

}
