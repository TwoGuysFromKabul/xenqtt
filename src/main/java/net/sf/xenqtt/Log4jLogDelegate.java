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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Log4J based implementation of {@link LogDelegate}
 */
final class Log4jLogDelegate implements LogDelegate {

	private final Logger log = Logger.getLogger("xenqtt");
	private final boolean traceEnabled;
	private final boolean debugEnabled;
	private final boolean infoEnabled;
	private final boolean warnEnabled;
	private final boolean errorEnabled;
	private final boolean fatalEnabled;

	Log4jLogDelegate() {
		Level effectiveLevel = log.getEffectiveLevel();
		traceEnabled = Level.TRACE.isGreaterOrEqual(effectiveLevel);
		debugEnabled = Level.DEBUG.isGreaterOrEqual(effectiveLevel);
		infoEnabled = Level.INFO.isGreaterOrEqual(effectiveLevel);
		warnEnabled = Level.WARN.isGreaterOrEqual(effectiveLevel);
		errorEnabled = Level.ERROR.isGreaterOrEqual(effectiveLevel);
		fatalEnabled = Level.FATAL.isGreaterOrEqual(effectiveLevel);
	}

	/**
	 * Log a message at the TRACE log level, if possible.
	 * 
	 * @param message
	 *            The log message to emit
	 * @param parameters
	 *            The parameters to use in replacing format specifiers in the specified {@code message}. This can be omitted if no such specifiers exist
	 */
	@Override
	public void trace(String message, Object... parameters) {
		if (traceEnabled) {
			log.trace(String.format(message, parameters));
		}
	}

	/**
	 * Log a message at the DEBUG log level, if possible.
	 * 
	 * @param message
	 *            The log message to emit
	 * @param parameters
	 *            The parameters to use in replacing format specifiers in the specified {@code message}. This can be omitted if no such specifiers exist
	 */
	@Override
	public void debug(String message, Object... parameters) {
		if (debugEnabled) {
			log.debug(String.format(message, parameters));
		}
	}

	/**
	 * Log a message at the INFO log level, if possible.
	 * 
	 * @param message
	 *            The log message to emit
	 * @param parameters
	 *            The parameters to use in replacing format specifiers in the specified {@code message}. This can be omitted if no such specifiers exist
	 */
	@Override
	public void info(String message, Object... parameters) {
		if (infoEnabled) {
			log.info(String.format(message, parameters));
		}
	}

	/**
	 * Log a message at the WARN log level, if possible.
	 * 
	 * @param message
	 *            The log message to emit
	 * @param parameters
	 *            The parameters to use in replacing format specifiers in the specified {@code message}. This can be omitted if no such specifiers exist
	 */
	@Override
	public void warn(String message, Object... parameters) {
		if (warnEnabled) {
			log.warn(String.format(message, parameters));
		}
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
	@Override
	public void warn(Throwable t, String message, Object... parameters) {
		if (warnEnabled) {
			log.warn(String.format(message, parameters), t);
		}
	}

	/**
	 * Log a message at the ERROR log level, if possible.
	 * 
	 * @param message
	 *            The log message to emit
	 * @param parameters
	 *            The parameters to use in replacing format specifiers in the specified {@code message}. This can be omitted if no such specifiers exist
	 */
	@Override
	public void error(String message, Object... parameters) {
		if (errorEnabled) {
			log.error(String.format(message, parameters));
		}
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
	@Override
	public void error(Throwable t, String message, Object... parameters) {
		if (errorEnabled) {
			log.error(String.format(message, parameters), t);
		}
	}

	/**
	 * Log a message at the FATAL log level.
	 * 
	 * @param message
	 *            The log message to emit
	 * @param parameters
	 *            The parameters to use in replacing format specifiers in the specified {@code message}. This can be omitted if no such specifiers exist
	 */
	@Override
	public void fatal(String message, Object... parameters) {
		if (fatalEnabled) {
			log.fatal(String.format(message, parameters));
		}
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
	@Override
	public void fatal(Throwable t, String message, Object... parameters) {
		if (fatalEnabled) {
			log.fatal(String.format(message, parameters), t);
		}
	}

}
