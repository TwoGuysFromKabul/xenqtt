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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Test;

public class JavaLoggingDelegateTest {

	JavaLoggingDelegate delegate = new JavaLoggingDelegate();

	@After
	public void teardown() {
		File xenqttLogFile = getXenqttLogFile();
		if (xenqttLogFile != null) {
			delete(xenqttLogFile);
		}
	}

	private File getXenqttLogFile() {
		String userHome = System.getProperty("user.home");
		if (userHome == null) {
			return null;
		}

		String xenqttFileName = String.format("%s%sxenqtt.log.0", userHome, userHome.endsWith("/") ? "" : "/");
		File xenqttFile = new File(xenqttFileName);
		if (!xenqttFile.exists()) {
			return null;
		}

		return xenqttFile;
	}

	private void delete(File xenqttLogFile) {
		try {
			xenqttLogFile.delete();
		} catch (Exception ex) {
			fail("Unable to delete the xenqtt.log.0 file in the user home directory.");
		}
	}

	@Test
	public void testLogging() throws Exception {
		delegate.trace("trace message with parameter: %s", "hello");
		delegate.debug("debug message with parameter: %s", "hello");
		delegate.info("info message with parameter: %s", "hello");
		delegate.warn("warn message with parameter: %s", "hello");
		delegate.error("error message with parameter: %s", "hello");
		delegate.fatal("fatal message with parameter: %s", "hello");

		List<String> lines = getLinesFromXenqttLogFile();
		List<String> nextStandardLogLevel = new ArrayList<String>(Arrays.asList(new String[] { "trace", "debug", "info", "warn", "error", "fatal" }));
		for (int i = 0; i < lines.size(); i++) {
			assertTrue(lines.get(i).contains(String.format("%s message with parameter: hello", nextStandardLogLevel.remove(0))));
		}
	}

	private List<String> getLinesFromXenqttLogFile() throws Exception {
		File xenqttLogFile = getXenqttLogFile();
		if (xenqttLogFile == null) {
			fail("Unable to get the xenqtt.log.0 file from the user home directory.");
		}

		BufferedReader reader = new BufferedReader(new FileReader(xenqttLogFile));
		List<String> lines = new ArrayList<String>();
		String line = null;
		while ((line = reader.readLine()) != null) {
			lines.add(line);
		}
		reader.close();

		return lines;
	}

}
