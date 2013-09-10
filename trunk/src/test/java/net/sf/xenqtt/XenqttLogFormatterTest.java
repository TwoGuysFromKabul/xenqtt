package net.sf.xenqtt;

import static org.junit.Assert.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.Test;

public class XenqttLogFormatterTest {

	XenqttLogFormatter formatter = new XenqttLogFormatter();

	@Test
	public void testFormat_Finest() {
		long now = System.currentTimeMillis();
		String message = "You are the finest baby!";
		LogRecord record = new LogRecord(Level.FINEST, message);
		record.setMillis(now);
		record.setThrown(null);

		String logMessage = formatter.format(record);
		String expected = String.format("%s [%s] - %s", format(now), "TRACE", message);
		assertEquals(expected, logMessage);
	}

	@Test
	public void testFormat_Finer() {
		long now = System.currentTimeMillis();
		String message = "You are finer than that.";
		LogRecord record = new LogRecord(Level.FINER, message);
		record.setMillis(now);
		record.setThrown(null);

		String logMessage = formatter.format(record);
		String expected = String.format("%s [%s] - %s", format(now), "TRACE", message);
		assertEquals(expected, logMessage);
	}

	@Test
	public void testFormat_Fine() {
		long now = System.currentTimeMillis();
		String message = "That will be fine";
		LogRecord record = new LogRecord(Level.FINE, message);
		record.setMillis(now);
		record.setThrown(null);

		String logMessage = formatter.format(record);
		String expected = String.format("%s [%s] - %s", format(now), "DEBUG", message);
		assertEquals(expected, logMessage);
	}

	@Test
	public void testFormat_Config() {
		long now = System.currentTimeMillis();
		String message = "That is the incorrect configuration.";
		LogRecord record = new LogRecord(Level.CONFIG, message);
		record.setMillis(now);
		record.setThrown(null);

		String logMessage = formatter.format(record);
		String expected = String.format("%s [%s] - %s", format(now), "DEBUG", message);
		assertEquals(expected, logMessage);
	}

	@Test
	public void testFormat_Info() {
		long now = System.currentTimeMillis();
		String message = "I am an information nut!";
		LogRecord record = new LogRecord(Level.INFO, message);
		record.setMillis(now);
		record.setThrown(null);

		String logMessage = formatter.format(record);
		String expected = String.format("%s [%s] - %s", format(now), "INFO", message);
		assertEquals(expected, logMessage);
	}

	@Test
	public void testFormat_Warning() {
		long now = System.currentTimeMillis();
		String message = "I'm warning you...";
		LogRecord record = new LogRecord(Level.WARNING, message);
		record.setMillis(now);
		record.setThrown(null);

		String logMessage = formatter.format(record);
		String expected = String.format("%s [%s] - %s", format(now), "WARN", message);
		assertEquals(expected, logMessage);
	}

	@Test
	public void testFormat_Warning_Exception() {
		long now = System.currentTimeMillis();
		String message = "I'm warning you...";
		LogRecord record = new LogRecord(Level.WARNING, message);
		record.setMillis(now);
		record.setThrown(new RuntimeException("I warned you."));

		String logMessage = formatter.format(record);
		String expected = String.format("%s [%s] - %s", format(now), "WARN", message);
		String[] components = logMessage.split("[\r\n]+");
		assertEquals(expected, components[0]);
		assertTrue(components[1].contains("I warned you."));
	}

	@Test
	public void testFormat_Severe() {
		long now = System.currentTimeMillis();
		String message = "The threat level is severe.";
		LogRecord record = new LogRecord(Level.SEVERE, message);
		record.setMillis(now);
		record.setThrown(null);

		String logMessage = formatter.format(record);
		String expected = String.format("%s [%s] - %s", format(now), "ERROR", message);
		assertEquals(expected, logMessage);
	}

	@Test
	public void testFormat_Severe_Exception() {
		long now = System.currentTimeMillis();
		String message = "The threat level is severe.";
		LogRecord record = new LogRecord(Level.SEVERE, message);
		record.setMillis(now);
		record.setThrown(new RuntimeException("The severity is extreme!"));

		String logMessage = formatter.format(record);
		String expected = String.format("%s [%s] - %s", format(now), "ERROR", message);
		String[] components = logMessage.split("[\r\n]+");
		assertEquals(expected, components[0]);
		assertTrue(components[1].contains("The severity is extreme!"));
	}

	private String format(long now) {
		return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US).format(new Date(now));
	}

}
