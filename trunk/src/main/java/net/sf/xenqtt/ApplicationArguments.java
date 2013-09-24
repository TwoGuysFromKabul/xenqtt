package net.sf.xenqtt;

import java.util.ArrayList;
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

	public boolean wasFlagSpecified(String flag) {
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

	public boolean wereAllFlagsInterrogated() {
		for (Flag flag : flags) {
			if (!flag.interrogated) {
				return false;
			}
		}

		return true;
	}

	public Integer getArgumentAsInteger(String argument) {
		XenqttUtil.validateNotEmpty("argument", argument);

		String arg = arguments.get(format(argument));
		if (arg == null) {
			return null;
		}

		try {
			return Integer.valueOf(arg);
		} catch (Exception ex) {
			Log.error(ex, "Unable to parse the argument %s as an integer.", argument);
			throw new RuntimeException(ex);
		}
	}

	private String format(String argOrFlag) {
		if (!argOrFlag.startsWith("-")) {
			return String.format("-%s", argOrFlag);
		}

		return argOrFlag;
	}

	private static final class Flag {

		private final String value;
		private boolean interrogated;

		private Flag(String value) {
			this.value = value;
		}

	}

}
