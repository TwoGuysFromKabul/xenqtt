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

import net.sf.xenqtt.Log.LoggingDelegate;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * A {@link LoggingDelegate} that delegates logging events to log4j.
 */
final class Log4jLoggingDelegate implements LoggingDelegate {

	private final Logger log = Logger.getLogger("xenqtt");
	private final LoggingLevels levels;

	/**
	 * Create a new instance of this class.
	 */
	Log4jLoggingDelegate() {
		boolean traceEnabled = Level.TRACE.isGreaterOrEqual(log.getEffectiveLevel());
		boolean debugEnabled = Level.DEBUG.isGreaterOrEqual(log.getEffectiveLevel());
		boolean infoEnabled = Level.INFO.isGreaterOrEqual(log.getEffectiveLevel());
		boolean warnEnabled = Level.WARN.isGreaterOrEqual(log.getEffectiveLevel());
		boolean errorEnabled = Level.ERROR.isGreaterOrEqual(log.getEffectiveLevel());
		boolean fatalEnabled = Level.FATAL.isGreaterOrEqual(log.getEffectiveLevel());
		levels = new LoggingLevels(traceEnabled, debugEnabled, infoEnabled, warnEnabled, errorEnabled, fatalEnabled);
	}

	/**
	 * @return The currently-enabled {@link LoggingLevels logging levels}
	 */
	LoggingLevels getLoggingLevels() {
		return levels;
	}

	/**
	 * @see net.sf.xenqtt.Log.LoggingDelegate#trace(java.lang.String)
	 */
	@Override
	public void trace(String message) {
		log.trace(message);
	}

	/**
	 * @see net.sf.xenqtt.Log.LoggingDelegate#trace(java.lang.Throwable, java.lang.String)
	 */
	@Override
	public void trace(Throwable t, String message) {
		log.trace(message, t);
	}

	/**
	 * @see net.sf.xenqtt.Log.LoggingDelegate#debug(java.lang.String)
	 */
	@Override
	public void debug(String message) {
		log.debug(message);
	}

	/**
	 * @see net.sf.xenqtt.Log.LoggingDelegate#debug(java.lang.Throwable, java.lang.String)
	 */
	@Override
	public void debug(Throwable t, String message) {
		log.debug(message, t);
	}

	/**
	 * @see net.sf.xenqtt.Log.LoggingDelegate#info(java.lang.String)
	 */
	@Override
	public void info(String message) {
		log.info(message);
	}

	/**
	 * @see net.sf.xenqtt.Log.LoggingDelegate#info(java.lang.Throwable, java.lang.String)
	 */
	@Override
	public void info(Throwable t, String message) {
		log.info(message, t);
	}

	/**
	 * @see net.sf.xenqtt.Log.LoggingDelegate#warn(java.lang.String)
	 */
	@Override
	public void warn(String message) {
		log.warn(message);
	}

	/**
	 * @see net.sf.xenqtt.Log.LoggingDelegate#warn(java.lang.Throwable, java.lang.String)
	 */
	@Override
	public void warn(Throwable t, String message) {
		log.warn(message, t);
	}

	/**
	 * @see net.sf.xenqtt.Log.LoggingDelegate#error(java.lang.String)
	 */
	@Override
	public void error(String message) {
		log.error(message);
	}

	/**
	 * @see net.sf.xenqtt.Log.LoggingDelegate#error(java.lang.Throwable, java.lang.String)
	 */
	@Override
	public void error(Throwable t, String message) {
		log.error(message, t);
	}

	/**
	 * @see net.sf.xenqtt.Log.LoggingDelegate#fatal(java.lang.String)
	 */
	@Override
	public void fatal(String message) {
		log.fatal(message);
	}

	/**
	 * @see net.sf.xenqtt.Log.LoggingDelegate#fatal(java.lang.Throwable, java.lang.String)
	 */
	@Override
	public void fatal(Throwable t, String message) {
		log.fatal(message, t);
	}

}
