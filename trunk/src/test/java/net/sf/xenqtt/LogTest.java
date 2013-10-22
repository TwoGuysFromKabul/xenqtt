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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class LogTest {

	PrintStream standardOut = System.out;
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	PrintStream out = new PrintStream(baos, true);

	@Before
	public void setup() {
		System.setOut(out);
	}

	@After
	public void teardown() {
		System.setOut(standardOut);
	}

	@Test
	public void testLogging() {
		Log.trace("Should not appear: %s", "trace");
		Log.debug("Should not appear: %s", "debug");
		Log.info("Should appear: %s", "info");
		Log.warn("Should appear: %s", "warn");
		Log.warn(new RuntimeException("warn"), "Should appear: %s", "warn");
		Log.error("Should appear: %s", "error");
		Log.error(new RuntimeException("error"), "Should appear: %s", "error");
		Log.fatal("Should appear: %s", "fatal");
		Log.fatal(new RuntimeException("fatal"), "Should appear: %s", "fatal");
	}

}
