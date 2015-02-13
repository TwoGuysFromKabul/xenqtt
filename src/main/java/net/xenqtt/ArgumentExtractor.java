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
package net.xenqtt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import net.xenqtt.application.XenqttApplication;

/**
 * <p>
 * Extracts arguments passed on the command-line for the invocation of a Xenqtt application. Arguments passed in must come in the following order:
 * </p>
 * 
 * <pre>
 * [global-options] appName [app-arguments]
 * </pre>
 * 
 * <p>
 * More information on each of the components specified above follows:
 * </p>
 * 
 * <ul>
 * <li>{@code global-options} - Global options and switches that apply regardless of the chosen app. Optional</li>
 * <li>{@code appNAME} - One of the Xenqtt applications. Required</li>
 * <li>{@code app-arguments} - The arguments that will be passed into the invocation of the app's {@code main} method. Arguments are passed in the order they
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
		String appName = null;
		List<String> appArguments = new ArrayList<String>();
		for (String arg : args) {
			if (arg.startsWith("-") && appName == null) {
				globalOptions.add(arg);
			} else {
				if (appName == null) {
					appName = arg.toLowerCase();
				} else {
					appArguments.add(arg);
				}
			}
		}

		if (appName == null || !isValidApp(appName)) {
			return null;
		}

		boolean helpApp = appName.equalsIgnoreCase("help");
		AppContext applicationArguments = getApplicationArguments(appArguments, helpApp, latch);

		return new Arguments(globalOptions, appName, applicationArguments);
	}

	private static boolean isValidApp(String app) {

		String searchFor = String.format("%s/%sApplication.class", XenqttApplication.class.getPackage().getName().replace('.', '/'), app);
		List<String> classFileNames = XenqttUtil.findFilesOnClassPath(XenqttApplication.class.getPackage().getName(), ".class");
		for (String name : classFileNames) {
			if (searchFor.equalsIgnoreCase(name)) {
				return true;
			}
		}

		return false;
	}

	private static AppContext getApplicationArguments(List<String> appArguments, boolean helpApp, CountDownLatch latch) {
		if (appArguments.isEmpty()) {
			return new AppContext(latch);
		}

		if (helpApp) {
			String desiredHelp = appArguments.get(0);
			Map<String, String> args = new HashMap<String, String>();
			args.put("-m", desiredHelp);

			return new AppContext(Collections.<String> emptyList(), args, latch);
		}

		List<String> flags = new ArrayList<String>();
		Map<String, String> arguments = new HashMap<String, String>();
		char previousFlagOrArg = '\u0000';
		for (String appArgument : appArguments) {
			if (appArgument.startsWith("-")) {
				if (appArgument.length() < 2) {
					throw new IllegalArgumentException("You cannot specify a flag or an argument identifier that just has the -");
				}

				if (previousFlagOrArg != '\u0000') {
					flags.add(String.format("-%c", previousFlagOrArg));
				}
				previousFlagOrArg = parseFlags(appArgument, flags);
			} else {
				if (previousFlagOrArg == '\u0000') {
					throw new IllegalArgumentException(String.format("Cannot specify an argument value without an argument. Value: %s", appArgument));
				}

				arguments.put(String.format("-%c", previousFlagOrArg), appArgument);
				previousFlagOrArg = '\u0000';
			}
		}

		if (previousFlagOrArg != '\u0000') {
			flags.add(String.format("-%c", previousFlagOrArg));
		}

		return new AppContext(flags, arguments, latch);
	}

	private static char parseFlags(String appArgument, List<String> flags) {
		char previous = appArgument.charAt(1);
		for (int i = 2; i < appArgument.length(); i++) {
			flags.add(String.format("-%c", previous));
			previous = appArgument.charAt(i);
		}

		return previous;
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
		 * The specified application name. This is required and will not be {@code null}. This will always be all lower case.
		 */
		final String applicationName;

		/**
		 * The {@link AppContext arguments} to pass into the application. These arguments are optional and can be {@code null}.
		 */
		final AppContext applicationArguments;

		/**
		 * Create a new instance of this class.
		 * 
		 * @param app
		 *            The XenQTT application to run. This is required and cannot be empty or {@code null}
		 * @param applicationArguments
		 *            The {@link AppContext arguments} to pass to the application being started as specified by the {@code a[[} given. These are optional and
		 *            may be {@code null}
		 */
		Arguments(String app, AppContext applicationArguments) {
			this(null, app, applicationArguments);
		}

		/**
		 * Create a new instance of this class.
		 * 
		 * @param globalOptions
		 *            The global options to use system-wide. These are optional and may be {@code null}
		 * @param app
		 *            The Xenqtt application to run. This is required and cannot be empty or {@code null}
		 */
		Arguments(List<String> globalOptions, String app) {
			this(globalOptions, app, null);
		}

		/**
		 * Create a new instance of this class.
		 * 
		 * @param globalOptions
		 *            The global options to use system-wide. These are optional and may be {@code null}
		 * @param app
		 *            The Xenqtt application to run. This is required and cannot be empty or {@code null}
		 * @param applicationArguments
		 *            The {@link AppContext arguments} to pass to the application being started as specified by the {@code app} given. These are optional and
		 *            may be {@code null}
		 */
		Arguments(List<String> globalOptions, String app, AppContext applicationArguments) {
			if (app == null || app.trim().equals("")) {
				throw new IllegalArgumentException("The application to run cannot be empty or null.");
			}

			this.globalOptions = globalOptions;
			this.applicationName = app.toLowerCase();
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
