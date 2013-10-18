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

public class AppContextTest {

	AppContext arguments;

	@Before
	public void setup() {
		List<String> flags = new ArrayList<String>();
		flags.add("-a");
		flags.add("-b");
		flags.add("-c");

		Map<String, String> argumentsMap = new HashMap<String, String>();
		argumentsMap.put("-i", String.valueOf(Integer.MAX_VALUE));
		argumentsMap.put("-l", String.valueOf(Long.MAX_VALUE));
		argumentsMap.put("-d", String.valueOf(Double.MAX_VALUE));
		argumentsMap.put("-b", "true");
		argumentsMap.put("-s", "I am the very model of a modern Major General.");

		arguments = new AppContext(flags, argumentsMap, null);
	}

	@Test
	public void testGetArgAsInt_Required() {
		assertEquals(Integer.MAX_VALUE, arguments.getArgAsInt("-i"));
	}

	@Test(expected = IllegalStateException.class)
	public void testGetArgAsInt_Required_Missing() {
		arguments.getArgAsInt("-x");
	}

	@Test(expected = RuntimeException.class)
	public void testGetArgAsInt_Required_Malformed() {
		arguments.getArgAsInt("-s");
	}

	@Test
	public void testGetArgAsInt_Optional_Found() {
		assertEquals(Integer.MAX_VALUE, arguments.getArgAsInt("-i", 1));
	}

	@Test
	public void testGetArgAsInt_Optional_NotFound() {
		assertEquals(1, arguments.getArgAsInt("-x", 1));
	}

	@Test
	public void testGetArgAsLong_Required() {
		assertEquals(Long.MAX_VALUE, arguments.getArgAsLong("-l"));
	}

	@Test(expected = IllegalStateException.class)
	public void testGetArgAsLong_Required_Missing() {
		arguments.getArgAsLong("-x");
	}

	@Test(expected = RuntimeException.class)
	public void testGetArgAsLong_Required_Malformed() {
		arguments.getArgAsLong("-s");
	}

	@Test
	public void testGetArgAsLong_Optional_Found() {
		assertEquals(Long.MAX_VALUE, arguments.getArgAsLong("-l", 1));
	}

	@Test
	public void testGetArgAsLong_Optional_NotFound() {
		assertEquals(1, arguments.getArgAsLong("-x", 1));
	}

	@Test
	public void testGetArgAsDouble_Required() {
		assertEquals(Double.MAX_VALUE, arguments.getArgAsDouble("-d"), 0.0D);
	}

	@Test(expected = IllegalStateException.class)
	public void testGetArgAsDouble_Required_Missing() {
		arguments.getArgAsDouble("-x");
	}

	@Test(expected = RuntimeException.class)
	public void testGetArgAsDouble_Required_Malformed() {
		arguments.getArgAsDouble("-s");
	}

	@Test
	public void testGetArgAsDouble_Optional_Found() {
		assertEquals(Double.MAX_VALUE, arguments.getArgAsDouble("-d", 1.0D), 0.0D);
	}

	@Test
	public void testGetArgAsDouble_Optional_NotFound() {
		assertEquals(1.0D, arguments.getArgAsDouble("-x", 1.0D), 0.0D);
	}

	@Test
	public void testGetArgAsBoolean_Required() {
		assertTrue(arguments.getArgAsBoolean("-b"));
	}

	@Test(expected = IllegalStateException.class)
	public void testGetArgAsBoolean_Required_Missing() {
		arguments.getArgAsBoolean("-x");
	}

	@Test
	public void testGetArgAsBoolean_Required_Malformed() {
		assertFalse(arguments.getArgAsBoolean("-s"));
	}

	@Test
	public void testGetArgAsBoolean_Optional_Found() {
		assertTrue(arguments.getArgAsBoolean("-b", false));
	}

	@Test
	public void testGetArgAsBoolean_Optional_NotFound() {
		assertFalse(arguments.getArgAsBoolean("-x", false));
	}

	@Test
	public void testGetArgAsString_Required() {
		assertEquals("I am the very model of a modern Major General.", arguments.getArgAsString("-s"));
	}

	@Test(expected = IllegalStateException.class)
	public void testGetArgAsString_Required_Missing() {
		arguments.getArgAsInt("-x");
	}

	@Test
	public void testGetArgAsString_Optional_Found() {
		assertEquals("I am the very model of a modern Major General.", arguments.getArgAsString("-s", "I am information, vegetable, animal, and mineral."));
	}

	@Test
	public void testGetArgAsString_Optional_NotFound() {
		assertEquals("I am information, vegetable, animal, and mineral.", arguments.getArgAsString("-x", "I am information, vegetable, animal, and mineral."));
	}

	@Test
	public void testIsFlagSpecified() {
		assertTrue(arguments.isFlagSpecified("-a"));
		assertTrue(arguments.isFlagSpecified("-b"));
		assertTrue(arguments.isFlagSpecified("-c"));
		assertFalse(arguments.isFlagSpecified("-i"));
		assertFalse(arguments.isFlagSpecified("-l"));
		assertFalse(arguments.isFlagSpecified("-d"));
		assertFalse(arguments.isFlagSpecified("-s"));
	}

	@Test
	public void testWereAllFlagsInterrogated_True() {
		assertTrue(arguments.isFlagSpecified("-a"));
		assertTrue(arguments.isFlagSpecified("-b"));
		assertTrue(arguments.isFlagSpecified("-c"));
		assertTrue(arguments.wereAllFlagsInterrogated());
	}

	@Test
	public void testWereAllFlagsInterrogated_False() {
		assertTrue(arguments.isFlagSpecified("-a"));
		assertTrue(arguments.isFlagSpecified("-b"));
		assertFalse(arguments.wereAllFlagsInterrogated());
	}

	@Test
	public void testIsEmpty() {
		assertFalse(arguments.isEmpty());
		assertTrue(new AppContext(null).isEmpty());
	}

}
