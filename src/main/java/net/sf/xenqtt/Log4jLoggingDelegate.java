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
	 * @see net.sf.xenqtt.Log.LoggingDelegate#trace(java.lang.String, java.lang.Object[])
	 */
	@Override
	public void trace(String message, Object... parameters) {
		log.trace(String.format(message, parameters));
	}

	/**
	 * @see net.sf.xenqtt.Log.LoggingDelegate#trace(java.lang.Throwable, java.lang.String, java.lang.Object[])
	 */
	@Override
	public void trace(Throwable t, String message, Object... parameters) {
		log.trace(String.format(message, parameters), t);
	}

	/**
	 * @see net.sf.xenqtt.Log.LoggingDelegate#debug(java.lang.String, java.lang.Object[])
	 */
	@Override
	public void debug(String message, Object... parameters) {
		log.debug(String.format(message, parameters));
	}

	/**
	 * @see net.sf.xenqtt.Log.LoggingDelegate#debug(java.lang.Throwable, java.lang.String, java.lang.Object[])
	 */
	@Override
	public void debug(Throwable t, String message, Object... parameters) {
		log.debug(String.format(message, parameters), t);
	}

	/**
	 * @see net.sf.xenqtt.Log.LoggingDelegate#info(java.lang.String, java.lang.Object[])
	 */
	@Override
	public void info(String message, Object... parameters) {
		log.info(String.format(message, parameters));
	}

	/**
	 * @see net.sf.xenqtt.Log.LoggingDelegate#info(java.lang.Throwable, java.lang.String, java.lang.Object[])
	 */
	@Override
	public void info(Throwable t, String message, Object... parameters) {
		log.info(String.format(message, parameters), t);
	}

	/**
	 * @see net.sf.xenqtt.Log.LoggingDelegate#warn(java.lang.String, java.lang.Object[])
	 */
	@Override
	public void warn(String message, Object... parameters) {
		log.warn(String.format(message, parameters));
	}

	/**
	 * @see net.sf.xenqtt.Log.LoggingDelegate#warn(java.lang.Throwable, java.lang.String, java.lang.Object[])
	 */
	@Override
	public void warn(Throwable t, String message, Object... parameters) {
		log.warn(String.format(message, parameters), t);
	}

	/**
	 * @see net.sf.xenqtt.Log.LoggingDelegate#error(java.lang.String, java.lang.Object[])
	 */
	@Override
	public void error(String message, Object... parameters) {
		log.error(String.format(message, parameters));
	}

	/**
	 * @see net.sf.xenqtt.Log.LoggingDelegate#error(java.lang.Throwable, java.lang.String, java.lang.Object[])
	 */
	@Override
	public void error(Throwable t, String message, Object... parameters) {
		log.error(String.format(message, parameters), t);
	}

	/**
	 * @see net.sf.xenqtt.Log.LoggingDelegate#fatal(java.lang.String, java.lang.Object[])
	 */
	@Override
	public void fatal(String message, Object... parameters) {
		log.fatal(String.format(message, parameters));
	}

	/**
	 * @see net.sf.xenqtt.Log.LoggingDelegate#fatal(java.lang.Throwable, java.lang.String, java.lang.Object[])
	 */
	@Override
	public void fatal(Throwable t, String message, Object... parameters) {
		log.fatal(String.format(message, parameters), t);
	}

}
