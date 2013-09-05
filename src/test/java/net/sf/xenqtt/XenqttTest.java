package net.sf.xenqtt;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.Before;
import org.junit.Test;

public class XenqttTest {

	static final LoggingLevels DEFAULT_LOGGING_LEVELS = new LoggingLevels(0x38);

	@Before
	public void setup() {
		Log.setLoggingLevels(DEFAULT_LOGGING_LEVELS);
		assertEquals(0x38, Log.getLoggingLevels().flags());
	}

	@Test
	public void testMain_WarnLogging() {
		Xenqtt.main(new String[] { "proxy" });
		LoggingLevels levels = Log.getLoggingLevels();
		assertTrue(levels.fatalEnabled);
		assertTrue(levels.errorEnabled);
		assertTrue(levels.warnEnabled);
		assertFalse(levels.infoEnabled);
		assertFalse(levels.debugEnabled);
		assertFalse(levels.traceEnabled);
	}

	@Test
	public void testMain_InfoLogging() {
		Xenqtt.main(new String[] { "proxy", "-v" });
		LoggingLevels levels = Log.getLoggingLevels();
		assertTrue(levels.fatalEnabled);
		assertTrue(levels.errorEnabled);
		assertTrue(levels.warnEnabled);
		assertTrue(levels.infoEnabled);
		assertFalse(levels.debugEnabled);
		assertFalse(levels.traceEnabled);
	}

	@Test
	public void testMain_InfoLogging_Caps() {
		Xenqtt.main(new String[] { "proxy", "-V" });
		LoggingLevels levels = Log.getLoggingLevels();
		assertTrue(levels.fatalEnabled);
		assertTrue(levels.errorEnabled);
		assertTrue(levels.warnEnabled);
		assertTrue(levels.infoEnabled);
		assertFalse(levels.debugEnabled);
		assertFalse(levels.traceEnabled);
	}

	@Test
	public void testMain_DebugLogging_SeparateSwitches() {
		Xenqtt.main(new String[] { "proxy", "-v", "-v" });
		LoggingLevels levels = Log.getLoggingLevels();
		assertTrue(levels.fatalEnabled);
		assertTrue(levels.errorEnabled);
		assertTrue(levels.warnEnabled);
		assertTrue(levels.infoEnabled);
		assertTrue(levels.debugEnabled);
		assertFalse(levels.traceEnabled);
	}

	@Test
	public void testMain_DebugLogging_SeparateSwitches_Caps() {
		Xenqtt.main(new String[] { "proxy", "-V", "-V" });
		LoggingLevels levels = Log.getLoggingLevels();
		assertTrue(levels.fatalEnabled);
		assertTrue(levels.errorEnabled);
		assertTrue(levels.warnEnabled);
		assertTrue(levels.infoEnabled);
		assertTrue(levels.debugEnabled);
		assertFalse(levels.traceEnabled);
	}

	@Test
	public void testMain_DebugLogging_SameSwitch() {
		Xenqtt.main(new String[] { "proxy", "-vv" });
		LoggingLevels levels = Log.getLoggingLevels();
		assertTrue(levels.fatalEnabled);
		assertTrue(levels.errorEnabled);
		assertTrue(levels.warnEnabled);
		assertTrue(levels.infoEnabled);
		assertTrue(levels.debugEnabled);
		assertFalse(levels.traceEnabled);
	}

	@Test
	public void testMain_DebugLogging_SameSwitch_Caps() {
		Xenqtt.main(new String[] { "proxy", "-VV" });
		LoggingLevels levels = Log.getLoggingLevels();
		assertTrue(levels.fatalEnabled);
		assertTrue(levels.errorEnabled);
		assertTrue(levels.warnEnabled);
		assertTrue(levels.infoEnabled);
		assertTrue(levels.debugEnabled);
		assertFalse(levels.traceEnabled);
	}

	@Test
	public void testMain_InvalidMode() throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(baos);
		System.setOut(out);

		Xenqtt.main(new String[] { "proxe", "-vv" });
		assertTrue(new String(baos.toByteArray(), "US-ASCII").startsWith("usage: "));
	}

	@Test
	public void testMain_NoMode() throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(baos);
		System.setOut(out);

		Xenqtt.main(new String[] { "-vv" });
		assertTrue(new String(baos.toByteArray(), "US-ASCII").startsWith("usage: "));
	}

	@Test
	public void testMain_InvalidSwitch() throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(baos);
		System.setOut(out);

		Xenqtt.main(new String[] { "proxy", "-verbose" });
		assertTrue(new String(baos.toByteArray(), "US-ASCII").startsWith("usage: "));
	}

	@Test
	public void testMain_NoArgs() throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(baos);
		System.setOut(out);

		Xenqtt.main(new String[0]);
		assertTrue(new String(baos.toByteArray(), "US-ASCII").startsWith("usage: "));
	}

}
