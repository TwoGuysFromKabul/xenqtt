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
 * Specifies a type to which logging events are delegated. Specific implementations will log in different ways depending on how they are implemented.
 */
interface LoggingDelegate {

	/**
	 * Log an event at the trace level.
	 * 
	 * @param message
	 *            The message to log
	 */
	void trace(String message);

	/**
	 * Log an event at the trace level.
	 * 
	 * @param t
	 *            A {@link Throwable} to associate with the event
	 * @param message
	 *            The message to log
	 */
	void trace(Throwable t, String message);

	/**
	 * Log an event at the debug level.
	 * 
	 * @param message
	 *            The message to log
	 */
	void debug(String message);

	/**
	 * Log an event at the debug level.
	 * 
	 * @param t
	 *            A {@link Throwable} to associate with the event
	 * @param message
	 *            The message to log
	 */
	void debug(Throwable t, String message);

	/**
	 * Log an event at the info level.
	 * 
	 * @param message
	 *            The message to log
	 */
	void info(String message);

	/**
	 * Log an event at the info level.
	 * 
	 * @param t
	 *            A {@link Throwable} to associate with the event
	 * @param message
	 *            The message to log
	 */
	void info(Throwable t, String message);

	/**
	 * Log an event at the warn level.
	 * 
	 * @param message
	 *            The message to log
	 */
	void warn(String message);

	/**
	 * Log an event at the warn level.
	 * 
	 * @param t
	 *            A {@link Throwable} to associate with the event
	 * @param message
	 *            The message to log
	 */
	void warn(Throwable t, String message);

	/**
	 * Log an event at the error level.
	 * 
	 * @param message
	 *            The message to log
	 */
	void error(String message);

	/**
	 * Log an event at the error level.
	 * 
	 * @param t
	 *            A {@link Throwable} to associate with the event
	 * @param message
	 *            The message to log
	 */
	void error(Throwable t, String message);

	/**
	 * Log an event at the fatal level.
	 * 
	 * @param message
	 *            The message to log
	 */
	void fatal(String message);

	/**
	 * Log an event at the fatal level.
	 * 
	 * @param t
	 *            A {@link Throwable} to associate with the event
	 * @param message
	 *            The message to log
	 */
	void fatal(Throwable t, String message);

}
