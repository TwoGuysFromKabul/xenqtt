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
	private static final Logger LOGGER;

	static {
		DELEGATE = createDelegate();
		LoggingLevels levels = determineLoggingLevels();
		LOGGER = getLogger(levels);
		LOGGER.init();
	}

	private Log() {
	}

	private static LoggingDelegate createDelegate() {
		if (inUnitTest()) {
			return new ConsoleLoggingDelegate();
		}

		try {
			Class.forName("org.apache.log4j.Logger");

			return new Log4jLoggingDelegate();
		} catch (ClassNotFoundException ex) {
			// ignore
		} catch (Exception ex) {
			System.err.println("Unable to determine which logging strategy to use. Using Java logging by default.");
		}

		return new JavaLoggingDelegate();
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

		return Xenqtt.loggingLevels;
	}

	private static LoggingLevels getLog4jLoggingLevels() {
		return ((Log4jLoggingDelegate) DELEGATE).getLoggingLevels();
	}

	private static Logger getLogger(LoggingLevels levels) {
		if (Boolean.parseBoolean(System.getProperty("xenqtt.logging.async")) || inUnitTest()) {
			return new AsynchronousLogger(levels, DELEGATE);
		}

		return new SynchronousLogger(levels, DELEGATE);
	}

	/**
	 * Shuts down the logger. Package visible. Should only be called by {@link Xenqtt}.
	 */
	static void shutdown() {
		LOGGER.shutdown();
	}

	/**
	 * Log a message at the TRACE log level, if possible.
	 * 
	 * @param message
	 *            The log message to emit
	 * @param parameters
	 *            The parameters to use in replacing format specifiers in the specified {@code message}. This can be omitted if no such specifiers exist
	 */
	public static void trace(String message, Object... parameters) {
		LOGGER.log(LoggingLevels.TRACE_FLAG, message, parameters);
	}

	/**
	 * Log a message at the DEBUG log level, if possible.
	 * 
	 * @param message
	 *            The log message to emit
	 * @param parameters
	 *            The parameters to use in replacing format specifiers in the specified {@code message}. This can be omitted if no such specifiers exist
	 */
	public static void debug(String message, Object... parameters) {
		LOGGER.log(LoggingLevels.DEBUG_FLAG, message, parameters);
	}

	/**
	 * Log a message at the INFO log level, if possible.
	 * 
	 * @param message
	 *            The log message to emit
	 * @param parameters
	 *            The parameters to use in replacing format specifiers in the specified {@code message}. This can be omitted if no such specifiers exist
	 */
	public static void info(String message, Object... parameters) {
		LOGGER.log(LoggingLevels.INFO_FLAG, message, parameters);
	}

	/**
	 * Log a message at the WARN log level, if possible.
	 * 
	 * @param message
	 *            The log message to emit
	 * @param parameters
	 *            The parameters to use in replacing format specifiers in the specified {@code message}. This can be omitted if no such specifiers exist
	 */
	public static void warn(String message, Object... parameters) {
		LOGGER.log(LoggingLevels.WARN_FLAG, message, parameters);
	}

	/**
	 * Log a message at the WARN log level, if possible.
	 * 
	 * @param t
	 *            A {@link Throwable} that was thrown at some point that is being logged with the {@code message} and {@code parameters}
	 * @param message
	 *            The log message to emit
	 * @param parameters
	 *            The parameters to use in replacing format specifiers in the specified {@code message}. This can be omitted if no such specifiers exist
	 */
	public static void warn(Throwable t, String message, Object... parameters) {
		LOGGER.log(LoggingLevels.WARN_FLAG, t, message, parameters);
	}

	/**
	 * Log a message at the ERROR log level, if possible.
	 * 
	 * @param message
	 *            The log message to emit
	 * @param parameters
	 *            The parameters to use in replacing format specifiers in the specified {@code message}. This can be omitted if no such specifiers exist
	 */
	public static void error(String message, Object... parameters) {
		LOGGER.log(LoggingLevels.ERROR_FLAG, message, parameters);
	}

	/**
	 * Log a message at the ERROR log level, if possible.
	 * 
	 * @param t
	 *            A {@link Throwable} that was thrown at some point that is being logged with the {@code message} and {@code parameters}
	 * @param message
	 *            The log message to emit
	 * @param parameters
	 *            The parameters to use in replacing format specifiers in the specified {@code message}. This can be omitted if no such specifiers exist
	 */
	public static void error(Throwable t, String message, Object... parameters) {
		LOGGER.log(LoggingLevels.ERROR_FLAG, t, message, parameters);
	}

	/**
	 * Log a message at the FATAL log level.
	 * 
	 * @param message
	 *            The log message to emit
	 * @param parameters
	 *            The parameters to use in replacing format specifiers in the specified {@code message}. This can be omitted if no such specifiers exist
	 */
	public static void fatal(String message, Object... parameters) {
		LOGGER.log(LoggingLevels.FATAL_FLAG, message, parameters);
	}

	/**
	 * Log a message at the FATAL log level.
	 * 
	 * @param t
	 *            A {@link Throwable} that was thrown at some point that is being logged with the {@code message} and {@code parameters}
	 * @param message
	 *            The log message to emit
	 * @param parameters
	 *            The parameters to use in replacing format specifiers in the specified {@code message}. This can be omitted if no such specifiers exist
	 */
	public static void fatal(Throwable t, String message, Object... parameters) {
		LOGGER.log(LoggingLevels.FATAL_FLAG, t, message, parameters);
	}

	private static final class ConsoleLoggingDelegate implements LoggingDelegate {

		@Override
		public void trace(String message) {
			System.out.println(message);
		}

		@Override
		public void trace(Throwable t, String message) {
			System.out.println(message);
			if (t != null) {
				t.printStackTrace(System.out);
			}
		}

		@Override
		public void debug(String message) {
			System.out.println(message);
		}

		@Override
		public void debug(Throwable t, String message) {
			System.out.println(message);
			if (t != null) {
				t.printStackTrace(System.out);
			}
		}

		@Override
		public void info(String message) {
			System.out.println(message);
		}

		@Override
		public void info(Throwable t, String message) {
			System.out.println(message);
			if (t != null) {
				t.printStackTrace(System.out);
			}
		}

		@Override
		public void warn(String message) {
			System.out.println(message);
		}

		@Override
		public void warn(Throwable t, String message) {
			System.out.println(message);
			if (t != null) {
				t.printStackTrace(System.out);
			}
		}

		@Override
		public void error(String message) {
			System.out.println(message);
		}

		@Override
		public void error(Throwable t, String message) {
			System.out.println(message);
			if (t != null) {
				t.printStackTrace(System.out);
			}
		}

		@Override
		public void fatal(String message) {
			System.out.println(message);
		}

		@Override
		public void fatal(Throwable t, String message) {
			System.out.println(message);
			if (t != null) {
				t.printStackTrace(System.out);
			}

		}

	}

}
