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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

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

	/**
	 * Extract the {@link Arguments arguments} specified by the user from the command-line arguments.
	 * 
	 * @param latch
	 *            The shutdown latch to include in the arguments
	 * @param args
	 *            The command-line arguments specified by the user
	 * 
	 * @return The arguments that will be used to bootstrap the application
	 */
	static Arguments extractArguments(CountDownLatch latch, String... args) {
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

		boolean helpMode = Mode.lookup(mode) == Mode.HELP;
		AppContext applicationArguments = getApplicationArguments(modeArguments, helpMode, latch);

		return new Arguments(globalOptions, mode, applicationArguments);
	}

	private static AppContext getApplicationArguments(List<String> modeArguments, boolean helpMode, CountDownLatch latch) {
		if (modeArguments.isEmpty()) {
			return new AppContext(latch);
		}

		if (helpMode) {
			String desiredHelp = modeArguments.get(0);
			Map<String, String> args = new HashMap<String, String>();
			args.put("-m", desiredHelp);

			return new AppContext(Collections.<String> emptyList(), args, latch);
		}

		List<String> flags = new ArrayList<String>();
		Map<String, String> arguments = new HashMap<String, String>();
		char previousFlagOrArg = '\u0000';
		for (String modeArgument : modeArguments) {
			if (modeArgument.startsWith("-")) {
				if (modeArgument.length() < 2) {
					throw new IllegalArgumentException("You cannot specify a flag or an argument identifier that just has the -");
				}

				if (previousFlagOrArg != '\u0000') {
					flags.add(String.format("-%c", previousFlagOrArg));
				}
				previousFlagOrArg = parseFlags(modeArgument, flags);
			} else {
				if (previousFlagOrArg == '\u0000') {
					throw new IllegalArgumentException(String.format("Cannot specify an argument value without an argument. Value: %s", modeArgument));
				}

				arguments.put(String.format("-%c", previousFlagOrArg), modeArgument);
				previousFlagOrArg = '\u0000';
			}
		}

		if (previousFlagOrArg != '\u0000') {
			flags.add(String.format("-%c", previousFlagOrArg));
		}

		return new AppContext(flags, arguments, latch);
	}

	private static char parseFlags(String modeArgument, List<String> flags) {
		char previous = modeArgument.charAt(1);
		for (int i = 2; i < modeArgument.length(); i++) {
			flags.add(String.format("-%c", previous));
			previous = modeArgument.charAt(i);
		}

		return previous;
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
		 * A {@link Mode mode} that displays the license file for using Xenqtt and exits.
		 */
		LICENSE("license"),

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
		MOCK_BROKER("mockbroker"),

		/**
		 * A {@link Mode mode} that runs the test client for load testing Xenqtt.
		 */
		TEST_CLIENT("testclient");

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

		/**
		 * Determine if a particular mode string represents a valid operating {@link Mode mode}.
		 * 
		 * @param m
		 *            The mode string
		 * 
		 * @return {@code true} if {@code m} represents a valid operating mode, {@code false} if it does not
		 */
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
		 * The {@link AppContext arguments} to pass into the application. These arguments are optional and can be {@code null}.
		 */
		final AppContext applicationArguments;

		/**
		 * Create a new instance of this class.
		 * 
		 * @param mode
		 *            The mode to run Xenqtt in. This is required and cannot be empty or {@code null}
		 * @param applicationArguments
		 *            The {@link AppContext arguments} to pass to the application being started as specified by the {@code mode} given. These are optional and
		 *            may be {@code null}
		 */
		Arguments(String mode, AppContext applicationArguments) {
			this(null, mode, applicationArguments);
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
		 * @param applicationArguments
		 *            The {@link AppContext arguments} to pass to the application being started as specified by the {@code mode} given. These are optional and
		 *            may be {@code null}
		 */
		Arguments(List<String> globalOptions, String mode, AppContext applicationArguments) {
			if (mode == null || mode.trim().equals("")) {
				throw new IllegalArgumentException("The mode cannot be empty or null.");
			}

			this.globalOptions = globalOptions;
			this.mode = Mode.lookup(mode);
			this.applicationArguments = applicationArguments;
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

		boolean isConsoleLoggingSpecified() {
			if (globalOptions == null) {
				return false;
			}

			for (String globalOption : globalOptions) {
				if (globalOption.equalsIgnoreCase("-c")) {
					return true;
				}
			}

			return false;
		}

	}

}
