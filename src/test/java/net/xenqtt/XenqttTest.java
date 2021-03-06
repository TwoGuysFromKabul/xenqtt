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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import net.xenqtt.Xenqtt;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class XenqttTest {

	PrintStream standardOut;

	@Before
	public void setup() {
		standardOut = System.out;
	}

	@After
	public void teardown() {
		System.setOut(standardOut);
	}

	@Test
	public void testMain_InvalidMode() throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(baos);
		System.setOut(out);

		Xenqtt.main(new String[] { "-vv", "proxe" });
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
	public void testMain_NoArgs() throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(baos);
		System.setOut(out);

		Xenqtt.main(new String[0]);
		assertTrue(new String(baos.toByteArray(), "US-ASCII").startsWith("usage: "));
	}

}
