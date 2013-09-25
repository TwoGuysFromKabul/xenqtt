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

/**
 * Supplies arguments and flags for the disparate modes available in Xenqtt.
 */
public final class ApplicationArguments {

	private final List<Flag> flags;
	private final Map<String, String> arguments;

	/**
	 * Create a new instance of this class.
	 */
	public ApplicationArguments() {
		this(Collections.<String> emptyList(), new HashMap<String, String>());
	}

	/**
	 * Create a new instance of this class.
	 * 
	 * @param flags
	 *            The flags that were specified for the application
	 * @param arguments
	 *            The arguments that were specified for the application
	 */
	public ApplicationArguments(List<String> flags, Map<String, String> arguments) {
		this.flags = getFlags(flags);
		this.arguments = arguments;
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
		XenqttUtil.validateNotEmpty("argument", argument);

		String arg = arguments.get(format(argument));
		if (arg == null) {
			String message = String.format("The argument %s was required but not found.", argument);
			Log.error(message);
			throw new IllegalStateException(message);
		}

		try {
			return Integer.parseInt(arg);
		} catch (Exception ex) {
			Log.error(ex, "Unable to parse the argument %s as an integer.", argument);
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
		XenqttUtil.validateNotEmpty("argument", argument);

		String arg = arguments.get(format(argument));
		if (arg == null) {
			return defaultValue;
		}

		try {
			return Integer.parseInt(arg);
		} catch (Exception ex) {
			Log.error(ex, "Unable to parse the argument %s as an integer.", argument);
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
		XenqttUtil.validateNotEmpty("argument", argument);

		String arg = arguments.get(format(argument));
		if (arg == null) {
			String message = String.format("The argument %s was required but not found.", argument);
			Log.error(message);
			throw new IllegalStateException(message);
		}

		try {
			return Long.parseLong(arg);
		} catch (Exception ex) {
			Log.error(ex, "Unable to parse the argument %s as a long.", argument);
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
		XenqttUtil.validateNotEmpty("argument", argument);

		String arg = arguments.get(format(argument));
		if (arg == null) {
			return defaultValue;
		}

		try {
			return Long.parseLong(arg);
		} catch (Exception ex) {
			Log.error(ex, "Unable to parse the argument %s as a long.", argument);
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
		XenqttUtil.validateNotEmpty("argument", argument);

		String arg = arguments.get(format(argument));
		if (arg == null) {
			String message = String.format("The argument %s was required but not found.", argument);
			Log.error(message);
			throw new IllegalStateException(message);
		}

		try {
			return Double.parseDouble(arg);
		} catch (Exception ex) {
			Log.error(ex, "Unable to parse the argument %s as a double.", argument);
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
		XenqttUtil.validateNotEmpty("argument", argument);

		String arg = arguments.get(format(argument));
		if (arg == null) {
			return defaultValue;
		}

		try {
			return Double.parseDouble(arg);
		} catch (Exception ex) {
			Log.error(ex, "Unable to parse the argument %s as a double.", argument);
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
		XenqttUtil.validateNotEmpty("argument", argument);

		String arg = arguments.get(format(argument));
		if (arg == null) {
			String message = String.format("The argument %s was required but not found.", argument);
			Log.error(message);
			throw new IllegalStateException(message);
		}

		try {
			return Boolean.parseBoolean(arg);
		} catch (Exception ex) {
			Log.error(ex, "Unable to parse the argument %s as a boolean.", argument);
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
		XenqttUtil.validateNotEmpty("argument", argument);

		String arg = arguments.get(format(argument));
		if (arg == null) {
			return defaultValue;
		}

		try {
			return Boolean.parseBoolean(arg);
		} catch (Exception ex) {
			Log.error(ex, "Unable to parse the argument %s as a boolean.", argument);
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
		XenqttUtil.validateNotEmpty("argument", argument);

		String arg = arguments.get(format(argument));
		if (arg == null) {
			String message = String.format("The argument %s was required but not found.", argument);
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

	private static final class Flag {

		private final String value;
		private boolean interrogated;

		private Flag(String value) {
			this.value = value;
		}

	}

}
