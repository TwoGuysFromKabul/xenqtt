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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
			System.out.println("Log4j is not available on the classpath. Using Java logging by default.");
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

		return getDefaultLoggingLevels();
	}

	private static LoggingLevels getLog4jLoggingLevels() {
		return ((Log4jLoggingDelegate) DELEGATE).getLoggingLevels();
	}

	private static LoggingLevels getDefaultLoggingLevels() {
		return new LoggingLevels(false, false, false, true, true, true);
	}

	private static Logger getLogger(LoggingLevels levels) {
		if (Boolean.parseBoolean(System.getProperty("xenqtt.logging.async")) || inUnitTest()) {
			return new AsynchronousLogger(levels);
		}

		return new SynchronousLogger(levels);
	}

	/**
	 * Set the {@link LoggingLevels logging levels} to use. This method will only update the logging levels for the asynchronous logging mode. Level changes for
	 * synchronous loggers are not supported by this API.
	 * 
	 * @param levels
	 *            The logging levels to use
	 */
	public static void setLoggingLevels(LoggingLevels levels) {
		if (LOGGER instanceof SynchronousLogger) {
			return;
		}

		((AsynchronousLogger) LOGGER).setLoggingLevels(levels);
	}

	public static void setLoggingDestination(String outputFile) {
		if (LOGGER instanceof SynchronousLogger) {
			return;
		}

		((AsynchronousLogger) LOGGER).setLoggingDestination(outputFile);
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

	/**
	 * Specifies a type to which logging events are delegated. Specific implementations will log in different ways depending on how they are implemented.
	 */
	static interface LoggingDelegate {

		void trace(String message, Object... parameters);

		void trace(Throwable t, String message, Object... parameters);

		void debug(String message, Object... parameters);

		void debug(Throwable t, String message, Object... parameters);

		void info(String message, Object... parameters);

		void info(Throwable t, String message, Object... parameters);

		void warn(String message, Object... parameters);

		void warn(Throwable t, String message, Object... parameters);

		void error(String message, Object... parameters);

		void error(Throwable t, String message, Object... parameters);

		void fatal(String message, Object... parameters);

		void fatal(Throwable t, String message, Object... parameters);

	}

	private static abstract class LogWork {

		protected static enum WorkType {

			MESSAGE, LEVELS, DESTINATION_CHANGE;

		}

		protected abstract WorkType workType();

	}

	private static final class LogMessage extends LogWork {

		private final int levelFlag;
		private final String message;
		private final Object[] parameters;
		private final Throwable t;

		private LogMessage(int levelFlag, String message, Object[] parameters) {
			this(levelFlag, message, parameters, null);
		}

		private LogMessage(int levelFlag, String message, Object[] parameters, Throwable t) {
			this.levelFlag = levelFlag;
			this.message = message;
			this.parameters = parameters;
			this.t = t;
		}

		@Override
		protected WorkType workType() {
			return WorkType.MESSAGE;
		}

	}

	private static final class LoggingLevelsChange extends LogWork {

		private final LoggingLevels levels;

		private LoggingLevelsChange(LoggingLevels levels) {
			this.levels = levels;
		}

		@Override
		protected WorkType workType() {
			return WorkType.LEVELS;
		}

	}

	private static final class LoggingDestinationChange extends LogWork {

		private final String outputFile;

		private LoggingDestinationChange(String outputFile) {
			this.outputFile = outputFile;
		}

		@Override
		protected WorkType workType() {
			return WorkType.DESTINATION_CHANGE;
		}

	}

	private static interface Logger {

		void log(int levelFlag, String message, Object... parameters);

		void log(int levelFlag, Throwable t, String message, Object... parameters);

	}

	private static final class SynchronousLogger implements Logger {

		private final LoggingLevels levels;

		private SynchronousLogger(LoggingLevels levels) {
			this.levels = levels;
		}

		/**
		 * @see net.sf.xenqtt.Log.Logger#log(int, java.lang.String, java.lang.Object[])
		 */
		@Override
		public void log(int levelFlag, String message, Object... parameters) {
			if (!levels.isLoggable(levelFlag)) {
				return;
			}

			doLog(levelFlag, null, message, parameters);
		}

		/**
		 * @see net.sf.xenqtt.Log.Logger#log(int, java.lang.Throwable, java.lang.String, java.lang.Object[])
		 */
		@Override
		public void log(int levelFlag, Throwable t, String message, Object... parameters) {
			if (!levels.isLoggable(levelFlag)) {
				return;
			}

			doLog(levelFlag, t, message, parameters);
		}

		private void doLog(int levelFlag, Throwable t, String message, Object[] parameters) {
			switch (levelFlag) {
			case LoggingLevels.TRACE_FLAG:
				DELEGATE.trace(t, message, parameters);
				break;
			case LoggingLevels.DEBUG_FLAG:
				DELEGATE.debug(t, message, parameters);
				break;
			case LoggingLevels.INFO_FLAG:
				DELEGATE.info(t, message, parameters);
				break;
			case LoggingLevels.WARN_FLAG:
				DELEGATE.warn(t, message, parameters);
				break;
			case LoggingLevels.ERROR_FLAG:
				DELEGATE.error(t, message, parameters);
				break;
			case LoggingLevels.FATAL_FLAG:
				DELEGATE.fatal(t, message, parameters);
				break;
			}
		}

	}

	private static final class AsynchronousLogger implements Logger {

		private final LoggingManager loggingManager;

		private AsynchronousLogger(LoggingLevels levels) {
			loggingManager = new LoggingManager(levels);
			loggingManager.start();
		}

		/**
		 * @see net.sf.xenqtt.Log.Logger#log(int, java.lang.String, java.lang.Object[])
		 */
		@Override
		public void log(int levelFlag, String message, Object... parameters) {
			loggingManager.offerWork(new LogMessage(levelFlag, message, parameters));
		}

		/**
		 * @see net.sf.xenqtt.Log.Logger#log(int, java.lang.Throwable, java.lang.String, java.lang.Object[])
		 */
		@Override
		public void log(int levelFlag, Throwable t, String message, Object... parameters) {
			loggingManager.offerWork(new LogMessage(levelFlag, message, parameters, t));
		}

		private void setLoggingLevels(LoggingLevels levels) {
			loggingManager.offerWork(new LoggingLevelsChange(levels));
		}

		private void setLoggingDestination(String outputFile) {
			loggingManager.offerWork(new LoggingDestinationChange(outputFile));
		}

	}

	private static final class LoggingManager extends Thread {

		private LoggingLevels levels;
		private final BlockingQueue<LogWork> work;

		private LoggingManager(LoggingLevels levels) {
			super("LoggingManager");
			this.levels = levels;
			work = new LinkedBlockingQueue<LogWork>();

			setDaemon(true);
		}

		@Override
		public void run() {
			for (;;) {
				try {
					LogWork presentWork = work.take();
					if (presentWork instanceof LoggingLevelsChange) {
						this.levels = ((LoggingLevelsChange) presentWork).levels;
					} else if (presentWork instanceof LoggingDestinationChange) {
						if (DELEGATE instanceof JavaLoggingDelegate) {
							String outputFile = ((LoggingDestinationChange) presentWork).outputFile;
							((JavaLoggingDelegate) DELEGATE).updateLoggingDestination(outputFile);
						}
					} else {
						logMessage((LogMessage) presentWork);
					}
				} catch (InterruptedException ex) {
					return;
				} catch (Exception ex) {
					System.err.println("Unable to process a message for logging purposes. It will be discarded.");
					ex.printStackTrace();
				}
			}
		}

		private void logMessage(LogMessage message) throws Exception {
			if (!levels.isLoggable(message.levelFlag)) {
				return;
			}

			switch (message.levelFlag) {
			case LoggingLevels.TRACE_FLAG:
				DELEGATE.trace(message.t, message.message, message.parameters);
				break;
			case LoggingLevels.DEBUG_FLAG:
				DELEGATE.debug(message.t, message.message, message.parameters);
				break;
			case LoggingLevels.INFO_FLAG:
				DELEGATE.info(message.t, message.message, message.parameters);
				break;
			case LoggingLevels.WARN_FLAG:
				DELEGATE.warn(message.t, message.message, message.parameters);
				break;
			case LoggingLevels.ERROR_FLAG:
				DELEGATE.error(message.t, message.message, message.parameters);
				break;
			case LoggingLevels.FATAL_FLAG:
				DELEGATE.fatal(message.t, message.message, message.parameters);
				break;
			}
		}

		private void offerWork(LogWork workToOffer) {
			work.offer(workToOffer);
		}

	}

	private static final class ConsoleLoggingDelegate implements LoggingDelegate {

		@Override
		public void trace(String message, Object... parameters) {
			System.out.println(String.format(message, parameters));
		}

		@Override
		public void trace(Throwable t, String message, Object... parameters) {
			System.out.println(String.format(message, parameters));
			if (t != null) {
				t.printStackTrace(System.out);
			}
		}

		@Override
		public void debug(String message, Object... parameters) {
			System.out.println(String.format(message, parameters));
		}

		@Override
		public void debug(Throwable t, String message, Object... parameters) {
			System.out.println(String.format(message, parameters));
			if (t != null) {
				t.printStackTrace(System.out);
			}
		}

		@Override
		public void info(String message, Object... parameters) {
			System.out.println(String.format(message, parameters));
		}

		@Override
		public void info(Throwable t, String message, Object... parameters) {
			System.out.println(String.format(message, parameters));
			if (t != null) {
				t.printStackTrace(System.out);
			}
		}

		@Override
		public void warn(String message, Object... parameters) {
			System.out.println(String.format(message, parameters));
		}

		@Override
		public void warn(Throwable t, String message, Object... parameters) {
			System.out.println(String.format(message, parameters));
			if (t != null) {
				t.printStackTrace(System.out);
			}
		}

		@Override
		public void error(String message, Object... parameters) {
			System.out.println(String.format(message, parameters));
		}

		@Override
		public void error(Throwable t, String message, Object... parameters) {
			System.out.println(String.format(message, parameters));
			if (t != null) {
				t.printStackTrace(System.out);
			}
		}

		@Override
		public void fatal(String message, Object... parameters) {
			System.out.println(String.format(message, parameters));
		}

		@Override
		public void fatal(Throwable t, String message, Object... parameters) {
			System.out.println(String.format(message, parameters));
			if (t != null) {
				t.printStackTrace(System.out);
			}

		}

	}

}
