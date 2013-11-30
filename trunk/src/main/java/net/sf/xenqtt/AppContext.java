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

import net.sf.xenqtt.application.XenqttApplication;

/**
 * Contains the application context for a Xenqtt {@link XenqttApplication application}
 */
public final class AppContext {

	private final List<Flag> flags;
	private final Map<String, String> arguments;
	private final CountDownLatch latch;

	/**
	 * Create a new instance of this class.
	 * 
	 * @param latch
	 *            The {@link CountDownLatch latch} which the main application thread will await on. Trigger this if your application can terminate after
	 *            performing certain tasks (e.g. a test)
	 */
	public AppContext(CountDownLatch latch) {
		this(Collections.<String> emptyList(), new HashMap<String, String>(), latch);
	}

	/**
	 * Create a new instance of this class.
	 * 
	 * @param flags
	 *            The flags that were specified for the application
	 * @param arguments
	 *            The arguments that were specified for the application
	 * @param latch
	 *            The {@link CountDownLatch latch} which the main application thread will await on. Trigger this if your application can terminate after
	 *            performing certain tasks (e.g. a test)
	 */
	public AppContext(List<String> flags, Map<String, String> arguments, CountDownLatch latch) {
		this.flags = getFlags(flags);
		this.arguments = arguments;
		this.latch = latch;
	}

	private List<Flag> getFlags(List<String> stringFlags) {
		List<Flag> flags = new ArrayList<Flag>();
		for (String stringFlag : stringFlags) {
			stringFlag = format(stringFlag);
			flags.add(new Flag(stringFlag));
		}

		return flags;
	}

	/**
	 * Determine if a particular flag was specified.
	 * 
	 * @param flag
	 *            The flag to check for
	 * 
	 * @return {@code true} if the specified {@code flag} was found in the flags that were given by the user, {@code false} if it was not
	 */
	public boolean isFlagSpecified(String flag) {
		XenqttUtil.validateNotEmpty("flag", flag);

		flag = format(flag);
		for (Flag f : flags) {
			if (f.value.equals(flag)) {
				f.interrogated = true;
				return true;
			}
		}

		return false;
	}

