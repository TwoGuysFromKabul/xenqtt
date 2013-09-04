package net.sf.xenqtt.util;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class LogTest {

	static final int TRACE_AND_ABOVE = 0x3f;
	static final int DEBUG_AND_ABOVE = 0x3e;
	static final int INFO_AND_ABOVE = 0x3c;
	static final int WARN_AND_ABOVE = 0x38;
	static final int ERROR_AND_ABOVE = 0x30;
	static final int FATAL = 0x20;
	static final LoggingLevels DEFAULT_LOGGING_LEVELS = new LoggingLevels(WARN_AND_ABOVE);
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	PrintStream out = new PrintStream(baos);
	PrintStream err = new PrintStream(baos);

	@Before
	public void setup() {
		Log.setLoggingLevels(DEFAULT_LOGGING_LEVELS);
		System.setOut(out);
		System.setErr(err);
	}

	@Test
	public void testTrace() {
		Log.setLoggingLevels(toLoggingLevels(TRACE_AND_ABOVE));
		Log.trace("Message: %s", "Trace");
		Log.debug("Message: %s", "Debug");
		Log.info("Message: %s", "Info");
		Log.warn("Message: %s", "Warn");
		Log.error("Message: %s", "Error");
		Log.fatal("Message: %s", "Fatal");

		String messages = new String(baos.toByteArray(), Charset.forName("US-ASCII"));
		String[] individualMessages = messages.split("[\r\n]+");
		assertEquals(6, individualMessages.length);

		List<String> expected = new ArrayList<String>(Arrays.asList(new String[] { "Message: Trace", "Message: Debug", "Message: Info", "Message: Warn",
				"Message: Error", "Message: Fatal" }));
		for (String individualMessage : individualMessages) {
			expected.remove(individualMessage);
		}
		assertTrue(expected.isEmpty());
	}

	@Test
	public void testDebug() {
		Log.setLoggingLevels(toLoggingLevels(DEBUG_AND_ABOVE));
		Log.trace("Message: %s", "Trace");
		Log.debug("Message: %s", "Debug");
		Log.info("Message: %s", "Info");
		Log.warn("Message: %s", "Warn");
		Log.error("Message: %s", "Error");
		Log.fatal("Message: %s", "Fatal");

		String messages = new String(baos.toByteArray(), Charset.forName("US-ASCII"));
		String[] individualMessages = messages.split("[\r\n]+");
		assertEquals(5, individualMessages.length);

		List<String> expected = new ArrayList<String>(Arrays.asList(new String[] { "Message: Debug", "Message: Info", "Message: Warn", "Message: Error",
				"Message: Fatal" }));
		for (String individualMessage : individualMessages) {
			expected.remove(individualMessage);
		}
		assertTrue(expected.isEmpty());
	}

	@Test
	public void testInfo() {
		Log.setLoggingLevels(toLoggingLevels(INFO_AND_ABOVE));
		Log.trace("Message: %s", "Trace");
		Log.debug("Message: %s", "Debug");
		Log.info("Message: %s", "Info");
		Log.warn("Message: %s", "Warn");
		Log.error("Message: %s", "Error");
		Log.fatal("Message: %s", "Fatal");

		String messages = new String(baos.toByteArray(), Charset.forName("US-ASCII"));
		String[] individualMessages = messages.split("[\r\n]+");
		assertEquals(4, individualMessages.length);

		List<String> expected = new ArrayList<String>(Arrays.asList(new String[] { "Message: Info", "Message: Warn", "Message: Error", "Message: Fatal" }));
		for (String individualMessage : individualMessages) {
			expected.remove(individualMessage);
		}
		assertTrue(expected.isEmpty());
	}

	@Test
	public void testWarn() {
		Log.setLoggingLevels(toLoggingLevels(WARN_AND_ABOVE));
		Log.trace("Message: %s", "Trace");
		Log.debug("Message: %s", "Debug");
		Log.info("Message: %s", "Info");
		Log.warn("Message: %s", "Warn");
		Log.error("Message: %s", "Error");
		Log.fatal("Message: %s", "Fatal");

		String messages = new String(baos.toByteArray(), Charset.forName("US-ASCII"));
		String[] individualMessages = messages.split("[\r\n]+");
		assertEquals(3, individualMessages.length);

		List<String> expected = new ArrayList<String>(Arrays.asList(new String[] { "Message: Warn", "Message: Error", "Message: Fatal" }));
		for (String individualMessage : individualMessages) {
			expected.remove(individualMessage);
		}
		assertTrue(expected.isEmpty());
	}

	@Test
	public void testWarn_WithException() {
		Log.setLoggingLevels(toLoggingLevels(WARN_AND_ABOVE));
		Log.warn(new RuntimeException("Doeth!"), "Message: %s", "Warn");

		String messages = new String(baos.toByteArray(), Charset.forName("US-ASCII"));
		String[] individualMessages = messages.split("[\r\n]+");
		assertEquals("Message: Warn", individualMessages[0]);
		assertEquals("java.lang.RuntimeException: Doeth!", individualMessages[1]);
	}

	@Test
	public void testError() {
		Log.setLoggingLevels(toLoggingLevels(ERROR_AND_ABOVE));
		Log.trace("Message: %s", "Trace");
		Log.debug("Message: %s", "Debug");
		Log.info("Message: %s", "Info");
		Log.warn("Message: %s", "Warn");
		Log.error("Message: %s", "Error");
		Log.fatal("Message: %s", "Fatal");

		String messages = new String(baos.toByteArray(), Charset.forName("US-ASCII"));
		String[] individualMessages = messages.split("[\r\n]+");
		assertEquals(2, individualMessages.length);

		List<String> expected = new ArrayList<String>(Arrays.asList(new String[] { "Message: Error", "Message: Fatal" }));
		for (String individualMessage : individualMessages) {
			expected.remove(individualMessage);
		}
		assertTrue(expected.isEmpty());
	}

	@Test
	public void testError_WithException() {
		Log.setLoggingLevels(toLoggingLevels(ERROR_AND_ABOVE));
		Log.error(new RuntimeException("Doeth!"), "Message: %s", "Error");

		String messages = new String(baos.toByteArray(), Charset.forName("US-ASCII"));
		String[] individualMessages = messages.split("[\r\n]+");
		assertEquals("Message: Error", individualMessages[0]);
		assertEquals("java.lang.RuntimeException: Doeth!", individualMessages[1]);
	}

	@Test
	public void testFatal() {
		Log.setLoggingLevels(toLoggingLevels(FATAL));
		Log.trace("Message: %s", "Trace");
		Log.debug("Message: %s", "Debug");
		Log.info("Message: %s", "Info");
		Log.warn("Message: %s", "Warn");
		Log.error("Message: %s", "Error");
		Log.fatal("Message: %s", "Fatal");

		String messages = new String(baos.toByteArray(), Charset.forName("US-ASCII"));
		String[] individualMessages = messages.split("[\r\n]+");
		assertEquals(1, individualMessages.length);

		List<String> expected = new ArrayList<String>(Arrays.asList(new String[] { "Message: Fatal" }));
		for (String individualMessage : individualMessages) {
			expected.remove(individualMessage);
		}
		assertTrue(expected.isEmpty());
	}

	@Test
	public void testFatal_WithException() {
		Log.setLoggingLevels(toLoggingLevels(FATAL));
		Log.fatal(new RuntimeException("Doeth!"), "Message: %s", "Fatal");

		String messages = new String(baos.toByteArray(), Charset.forName("US-ASCII"));
		String[] individualMessages = messages.split("[\r\n]+");
		assertEquals("Message: Fatal", individualMessages[0]);
		assertEquals("java.lang.RuntimeException: Doeth!", individualMessages[1]);
	}

	private LoggingLevels toLoggingLevels(int flags) {
		return new LoggingLevels(flags);
	}

}
