package net.sf.xenqtt;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * <p>
 * A {@link Formatter} implementation that logs {@link LogRecord log record}s to a single line in the log file. The format of the logged event is as follows:
 * </p>
 * 
 * <code>timestamp [levelUpperCase] - message</code>
 * 
 * <p>
 * An example timestamp would look as-follows:
 * </p>
 * 
 * <code>2013-09-10T19:11:30.567-0500 [INFO] - A cool and useful message</code>
 */
public class XenqttLogFormatter extends Formatter {

	private static final ThreadLocal<DateFormat> ISO_8601_DATE_FORMATTER = new ThreadLocal<DateFormat>() {

		@Override
		protected DateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
		}

	};
	private static final Map<Level, String> LEVELS;

	static {
		LEVELS = new HashMap<Level, String>();
		LEVELS.put(Level.FINEST, "TRACE");
		LEVELS.put(Level.FINER, "TRACE");
		LEVELS.put(Level.FINE, "DEBUG");
		LEVELS.put(Level.CONFIG, "DEBUG");
		LEVELS.put(Level.INFO, "INFO");
		LEVELS.put(Level.WARNING, "WARN");
		LEVELS.put(Level.SEVERE, "ERROR");
	}

	/**
	 * @see java.util.logging.Formatter#format(java.util.logging.LogRecord)
	 */
	@Override
	public String format(LogRecord record) {
		String timestamp = getTimestamp(record.getMillis());
		String level = getLevel(record.getLevel());
		String stackTrace = getStackTraceIfApplicable(record.getThrown());

		return String.format("%s [%s] - %s%s", timestamp, level, record.getMessage(), stackTrace);
	}

	private String getTimestamp(long millis) {
		return ISO_8601_DATE_FORMATTER.get().format(new Date(millis));
	}

	private String getLevel(Level level) {
		String strLevel = LEVELS.get(level);
		if (strLevel == null) {
			strLevel = "N/A";
		}

		return strLevel;
	}

	private String getStackTraceIfApplicable(Throwable thrown) {
		if (thrown == null) {
			return "";
		}

		StringBuilder stackTrace = new StringBuilder("\n");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream stream = new PrintStream(baos);
		thrown.printStackTrace(stream);
		stackTrace.append(new String(baos.toByteArray(), Charset.forName("US-ASCII")));

		return stackTrace.toString();
	}

}
