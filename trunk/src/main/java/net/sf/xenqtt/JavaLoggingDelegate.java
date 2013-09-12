package net.sf.xenqtt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import net.sf.xenqtt.Log.LoggingDelegate;

/**
 * A {@link LoggingDelegate} that logs messages and related data to a log file called {@code xenqtt.log} that is created and rolled in the directory where the
 * {@code xenqtt.jar} file is being executed. The logging is done via the Java Util Logging API.
 */
public class JavaLoggingDelegate implements LoggingDelegate {

	static {
		if (!isJavaUtilLoggingEnabled()) {
			LogManager logManager = LogManager.getLogManager();
			try {
				Properties properties = new Properties();
				properties.setProperty("handlers", "java.util.logging.FileHandler");
				properties.setProperty(".level", "ALL"); // Actual logging levels will be controlled by the Log class.
				properties.setProperty("java.util.logging.FileHandler.limit", "5368709120");
				properties.setProperty("java.util.logging.FileHandler.count", "20");
				properties.setProperty("java.util.logging.FileHandler.formatter", "net.sf.xenqtt.XenqttLogFormatter");
				String jarDirectory = getDirectoryHostingRunningXenqttJar();
				if (jarDirectory != null) {
					properties.setProperty("java.util.logging.FileHandler.pattern", String.format("%s/xenqtt.log", jarDirectory));
				} else {
					properties.setProperty("java.util.logging.FileHandler.pattern", "%h/xenqtt.log");
				}
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				properties.store(baos, null);

				ByteArrayInputStream bain = new ByteArrayInputStream(baos.toByteArray());
				logManager.readConfiguration(bain);
			} catch (Exception ex) {
				System.err.println("Unable to load the logging configuration for JUL.");
				ex.printStackTrace();
			}
		}
	}

	private static boolean isJavaUtilLoggingEnabled() {
		return System.getProperty("java.util.logging.config.file") != null || System.getProperty("java.util.logging.config.class") != null;
	}

	private static String getDirectoryHostingRunningXenqttJar() {
		URL url = JavaLoggingDelegate.class.getResource("/" + JavaLoggingDelegate.class.getName().replace('.', '/') + ".class");
		if (url == null) {
			return null;
		}

		String path = url.getPath();
		int startIndex = path.indexOf(":");
		if (startIndex >= 0) {
			path = path.substring(startIndex + 1);
		}

		int bangIndex = path.indexOf("jar!");
		if (bangIndex >= 0) {
			String jarFile = path.substring(0, bangIndex + 3);
			int pos = jarFile.lastIndexOf('/');
			if (pos > -1) {
				return jarFile.substring(0, pos);
			}
		}

		return null;
	}

	private final Logger log = Logger.getLogger("xenqtt");

	/**
	 * @see net.sf.xenqtt.Log.LoggingDelegate#trace(java.lang.String, java.lang.Object[])
	 */
	@Override
	public void trace(String message, Object... parameters) {
		log.finer(String.format(message, parameters));
	}

	/**
	 * @see net.sf.xenqtt.Log.LoggingDelegate#trace(java.lang.Throwable, java.lang.String, java.lang.Object[])
	 */
	@Override
	public void trace(Throwable t, String message, Object... parameters) {
		log.log(Level.FINER, String.format(message, parameters), t);
	}

	/**
	 * @see net.sf.xenqtt.Log.LoggingDelegate#debug(java.lang.String, java.lang.Object[])
	 */
	@Override
	public void debug(String message, Object... parameters) {
		log.fine(String.format(message, parameters));
	}

	/**
	 * @see net.sf.xenqtt.Log.LoggingDelegate#debug(java.lang.Throwable, java.lang.String, java.lang.Object[])
	 */
	@Override
	public void debug(Throwable t, String message, Object... parameters) {
		log.log(Level.FINE, String.format(message, parameters), t);
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
		log.log(Level.INFO, String.format(message, parameters), t);
	}

	/**
	 * @see net.sf.xenqtt.Log.LoggingDelegate#warn(java.lang.String, java.lang.Object[])
	 */
	@Override
	public void warn(String message, Object... parameters) {
		log.warning(String.format(message, parameters));
	}

	/**
	 * @see net.sf.xenqtt.Log.LoggingDelegate#warn(java.lang.Throwable, java.lang.String, java.lang.Object[])
	 */
	@Override
	public void warn(Throwable t, String message, Object... parameters) {
		log.log(Level.WARNING, String.format(message, parameters), t);
	}

	/**
	 * @see net.sf.xenqtt.Log.LoggingDelegate#error(java.lang.String, java.lang.Object[])
	 */
	@Override
	public void error(String message, Object... parameters) {
		log.severe(String.format(message, parameters));
	}

	/**
	 * @see net.sf.xenqtt.Log.LoggingDelegate#error(java.lang.Throwable, java.lang.String, java.lang.Object[])
	 */
	@Override
	public void error(Throwable t, String message, Object... parameters) {
		log.log(Level.SEVERE, String.format(message, parameters), t);
	}

	/**
	 * @see net.sf.xenqtt.Log.LoggingDelegate#fatal(java.lang.String, java.lang.Object[])
	 */
	@Override
	public void fatal(String message, Object... parameters) {
		log.severe(String.format(message, parameters));
	}

	/**
	 * @see net.sf.xenqtt.Log.LoggingDelegate#fatal(java.lang.Throwable, java.lang.String, java.lang.Object[])
	 */
	@Override
	public void fatal(Throwable t, String message, Object... parameters) {
		log.log(Level.SEVERE, String.format(message, parameters), t);
	}

}
