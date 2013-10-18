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
 * A {@link Logger} implementation that delegates log events to a specified {@link LoggingDelegate delegate} synchronously.
 */
final class SynchronousLogger implements Logger {

	private final LoggingLevels levels;
	private final LoggingDelegate delegate;

	/**
	 * Create a new instance of this class.
	 * 
	 * @param levels
	 *            The {@link LoggingLevels logging levels} to use in this {@link SynchronousLogger logger}
	 * @param delegate
	 *            The {@link LoggingDelegate delegate} that all requests for logging will be routed to
	 */
	SynchronousLogger(LoggingLevels levels, LoggingDelegate delegate) {
		this.levels = levels;
		this.delegate = delegate;
	}

	/**
	 * @see net.sf.xenqtt.Logger#init()
	 */
	@Override
	public void init() {
	}

	/**
	 * @see net.sf.xenqtt.Logger#shutdown()
	 */
	@Override
	public void shutdown() {
	}

	/**
	 * @see net.sf.xenqtt.Logger#log(int, java.lang.String, java.lang.Object[])
	 */
	@Override
	public void log(int levelFlag, String message, Object... parameters) {
		if (!levels.isLoggable(levelFlag)) {
			return;
		}

		doLog(levelFlag, null, message, parameters);
	}

	/**
	 * @see net.sf.xenqtt.Logger#log(int, java.lang.Throwable, java.lang.String, java.lang.Object[])
	 */
	@Override
	public void log(int levelFlag, Throwable t, String message, Object... parameters) {
		if (!levels.isLoggable(levelFlag)) {
			return;
		}

		doLog(levelFlag, t, message, parameters);
	}

	private void doLog(int levelFlag, Throwable t, String msg, Object[] parameters) {
		String message = String.format(msg, parameters);
		switch (levelFlag) {
		case LoggingLevels.TRACE_FLAG:
			delegate.trace(t, message);
			break;
		case LoggingLevels.DEBUG_FLAG:
			delegate.debug(t, message);
			break;
		case LoggingLevels.INFO_FLAG:
			delegate.info(t, message);
			break;
		case LoggingLevels.WARN_FLAG:
			delegate.warn(t, message);
			break;
		case LoggingLevels.ERROR_FLAG:
			delegate.error(t, message);
			break;
		case LoggingLevels.FATAL_FLAG:
			delegate.fatal(t, message);
			break;
		}
	}

}
