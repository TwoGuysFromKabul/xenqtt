package net.sf.xenqtt.util;

/**
 * <p>
 * Provides disparate logging methods for use within xenqtt. The {@code Log} class is able to detect other logging implementations and, should they be present
 * and available, use those by default. Should other logging implementations not be available the fallback logging mechanism will be used.
 * </p>
 * 
 * <p>
 * At present the fallback logging mechanism writes all {@code trace}, {@code debug}, and {@code info} logging events to standard out and all {@code warn},
 * {@code error}, and {@code fatal} logging events to standard error.
 * </p>
 */
public final class Log {

	private static final LoggingDelegate DELEGATE;
	private static LoggingLevels levels;

	static {
		DELEGATE = createDelegate();
		levels = determineLoggingLevels();
	}

	private Log() {
	}

	private static LoggingDelegate createDelegate() {
		if (!inUnitTest()) {
			try {
				Class.forName("org.apache.log4j.Logger");

				return new Log4jLoggingDelegate();
			} catch (ClassNotFoundException ex) {
				System.out.println("Log4j is not available on the classpath. Using console logging by default.");
			} catch (Exception ex) {
				System.err.println("Unable to determine which logging strategy to use. Using console logging by default.");
			}
		}

		return new ConsoleLoggingDelegate();
	}

	private static boolean inUnitTest() {
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		for (StackTraceElement element : stackTrace) {
			if (element.getClassName().startsWith("org.junit")) {
				return true;
			}
		}

		return false;
	}

	private static LoggingLevels determineLoggingLevels() {
		if (DELEGATE instanceof Log4jLoggingDelegate) {
			return getLog4jLoggingLevels();
		}

		return getDefaultLoggingLevels();
	}

	private static LoggingLevels getLog4jLoggingLevels() {
		return ((Log4jLoggingDelegate) DELEGATE).getLoggingLevels();
	}

	private static LoggingLevels getDefaultLoggingLevels() {
		return new LoggingLevels(false, false, false, true, true, true);
	}

	/**
	 * Set the {@link LoggingLevels logging levels} to use.
	 * 
	 * @param levels
	 *            The logging levels to use
	 */
	public static void setLoggingLevels(LoggingLevels levels) {
		Log.levels = levels;
	}

	/**
	 * @return The current {@link LoggingLevels logging levels} being used by this {@link Log} singleton
	 */
	public static LoggingLevels getLoggingLevels() {
		return levels;
	}

	public static void trace(String message, Object... parameters) {
		if (levels.traceEnabled) {
			DELEGATE.trace(message, parameters);
		}
	}

	public static void debug(String message, Object... parameters) {
		if (levels.debugEnabled) {
			DELEGATE.debug(message, parameters);
		}
	}

	public static void info(String message, Object... parameters) {
		if (levels.infoEnabled) {
			DELEGATE.info(message, parameters);
		}
	}

	public static void warn(String message, Object... parameters) {
		if (levels.warnEnabled) {
			DELEGATE.warn(message, parameters);
		}
	}

	public static void warn(Throwable t, String message, Object... parameters) {
		if (levels.warnEnabled) {
			DELEGATE.warn(t, message, parameters);
		}
	}

	public static void error(String message, Object... parameters) {
		if (levels.errorEnabled) {
			DELEGATE.error(message, parameters);
		}
	}

	public static void error(Throwable t, String message, Object... parameters) {
		if (levels.errorEnabled) {
			DELEGATE.error(t, message, parameters);
		}
	}

	public static void fatal(String message, Object... parameters) {
		if (levels.fatalEnabled) {
			DELEGATE.fatal(message, parameters);
		}
	}

	public static void fatal(Throwable t, String message, Object... parameters) {
		if (levels.fatalEnabled) {
			DELEGATE.fatal(t, message, parameters);
		}
	}

	static interface LoggingDelegate {

		void trace(String message, Object... parameters);

		void debug(String message, Object... parameters);

		void info(String message, Object... parameters);

		void warn(String message, Object... parameters);

		void warn(Throwable t, String message, Object... parameters);

		void error(String message, Object... parameters);

		void error(Throwable t, String message, Object... parameters);

		void fatal(String message, Object... parameters);

		void fatal(Throwable t, String message, Object... parameters);

	}

	private static final class ConsoleLoggingDelegate implements LoggingDelegate {

		/**
		 * @see net.sf.xenqtt.util.Log.LoggingDelegate#trace(java.lang.String, java.lang.Object[])
		 */
		@Override
		public void trace(String message, Object... parameters) {
			System.out.printf(message, parameters);
			System.out.println();
		}

		/**
		 * @see net.sf.xenqtt.util.Log.LoggingDelegate#debug(java.lang.String, java.lang.Object[])
		 */
		@Override
		public void debug(String message, Object... parameters) {
			System.out.printf(message, parameters);
			System.out.println();
		}

		/**
		 * @see net.sf.xenqtt.util.Log.LoggingDelegate#info(java.lang.String, java.lang.Object[])
		 */
		@Override
		public void info(String message, Object... parameters) {
			System.out.printf(message, parameters);
			System.out.println();
		}

		/**
		 * @see net.sf.xenqtt.util.Log.LoggingDelegate#warn(java.lang.String, java.lang.Object[])
		 */
		@Override
		public void warn(String message, Object... parameters) {
			System.err.printf(message, parameters);
			System.err.println();
		}

		/**
		 * @see net.sf.xenqtt.util.Log.LoggingDelegate#warn(java.lang.Throwable, java.lang.String, java.lang.Object[])
		 */
		@Override
		public void warn(Throwable t, String message, Object... parameters) {
			System.err.printf(message, parameters);
			System.err.println();
			t.printStackTrace();
		}

		/**
		 * @see net.sf.xenqtt.util.Log.LoggingDelegate#error(java.lang.String, java.lang.Object[])
		 */
		@Override
		public void error(String message, Object... parameters) {
			System.err.printf(message, parameters);
			System.err.println();
		}

		/**
		 * @see net.sf.xenqtt.util.Log.LoggingDelegate#error(java.lang.Throwable, java.lang.String, java.lang.Object[])
		 */
		@Override
		public void error(Throwable t, String message, Object... parameters) {
			System.err.printf(message, parameters);
			System.err.println();
			t.printStackTrace();
		}

		/**
		 * @see net.sf.xenqtt.util.Log.LoggingDelegate#fatal(java.lang.String, java.lang.Object[])
		 */
		@Override
		public void fatal(String message, Object... parameters) {
			System.err.printf(message, parameters);
			System.err.println();
		}

		/**
		 * @see net.sf.xenqtt.util.Log.LoggingDelegate#fatal(java.lang.Throwable, java.lang.String, java.lang.Object[])
		 */
		@Override
		public void fatal(Throwable t, String message, Object... parameters) {
			System.err.printf(message, parameters);
			System.err.println();
			t.printStackTrace();
		}

	}

}
