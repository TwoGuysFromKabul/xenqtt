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
 * A {@link Logger} implementation that delegates disparate log events for processing in an asynchronous manner.
 */
final class AsynchronousLogger implements Logger {

	private final LoggingLevels levels;
	private final LoggingManager manager;

	/**
	 * Create a new instance of this class.
	 * 
	 * @param levels
	 *            The {@link LoggingLevels logging levels} to use in this {@link AsynchronousLogger logger}
	 * @param delegate
	 *            The {@link LoggingDelegate delegate} that all requests for logging will be routed to
	 */
	AsynchronousLogger(LoggingLevels levels, LoggingDelegate delegate) {
		this.levels = levels;
		manager = new LoggingManager(delegate);
	}

	/**
	 * @see net.sf.xenqtt.Logger#init()
	 */
	@Override
	public void init() {
		manager.start();
	}

	/**
	 * @see net.sf.xenqtt.Logger#log(int, java.lang.String, java.lang.Object[])
	 */
	@Override
	public void log(int levelFlag, String message, Object... parameters) {
		if (!levels.isLoggable(levelFlag)) {
			return;
		}

		manager.offerMessage(new LogMessage(levelFlag, String.format(message, parameters)));
	}

	/**
	 * @see net.sf.xenqtt.Logger#log(int, java.lang.Throwable, java.lang.String, java.lang.Object[])
	 */
	@Override
	public void log(int levelFlag, Throwable t, String message, Object... parameters) {
		if (!levels.isLoggable(levelFlag)) {
			return;
		}

		manager.offerMessage(new LogMessage(levelFlag, String.format(message, parameters), t));
	}

	private static final class LogMessage {

		private final int levelFlag;
		private final String message;
		private final Throwable t;

		private LogMessage(int levelFlag, String message) {
			this(levelFlag, message, null);
		}

		private LogMessage(int levelFlag, String message, Throwable t) {
			this.levelFlag = levelFlag;
			this.message = message;
			this.t = t;
		}

	}

	private static final class LoggingManager extends Thread {

		private final LoggingDelegate delegate;
		private final BlockingQueue<LogMessage> messages;

		private LoggingManager(LoggingDelegate delegate) {
			this.delegate = delegate;
			messages = new LinkedBlockingQueue<LogMessage>();
		}

		@Override
		public void run() {
			for (;;) {
				try {
					LogMessage message = messages.take();
					log(message);
				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
					return;
				} catch (Exception ex) {
					System.err.println("Unable to log an event.");
					ex.printStackTrace();
				}
			}
		}

		private void log(LogMessage message) {
			switch (message.levelFlag) {
			case LoggingLevels.TRACE_FLAG:
				delegate.trace(message.t, message.message);
				break;
			case LoggingLevels.DEBUG_FLAG:
				delegate.debug(message.t, message.message);
				break;
			case LoggingLevels.INFO_FLAG:
				delegate.info(message.t, message.message);
				break;
			case LoggingLevels.WARN_FLAG:
				delegate.warn(message.t, message.message);
				break;
			case LoggingLevels.ERROR_FLAG:
				delegate.error(message.t, message.message);
				break;
			case LoggingLevels.FATAL_FLAG:
				delegate.fatal(message.t, message.message);
				break;
			}
		}

		private void offerMessage(LogMessage message) {
			messages.offer(message);
		}

	}

}