	/**
	 * Determine if each of the flags that was specified by the user was interrogated by the application.
	 * 
	 * @return {@code true} if each user-specified flag was interrogated, {@code false} if at least one was not
	 */
	public boolean wereAllFlagsInterrogated() {
		for (Flag flag : flags) {
			if (!flag.interrogated) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Get an argument value interpreted as an {@code int}.
	 * 
	 * @param argument
	 *            The name of the argument
	 * 
	 * @return The value associated with the argument interpreted as an {@code int}
	 * 
	 * @throws IllegalStateException
	 *             If the argument is not found
	 * @throws NumberFormatException
	 *             If the argument is found but cannot be interpreted as an {@code int}
	 */
	public int getArgAsInt(String argument) {
		String arg = getArgAsString(argument);
		try {
			return Integer.parseInt(arg);
		} catch (Exception ex) {
			Log.error(ex, "Unable to parse the argument %s as an integer.", format(argument));
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Get an argument value interpreted as an {@code int}.
	 * 
	 * @param argument
	 *            The name of the argument
	 * @param defaultValue
	 *            The default value to use if the argument is not found
	 * 
	 * @return The value associated with the argument interpreted as an {@code int}
	 * 
	 * @throws NumberFormatException
	 *             If the argument is found but cannot be interpreted as an {@code int}
	 */
	public int getArgAsInt(String argument, int defaultValue) {
		String arg = getArgAsString(argument, null);
		if (arg == null) {
			return defaultValue;
		}

		try {
			return Integer.parseInt(arg);
		} catch (Exception ex) {
			Log.error(ex, "Unable to parse the argument %s as an integer.", format(argument));
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Get an argument value interpreted as a {@code long}.
	 * 
	 * @param argument
	 *            The name of the argument
	 * 
	 * @return The value associated with the argument interpreted as a {@code long}
	 * 
	 * @throws IllegalStateException
	 *             If the argument is not found
	 * @throws NumberFormatException
	 *             If the argument is found but cannot be interpreted as a {@code long}
	 */
	public long getArgAsLong(String argument) {
		String arg = getArgAsString(argument);
		try {
			return Long.parseLong(arg);
		} catch (Exception ex) {
			Log.error(ex, "Unable to parse the argument %s as a long.", format(argument));
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Get an argument value interpreted as a {@code long}.
	 * 
	 * @param argument
	 *            The name of the argument
	 * @param defaultValue
	 *            The default value to use if the argument is not found
	 * 
	 * @return The value associated with the argument interpreted as a {@code long}
	 * 
	 * @throws NumberFormatException
	 *             If the argument is found but cannot be interpreted as a {@code long}
	 */
	public long getArgAsLong(String argument, long defaultValue) {
		String arg = getArgAsString(argument, null);
		if (arg == null) {
			return defaultValue;
		}

		try {
			return Long.parseLong(arg);
		} catch (Exception ex) {
			Log.error(ex, "Unable to parse the argument %s as a long.", format(argument));
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Get an argument value interpreted as a {@code double}.
	 * 
	 * @param argument
	 *            The name of the argument
	 * 
	 * @return The value associated with the argument interpreted as a {@code double}
	 * 
	 * @throws IllegalStateException
	 *             If the argument is not found
	 * @throws NumberFormatException
	 *             If the argument is found but cannot be interpreted as a {@code double}
	 */
	public double getArgAsDouble(String argument) {
		String arg = getArgAsString(argument);
		try {
			return Double.parseDouble(arg);
		} catch (Exception ex) {
			Log.error(ex, "Unable to parse the argument %s as a double.", format(argument));
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Get an argument value interpreted as a {@code double}.
	 * 
	 * @param argument
	 *            The name of the argument
	 * @param defaultValue
	 *            The default value to use if the argument is not found
	 * 
	 * @return The value associated with the argument interpreted as a {@code double}
	 * 
	 * @throws NumberFormatException
	 *             If the argument is found but cannot be interpreted as a {@code double}
	 */
	public double getArgAsDouble(String argument, double defaultValue) {
		String arg = getArgAsString(argument, null);
		if (arg == null) {
			return defaultValue;
		}

		try {
			return Double.parseDouble(arg);
		} catch (Exception ex) {
			Log.error(ex, "Unable to parse the argument %s as a double.", format(argument));
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Get an argument value interpreted as a {@code boolean}.
	 * 
	 * @param argument
	 *            The name of the argument
	 * 
	 * @return The value associated with the argument interpreted as a {@code boolean}
	 * 
	 * @throws IllegalStateException
	 *             If the argument is not found
	 * @throws NumberFormatException
	 *             If the argument is found but cannot be interpreted as a {@code boolean}
	 */
	public boolean getArgAsBoolean(String argument) {
		String arg = getArgAsString(argument);
		try {
			return Boolean.parseBoolean(arg);
		} catch (Exception ex) {
			Log.error(ex, "Unable to parse the argument %s as a boolean.", format(argument));
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Get an argument value interpreted as a {@code boolean}.
	 * 
	 * @param argument
	 *            The name of the argument
	 * @param defaultValue
	 *            The default value to use if the argument is not found
	 * 
	 * @return The value associated with the argument interpreted as a {@code boolean}
	 * 
	 * @throws NumberFormatException
	 *             If the argument is found but cannot be interpreted as a {@code boolean}
	 */
	public boolean getArgAsBoolean(String argument, boolean defaultValue) {
		String arg = getArgAsString(argument, null);
		if (arg == null) {
			return defaultValue;
		}

		try {
			return Boolean.parseBoolean(arg);
		} catch (Exception ex) {
			Log.error(ex, "Unable to parse the argument %s as a boolean.", format(argument));
			throw new RuntimeException(ex);
		}
	}

	/**
	 * Get an argument value interpreted as a {@link String string}.
	 * 
	 * @param argument
	 *            The name of the argument
	 * 
	 * @return The value associated with the argument interpreted as a string
	 * 
	 * @throws IllegalStateException
	 *             If the argument is not found
	 */
	public String getArgAsString(String argument) {
		String arg = getArgAsString(argument, null);
		if (arg == null) {
			String message = String.format("The argument %s was required but not found.", format(argument));
			Log.error(message);
			throw new IllegalStateException(message);
		}

		return arg;
	}

	/**
	 * Get an argument value interpreted as a {@link String string}.
	 * 
	 * @param argument
	 *            The name of the argument
	 * @param defaultValue
	 *            The default value to use if the argument is not found
	 * 
	 * @return The value associated with the argument interpreted as a string
	 */
	public String getArgAsString(String argument, String defaultValue) {

		XenqttUtil.validateNotEmpty("argument", argument);

		String arg = arguments.get(format(argument));
		if (arg == null) {
			if (isFlagSpecified(argument)) {
				String message = String.format("The argument %s requires a value.", format(argument));
				Log.error(message);
				throw new IllegalStateException(message);
			}
			return defaultValue;
		}

		return arg;
	}

	private String format(String argOrFlag) {
		if (!argOrFlag.startsWith("-")) {
			return String.format("-%s", argOrFlag);
		}

		return argOrFlag;
	}

	/**
	 * @return {@code true} if there are no flags or arguments, {@code false} if there is at least one flag or argument
	 */
	public boolean isEmpty() {
		return flags.isEmpty() && arguments.isEmpty();
	}

	/**
	 * Invoke if your application has finished its work and no longer needs to run.
	 */
	public void applicationDone() {
		latch.countDown();
	}

	private static final class Flag {

		private final String value;
		private boolean interrogated;

		private Flag(String value) {
			this.value = value;
		}

	}

}
