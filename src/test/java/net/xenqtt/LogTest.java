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
package net.xenqtt;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.xenqtt.Log;

import org.apache.log4j.Appender;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public class LogTest {

	static List<String> entries = new ArrayList<String>();

	@BeforeClass
	public static void setupBeforeClass() throws Exception {
		Logger xenqtt = Logger.getLogger("xenqtt");
		assertNotNull(xenqtt);

		Appender console = Logger.getRootLogger().getAppender("console");
		assertNotNull(console);

		console.addFilter(new Filter() {
			@Override
			public int decide(LoggingEvent event) {
				entries.add(event.getRenderedMessage());

				ThrowableInformation info = event.getThrowableInformation();
				if (info != null) {
					Throwable t = info.getThrowable();
					if (t != null) {
						entries.add(t.getMessage());
					}
				}

				return Filter.ACCEPT;
			}
		});
	}

	@After
	public void teardown() {
		entries.clear();
	}

	@Test
	public void testLogging() throws InterruptedException {
		Log.trace("Should not appear: %s", "trace");
		Log.debug("Should not appear: %s", "debug");
		Log.info("Should appear: %s", "info");
		Log.warn("Should appear: %s", "warn");
		Log.warn(new RuntimeException("Exception: warn"), "Should appear: %s", "warn");
		Log.error("Should appear: %s", "error");
		Log.error(new RuntimeException("Exception: error"), "Should appear: %s", "error");
		Log.fatal("Should appear: %s", "fatal");
		Log.fatal(new RuntimeException("Exception: fatal"), "Should appear: %s", "fatal");

		Thread.sleep(1000);

		Map<String, Integer> counts = new HashMap<String, Integer>();
		counts.put("Should appear: info", 1);
		counts.put("Should appear: warn", 2);
		counts.put("Exception: warn", 1);
		counts.put("Should appear: error", 2);
		counts.put("Exception: error", 1);
		counts.put("Should appear: fatal", 2);
		counts.put("Exception: fatal", 1);
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
