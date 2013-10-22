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
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
	public void testLogging() throws InterruptedException {
		Log.trace("Should not appear: %s", "trace");
		Log.debug("Should not appear: %s", "debug");
		Log.info("Should appear: %s", "info");
		Log.warn("Should appear: %s", "warn");
		Log.warn(new RuntimeException("warn"), "Should appear: %s", "warn");
		Log.error("Should appear: %s", "error");
		Log.error(new RuntimeException("error"), "Should appear: %s", "error");
		Log.fatal("Should appear: %s", "fatal");
		Log.fatal(new RuntimeException("fatal"), "Should appear: %s", "fatal");

		Thread.sleep(1000);

		List<String> entries = Arrays.asList(new String(baos.toByteArray(), Charset.forName("US-ASCII")).split("\n"));
		Map<String, Integer> counts = new HashMap<String, Integer>();
		counts.put("Should appear: info", 1);
		counts.put("Should appear: warn", 2);
		counts.put("java.lang.RuntimeException: warn", 1);
		counts.put("Should appear: error", 2);
		counts.put("java.lang.RuntimeException: error", 1);
		counts.put("Should appear: fatal", 2);
		counts.put("java.lang.RuntimeException: fatal", 1);
		for (String entry : entries) {
			String formattedEntry = entry.replaceAll("^.*\\- ", "");
			Integer count = counts.get(formattedEntry);
			if (count == null) {
				continue;
			}

			counts.put(formattedEntry, new Integer(count.intValue() - 1));
		}

		for (Entry<String, Integer> count : counts.entrySet()) {
			assertEquals(count.getKey(), 0, count.getValue().intValue());
		}
	}

}
