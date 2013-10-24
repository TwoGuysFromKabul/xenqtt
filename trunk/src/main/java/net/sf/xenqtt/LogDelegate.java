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
 * Implementations are delegated to by {@link Log}.
 */
interface LogDelegate {

	/**
	 * Log a message at the TRACE log level, if possible.
	 * 
	 * @param message
	 *            The log message to emit
	 * @param parameters
	 *            The parameters to use in replacing format specifiers in the specified {@code message}. This can be omitted if no such specifiers exist
	 */
	void trace(String message, Object... parameters);

	/**
	 * Log a message at the DEBUG log level, if possible.
	 * 
	 * @param message
	 *            The log message to emit
	 * @param parameters
	 *            The parameters to use in replacing format specifiers in the specified {@code message}. This can be omitted if no such specifiers exist
	 */
	void debug(String message, Object... parameters);

	/**
	 * Log a message at the INFO log level, if possible.
	 * 
	 * @param message
	 *            The log message to emit
	 * @param parameters
	 *            The parameters to use in replacing format specifiers in the specified {@code message}. This can be omitted if no such specifiers exist
	 */
	void info(String message, Object... parameters);

	/**
	 * Log a message at the WARN log level, if possible.
	 * 
	 * @param message
	 *            The log message to emit
	 * @param parameters
	 *            The parameters to use in replacing format specifiers in the specified {@code message}. This can be omitted if no such specifiers exist
	 */
	void warn(String message, Object... parameters);

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
	void warn(Throwable t, String message, Object... parameters);

	/**
	 * Log a message at the ERROR log level, if possible.
	 * 
	 * @param message
	 *            The log message to emit
	 * @param parameters
	 *            The parameters to use in replacing format specifiers in the specified {@code message}. This can be omitted if no such specifiers exist
	 */
	void error(String message, Object... parameters);

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
	void error(Throwable t, String message, Object... parameters);

	/**
	 * Log a message at the FATAL log level.
	 * 
	 * @param message
	 *            The log message to emit
	 * @param parameters
	 *            The parameters to use in replacing format specifiers in the specified {@code message}. This can be omitted if no such specifiers exist
	 */
	void fatal(String message, Object... parameters);

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
	void fatal(Throwable t, String message, Object... parameters);
}
