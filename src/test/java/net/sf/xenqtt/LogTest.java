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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import net.sf.xenqtt.Log.LoggingDelegate;

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

	boolean asyncTestable;
	PrintStream standardOut;
	NotifyingByteArrayOutputStream baos = new NotifyingByteArrayOutputStream();
	PrintStream out = new PrintStream(baos);

	@Before
	public void setup() throws Exception {
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
		System.setOut(standardOut);
	}

	@Test
	public void testTrace() throws Exception {
		if (asyncTestable) {
			doTestAtLevel(TRACE_AND_ABOVE, new String[] { "Message: Trace", "Message: Debug", "Message: Info", "Message: Warn", "Message: Error",
					"Message: Fatal" });
		} else {
			ensureLog4jLoggerUsed();
		}
	}

	@Test
	public void testDebug() throws Exception {
		if (asyncTestable) {
			doTestAtLevel(DEBUG_AND_ABOVE, new String[] { "Message: Debug", "Message: Info", "Message: Warn", "Message: Error", "Message: Fatal" });
		} else {
			ensureLog4jLoggerUsed();
		}
	}

	@Test
	public void testInfo() throws Exception {
		if (asyncTestable) {
			doTestAtLevel(INFO_AND_ABOVE, new String[] { "Message: Info", "Message: Warn", "Message: Error", "Message: Fatal" });
		} else {
			ensureLog4jLoggerUsed();
		}
	}

	@Test
	public void testWarn() throws Exception {
		if (asyncTestable) {
			doTestAtLevel(WARN_AND_ABOVE, new String[] { "Message: Warn", "Message: Error", "Message: Fatal" });
		} else {
			ensureLog4jLoggerUsed();
		}
	}

	@Test
	public void testWarn_WithException() throws Exception {
		if (asyncTestable) {
			doExceptionTestAtLevel(WARN_AND_ABOVE, new String[] { "Message: Warn", "java.lang.RuntimeException: Doeth!" });
		} else {
			ensureLog4jLoggerUsed();
		}
	}

	@Test
	public void testError() throws Exception {
		if (asyncTestable) {
			doTestAtLevel(ERROR_AND_ABOVE, new String[] { "Message: Error", "Message: Fatal" });
		} else {
			ensureLog4jLoggerUsed();
		}
	}

	@Test
	public void testError_WithException() throws Exception {
		if (asyncTestable) {
			doExceptionTestAtLevel(ERROR_AND_ABOVE, new String[] { "Message: Error", "java.lang.RuntimeException: Doeth!" });
		} else {
			ensureLog4jLoggerUsed();
		}
	}

	@Test
	public void testFatal() throws Exception {
		if (asyncTestable) {
			doTestAtLevel(FATAL, new String[] { "Message: Fatal" });
		} else {
			ensureLog4jLoggerUsed();
		}
	}

	@Test
	public void testFatal_WithException() throws Exception {
		if (asyncTestable) {
			doExceptionTestAtLevel(FATAL, new String[] { "Message: Fatal", "java.lang.RuntimeException: Doeth!" });
		} else {
			ensureLog4jLoggerUsed();
		}
	}

	private LoggingLevels toLoggingLevels(int flags) {
		return new LoggingLevels(flags);
	}

	private void doTestAtLevel(int levelFlag, String[] expectedValues) throws Exception {
		Log.setLoggingLevels(toLoggingLevels(levelFlag));
		CountDownLatch latch = new CountDownLatch(expectedValues.length);
		baos.setLatch(latch);

		Log.trace("Message: %s", "Trace");
		Log.debug("Message: %s", "Debug");
		Log.info("Message: %s", "Info");
		Log.warn("Message: %s", "Warn");
		Log.error("Message: %s", "Error");
		Log.fatal("Message: %s", "Fatal");
		assertTrue(latch.await(1, TimeUnit.SECONDS));

		List<String> messages = Arrays.asList(new String(baos.toByteArray(), Charset.forName("US-ASCII")).split("[\r\n]+"));
		List<String> expected = new ArrayList<String>(Arrays.asList(expectedValues));
		for (String message : messages) {
			Iterator<String> iter = expected.iterator();
			while (iter.hasNext()) {
				if (message.contains(iter.next())) {
					iter.remove();
					break;
				}
			}
		}
		assertTrue(expected.isEmpty());
	}

	private void doExceptionTestAtLevel(int levelFlag, String[] expectedValues) throws Exception {
		Log.setLoggingLevels(toLoggingLevels(levelFlag));
		CountDownLatch latch = new CountDownLatch(expectedValues.length);
		baos.setLatch(latch);

		switch (levelFlag) {
		case WARN_AND_ABOVE:
			Log.warn(new RuntimeException("Doeth!"), "Message: %s", "Warn");
			break;
		case ERROR_AND_ABOVE:
			Log.error(new RuntimeException("Doeth!"), "Message: %s", "Error");
			break;
		case FATAL:
			Log.fatal(new RuntimeException("Doeth!"), "Message: %s", "Fatal");
			break;
		}
		assertTrue(latch.await(1, TimeUnit.SECONDS));

		List<String> messages = new ArrayList<String>(Arrays.asList(new String(baos.toByteArray(), Charset.forName("US-ASCII")).split("[\r\n]+")));
		List<String> expected = new ArrayList<String>(Arrays.asList(expectedValues));
		for (String message : messages) {
			Iterator<String> iter = expected.iterator();
			while (iter.hasNext()) {
				if (message.equals(iter.next())) {
					iter.remove();
					break;
				}
			}

			if (expected.isEmpty()) {
				break;
			}
		}
		assertTrue(expected.isEmpty());
	}

	private void ensureLog4jLoggerUsed() throws Exception {
		Field delegateField = Log.class.getDeclaredField("DELEGATE");
		delegateField.setAccessible(true);
		LoggingDelegate delegate = (LoggingDelegate) delegateField.get(null);

		assertSame(Log4jLoggingDelegate.class, delegate.getClass());
	}

	private static final class NotifyingByteArrayOutputStream extends ByteArrayOutputStream {

		private volatile CountDownLatch latch;

		private void setLatch(CountDownLatch latch) {
			this.latch = latch;
		}

		/**
		 * @see java.io.ByteArrayOutputStream#write(byte[], int, int)
		 */
		@Override
		public synchronized void write(byte[] buffer, int off, int len) {
			super.write(buffer, off, len);
			if (latch != null) {
				for (byte b : buffer) {
					if (b == '\n') {
						latch.countDown();
					}
				}
			}
		}

	}

}
