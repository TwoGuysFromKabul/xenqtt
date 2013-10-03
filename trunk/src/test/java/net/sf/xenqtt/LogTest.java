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

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
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

	boolean asyncTestable = true;
	PrintStream standardOut;
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	PrintStream out = new PrintStream(baos);

	@Before
	public void setup() throws Exception {
		Xenqtt.loggingLevels = new LoggingLevels(false, false, false, true, true, true);
		asyncTestable = isAsyncTestable();
		if (asyncTestable) {
			standardOut = System.out;
			System.setOut(out);
		}
	}

	private boolean isAsyncTestable() throws Exception {
		Field field = Log.class.getDeclaredField("DELEGATE");
		field.setAccessible(true);
		LoggingDelegate delegate = (LoggingDelegate) field.get(null);
		Class<?> delegateClass = delegate.getClass();

		return Log4jLoggingDelegate.class != delegateClass && JavaLoggingDelegate.class != delegateClass;
	}

	@After
	public void teardown() {
		if (asyncTestable) {
			System.setOut(standardOut);
		}
	}

	@Test
	public void testLogging() throws Exception {
		if (asyncTestable) {
			doTestLogging();
		} else {
			ensureLog4jLoggerUsed();
		}
	}

	private void doTestLogging() throws Exception {
		Log.trace("Message: %s", "TRACE");
		Log.debug("Message: %s", "DEBUG");
		Log.info("Message: %s", "INFO");

		Log.warn("Message: %s", "WARN");
		Throwable tWarn = new Throwable("Warning");
		Log.warn(tWarn, "Message: %s", "WARN");

		Log.error("Message: %s", "ERROR");
		Throwable tError = new Throwable("Error");
		Log.warn(tError, "Message: %s", "ERROR");

		Log.fatal("Message: %s", "FATAL");
		Throwable tFatal = new Throwable("Fatal");
		Log.warn(tFatal, "Message: %s", "FATAL");

		Thread.sleep(1000);

		List<String> loggedLines = getLoggedLines();
		int warnExpected = 2;
		boolean warnException = false;
		int errorExpected = 2;
		boolean errorException = false;
		int fatalExpected = 2;
		boolean fatalException = false;
		for (String loggedLine : loggedLines) {
			if (loggedLine.equals("Message: WARN")) {
				warnExpected--;
			} else if (loggedLine.equals("Message: ERROR")) {
				errorExpected--;
			} else if (loggedLine.equals("Message: FATAL")) {
				fatalExpected--;
			} else if (loggedLine.equals("java.lang.Throwable: Warning")) {
				warnException = true;
			} else if (loggedLine.equals("java.lang.Throwable: Error")) {
				errorException = true;
			} else if (loggedLine.equals("java.lang.Throwable: Fatal")) {
				fatalException = true;
			} else if (!loggedLine.matches("^\\s*at .*$")) {
				fail(String.format("Unexpected log statement found: %s", loggedLine));
			}
		}

		assertEquals(0, warnExpected);
		assertTrue(warnException);
		assertEquals(0, errorExpected);
		assertTrue(errorException);
		assertEquals(0, fatalExpected);
		assertTrue(fatalException);
	}

	private List<String> getLoggedLines() {
		String logBuffer = new String(baos.toByteArray(), Charset.forName("US-ASCII"));

		return Arrays.asList(logBuffer.split("[\r\n]+"));
	}

	private void ensureLog4jLoggerUsed() throws Exception {
		Field delegateField = Log.class.getDeclaredField("DELEGATE");
		delegateField.setAccessible(true);
		LoggingDelegate delegate = (LoggingDelegate) delegateField.get(null);

		assertSame(Log4jLoggingDelegate.class, delegate.getClass());
	}

}
