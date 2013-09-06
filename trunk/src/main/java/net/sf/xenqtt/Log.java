package net.sf.xenqtt;

import java.lang.reflect.Method;
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
	private static final BlockingQueue<LogWork> workQueue = new LinkedBlockingQueue<LogWork>();
	private static final AsynchLogger ASYNCH_LOGGER;

	static {
		DELEGATE = createDelegate();
		LoggingLevels levels = determineLoggingLevels();
		ASYNCH_LOGGER = new AsynchLogger(levels);
		ASYNCH_LOGGER.start();
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

	/**
	 * Set the {@link LoggingLevels logging levels} to use.
	 * 
	 * @param levels
	 *            The logging levels to use
	 */
	public static void setLoggingLevels(LoggingLevels levels) {
		LoggingLevelsChange levelChange = new LoggingLevelsChange(levels);
		workQueue.offer(levelChange);
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
		workQueue.offer(new LogMessage("trace", LoggingLevels.TRACE_FLAG, message, parameters));
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
		workQueue.offer(new LogMessage("debug", LoggingLevels.DEBUG_FLAG, message, parameters));
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
		workQueue.offer(new LogMessage("info", LoggingLevels.INFO_FLAG, message, parameters));
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
		workQueue.offer(new LogMessage("warn", LoggingLevels.WARN_FLAG, message, parameters));
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
		workQueue.offer(new LogMessage("warn", LoggingLevels.WARN_FLAG, message, parameters, t));
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
		workQueue.offer(new LogMessage("error", LoggingLevels.ERROR_FLAG, message, parameters));
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
		workQueue.offer(new LogMessage("error", LoggingLevels.ERROR_FLAG, message, parameters, t));
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
		workQueue.offer(new LogMessage("fatal", LoggingLevels.FATAL_FLAG, message, parameters));
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
		workQueue.offer(new LogMessage("fatal", LoggingLevels.FATAL_FLAG, message, parameters, t));
	}

	/**
	 * Specifies a type to which logging events are delegated. Specific implementations will log in different ways depending on how they are implemented.
	 */
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

	private static abstract class LogWork {

		protected static enum WorkType {

			MESSAGE, LEVELS;

		}

		protected abstract WorkType workType();

	}

	private static final class LogMessage extends LogWork {

		private final String method;
		private final int levelFlag;
		private final String message;
		private final Object[] parameters;
		private final Throwable t;

		private LogMessage(String method, int levelFlag, String message, Object[] parameters) {
			this(method, levelFlag, message, parameters, null);
		}

		private LogMessage(String method, int levelFlag, String message, Object[] parameters, Throwable t) {
			this.method = method;
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

	private static final class AsynchLogger extends Thread {

		private volatile LoggingLevels levels;

		private AsynchLogger(LoggingLevels levels) {
			super("AsynchLogger");
			this.levels = levels;

			setDaemon(true);
		}

		@Override
		public void run() {
			for (;;) {
				try {
					LogWork work = workQueue.take();
					if (work instanceof LoggingLevelsChange) {
						this.levels = ((LoggingLevelsChange) work).levels;
					} else {
						logMessage((LogMessage) work);
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

			Class<?>[] parameterTypes = getParameterTypes(message);
			Method logMethod = DELEGATE.getClass().getMethod(message.method, parameterTypes);
			Object[] args = getArgs(message);
			logMethod.invoke(DELEGATE, args);
		}

		private Class<?>[] getParameterTypes(LogMessage message) {
			if (message.t != null) {
				return new Class<?>[] { Throwable.class, String.class, Object[].class };
			}

			return new Class<?>[] { String.class, Object[].class };
		}

		private Object[] getArgs(LogMessage message) {
			if (message.t != null) {
				return new Object[] { message.t, message.message, message.parameters };
			}

			return new Object[] { message.message, message.parameters };
		}

	}

	private static final class ConsoleLoggingDelegate implements LoggingDelegate {

		/**
		 * @see net.sf.xenqtt.Log.LoggingDelegate#trace(java.lang.String, java.lang.Object[])
		 */
		@Override
		public void trace(String message, Object... parameters) {
			System.out.println(String.format(message, parameters));
		}

		/**
		 * @see net.sf.xenqtt.Log.LoggingDelegate#debug(java.lang.String, java.lang.Object[])
		 */
		@Override
		public void debug(String message, Object... parameters) {
			System.out.println(String.format(message, parameters));
		}

		/**
		 * @see net.sf.xenqtt.Log.LoggingDelegate#info(java.lang.String, java.lang.Object[])
		 */
		@Override
		public void info(String message, Object... parameters) {
			System.out.println(String.format(message, parameters));
		}

		/**
		 * @see net.sf.xenqtt.Log.LoggingDelegate#warn(java.lang.String, java.lang.Object[])
		 */
		@Override
		public void warn(String message, Object... parameters) {
			System.err.println(String.format(message, parameters));
		}

		/**
		 * @see net.sf.xenqtt.Log.LoggingDelegate#warn(java.lang.Throwable, java.lang.String, java.lang.Object[])
		 */
		@Override
		public void warn(Throwable t, String message, Object... parameters) {
			System.err.println(String.format(message, parameters));
			t.printStackTrace();
		}

		/**
		 * @see net.sf.xenqtt.Log.LoggingDelegate#error(java.lang.String, java.lang.Object[])
		 */
		@Override
		public void error(String message, Object... parameters) {
			System.err.println(String.format(message, parameters));
		}

		/**
		 * @see net.sf.xenqtt.Log.LoggingDelegate#error(java.lang.Throwable, java.lang.String, java.lang.Object[])
		 */
		@Override
		public void error(Throwable t, String message, Object... parameters) {
			System.err.println(String.format(message, parameters));
			t.printStackTrace();
		}

		/**
		 * @see net.sf.xenqtt.Log.LoggingDelegate#fatal(java.lang.String, java.lang.Object[])
		 */
		@Override
		public void fatal(String message, Object... parameters) {
			System.err.println(String.format(message, parameters));
		}

		/**
		 * @see net.sf.xenqtt.Log.LoggingDelegate#fatal(java.lang.Throwable, java.lang.String, java.lang.Object[])
		 */
		@Override
		public void fatal(Throwable t, String message, Object... parameters) {
			System.err.println(String.format(message, parameters));
			t.printStackTrace();

		}

	}

}
