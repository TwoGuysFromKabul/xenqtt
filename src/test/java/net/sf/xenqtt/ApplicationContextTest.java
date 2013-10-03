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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class ApplicationContextTest {

	AppContext context;

	@Before
	public void setup() {
		List<String> flags = new ArrayList<String>();
		flags.add("-a");
		flags.add("-b");
		flags.add("-c");

		Map<String, String> contextMap = new HashMap<String, String>();
		contextMap.put("-i", String.valueOf(Integer.MAX_VALUE));
		contextMap.put("-l", String.valueOf(Long.MAX_VALUE));
		contextMap.put("-d", String.valueOf(Double.MAX_VALUE));
		contextMap.put("-b", "true");
		contextMap.put("-s", "I am the very model of a modern Major General.");

		context = new AppContext(flags, contextMap);
	}

	@Test
	public void testGetArgAsInt_Required() {
		assertEquals(Integer.MAX_VALUE, context.getArgAsInt("-i"));
	}

	@Test(expected = IllegalStateException.class)
	public void testGetArgAsInt_Required_Missing() {
		context.getArgAsInt("-x");
	}

	@Test(expected = RuntimeException.class)
	public void testGetArgAsInt_Required_Malformed() {
		context.getArgAsInt("-s");
	}

	@Test
	public void testGetArgAsInt_Optional_Found() {
		assertEquals(Integer.MAX_VALUE, context.getArgAsInt("-i", 1));
	}

	@Test
	public void testGetArgAsInt_Optional_NotFound() {
		assertEquals(1, context.getArgAsInt("-x", 1));
	}

	@Test
	public void testGetArgAsLong_Required() {
		assertEquals(Long.MAX_VALUE, context.getArgAsLong("-l"));
	}

	@Test(expected = IllegalStateException.class)
	public void testGetArgAsLong_Required_Missing() {
		context.getArgAsLong("-x");
	}

	@Test(expected = RuntimeException.class)
	public void testGetArgAsLong_Required_Malformed() {
		context.getArgAsLong("-s");
	}

	@Test
	public void testGetArgAsLong_Optional_Found() {
		assertEquals(Long.MAX_VALUE, context.getArgAsLong("-l", 1));
	}

	@Test
	public void testGetArgAsLong_Optional_NotFound() {
		assertEquals(1, context.getArgAsLong("-x", 1));
	}

	@Test
	public void testGetArgAsDouble_Required() {
		assertEquals(Double.MAX_VALUE, context.getArgAsDouble("-d"), 0.0D);
	}

	@Test(expected = IllegalStateException.class)
	public void testGetArgAsDouble_Required_Missing() {
		context.getArgAsDouble("-x");
	}

	@Test(expected = RuntimeException.class)
	public void testGetArgAsDouble_Required_Malformed() {
		context.getArgAsDouble("-s");
	}

	@Test
	public void testGetArgAsDouble_Optional_Found() {
		assertEquals(Double.MAX_VALUE, context.getArgAsDouble("-d", 1.0D), 0.0D);
	}

	@Test
	public void testGetArgAsDouble_Optional_NotFound() {
		assertEquals(1.0D, context.getArgAsDouble("-x", 1.0D), 0.0D);
	}

	@Test
	public void testGetArgAsBoolean_Required() {
		assertTrue(context.getArgAsBoolean("-b"));
	}

	@Test(expected = IllegalStateException.class)
	public void testGetArgAsBoolean_Required_Missing() {
		context.getArgAsBoolean("-x");
	}

	@Test
	public void testGetArgAsBoolean_Required_Malformed() {
		assertFalse(context.getArgAsBoolean("-s"));
	}

	@Test
	public void testGetArgAsBoolean_Optional_Found() {
		assertTrue(context.getArgAsBoolean("-b", false));
	}

	@Test
	public void testGetArgAsBoolean_Optional_NotFound() {
		assertFalse(context.getArgAsBoolean("-x", false));
	}

	@Test
	public void testGetArgAsString_Required() {
		assertEquals("I am the very model of a modern Major General.", context.getArgAsString("-s"));
	}

	@Test(expected = IllegalStateException.class)
	public void testGetArgAsString_Required_Missing() {
		context.getArgAsInt("-x");
	}

	@Test
	public void testGetArgAsString_Optional_Found() {
		assertEquals("I am the very model of a modern Major General.", context.getArgAsString("-s", "I am information, vegetable, animal, and mineral."));
	}

	@Test
	public void testGetArgAsString_Optional_NotFound() {
		assertEquals("I am information, vegetable, animal, and mineral.", context.getArgAsString("-x", "I am information, vegetable, animal, and mineral."));
	}

	@Test
	public void testIsFlagSpecified() {
		assertTrue(context.isFlagSpecified("-a"));
		assertTrue(context.isFlagSpecified("-b"));
		assertTrue(context.isFlagSpecified("-c"));
		assertFalse(context.isFlagSpecified("-i"));
		assertFalse(context.isFlagSpecified("-l"));
		assertFalse(context.isFlagSpecified("-d"));
		assertFalse(context.isFlagSpecified("-s"));
	}

	@Test
	public void testWereAllFlagsInterrogated_True() {
		assertTrue(context.isFlagSpecified("-a"));
		assertTrue(context.isFlagSpecified("-b"));
		assertTrue(context.isFlagSpecified("-c"));
		assertTrue(context.wereAllFlagsInterrogated());
	}

	@Test
	public void testWereAllFlagsInterrogated_False() {
		assertTrue(context.isFlagSpecified("-a"));
		assertTrue(context.isFlagSpecified("-b"));
		assertFalse(context.wereAllFlagsInterrogated());
	}

	@Test
	public void testIsEmpty() {
		assertFalse(context.isEmpty());
		assertTrue(new AppContext().isEmpty());
	}

}
