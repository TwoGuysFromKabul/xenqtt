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
package net.xenqtt;

/**
 * Exposes the disparate log levels that are currently enabled for Xenqtt.
 */
public final class LoggingLevels {

	public static final int TRACE_FLAG = 0x01;
	public static final int DEBUG_FLAG = 0x02;
	public static final int INFO_FLAG = 0x04;
	public static final int WARN_FLAG = 0x08;
	public static final int ERROR_FLAG = 0x10;
	public static final int FATAL_FLAG = 0x20;
	public static final int DEFAULT_LOGGING_LEVELS = WARN_FLAG | ERROR_FLAG | FATAL_FLAG;

	public final boolean traceEnabled;
	public final boolean debugEnabled;
	public final boolean infoEnabled;
	public final boolean warnEnabled;
	public final boolean errorEnabled;
	public final boolean fatalEnabled;
	private final int flags;

	/**
	 * Create a new instance of this class.
	 * 
	 * @param traceEnabled
	 *            Whether or not trace logging is enabled
	 * @param debugEnabled
	 *            Whether or not debug logging is enabled
	 * @param infoEnabled
	 *            Whether or not info logging is enabled
	 * @param warnEnabled
	 *            Whether or not warn logging is enabled
	 * @param errorEnabled
	 *            Whether or not error logging is enabled
	 * @param fatalEnabled
	 *            Whether or not fatal logging is enabled
	 */
	public LoggingLevels(boolean traceEnabled, boolean debugEnabled, boolean infoEnabled, boolean warnEnabled, boolean errorEnabled, boolean fatalEnabled) {
		this.traceEnabled = traceEnabled;
		this.debugEnabled = debugEnabled;
		this.infoEnabled = infoEnabled;
		this.warnEnabled = warnEnabled;
		this.errorEnabled = errorEnabled;
		this.fatalEnabled = fatalEnabled;
		flags = flags();
	}

	/**
	 * Create a new instance of this class.
	 * 
	 * @param flags
	 *            Flags that define which logging levels are enabled. The following specifies which flags map to which values:
	 * 
	 *            <pre>
	 * F = Fatal
	 * E = Error
	 * W = Warn
	 * I = Info
	 * D = Debug
	 * T = Trace
	 * -----------------------------------------
	 * |...| 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
	 * -----------------------------------------
	 * |...| X | X | X | F | E | W | I | D | T |
	 * -----------------------------------------
	 * </pre>
	 */
	public LoggingLevels(int flags) {
		this.flags = flags;
		traceEnabled = (flags & 0x01) != 0;
		debugEnabled = (flags & 0x02) != 0;
		infoEnabled = (flags & 0x04) != 0;
		warnEnabled = (flags & 0x08) != 0;
		errorEnabled = (flags & 0x10) != 0;
		fatalEnabled = (flags & 0x20) != 0;
	}

	/**
	 * @return Flags indicating which logging levels are currently enabled. The following describes the flags that will be set depending on logging levels:
	 * 
	 *         <pre>
	 * F = Fatal
	 * E = Error
	 * W = Warn
	 * I = Info
	 * D = Debug
	 * T = Trace
	 * -----------------------------------------
	 * |...| 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
	 * -----------------------------------------
	 * |...| X | X | X | F | E | W | I | D | T |
	 * -----------------------------------------
	 * </pre>
	 */
	public int flags() {
		int flags = 0x00;
		flags |= traceEnabled ? 0x01 : 0x00;
		flags |= debugEnabled ? 0x02 : 0x00;
		flags |= infoEnabled ? 0x04 : 0x00;
		flags |= warnEnabled ? 0x08 : 0x00;
		flags |= errorEnabled ? 0x10 : 0x00;
		flags |= fatalEnabled ? 0x20 : 0x00;

		return flags;
	}

	/**
	 * Determine if an event is loggable.
	 * 
	 * @param flag
	 *            The level flag to use in determining if an event is loggable
	 * 
	 * @return {@code true} if the event is loggable, {@code false} if it is not
	 */
	public boolean isLoggable(int flag) {
		return (flags & flag) != 0;
	}

}
