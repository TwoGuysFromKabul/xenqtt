package net.sf.xenqtt;

/**
 * Supplies arguments and flags for the disparate modes available in Xenqtt.
 */
public final class ModeArguments {

	private static final class Flag {

		private final String value;
		private boolean interrogated;

		private Flag(String value) {
			this.value = value;
		}

	}

}
