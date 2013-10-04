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

import java.util.concurrent.CountDownLatch;

import net.sf.xenqtt.ArgumentExtractor.Arguments;
import net.sf.xenqtt.ArgumentExtractor.Mode;

import org.junit.Test;

public class ArgumentExtractorTest {

	CountDownLatch latch = new CountDownLatch(0);

	@Test
	public void testExtractArguments_ModeOnly() {
		Arguments arguments = ArgumentExtractor.extractArguments(latch, "proxy");

		assertTrue(arguments.globalOptions.isEmpty());
		assertSame(Mode.PROXY, arguments.mode);
		assertNotNull(arguments.applicationArguments);
	}

	@Test
	public void testExtractArguments_InvalidMode() {
		assertNull(ArgumentExtractor.extractArguments(latch, "proxyer"));
	}

	@Test
	public void testExtractArguments_GlobalOptionsNoMode() {
		assertNull(ArgumentExtractor.extractArguments(latch, "-v", "-v"));
	}

	@Test
	public void testExtractArguments_ModeAndModeArguments() {
		Arguments arguments = ArgumentExtractor.extractArguments(latch, "proxy", "-p", "1883", "-a");

		assertTrue(arguments.globalOptions.isEmpty());
		assertSame(Mode.PROXY, arguments.mode);
		assertTrue(arguments.applicationArguments.isFlagSpecified("-a"));
		assertEquals(1883, arguments.applicationArguments.getArgAsInt("-p"));
		assertTrue(arguments.applicationArguments.wereAllFlagsInterrogated());
	}

	@Test
	public void testExtractArguments_ModeAndModeArguments_MultipleFlagsRunTogether() {
		Arguments arguments = ArgumentExtractor.extractArguments(latch, "proxy", "-p", "1883", "-abcd");

		assertTrue(arguments.globalOptions.isEmpty());
		assertSame(Mode.PROXY, arguments.mode);
		assertTrue(arguments.applicationArguments.isFlagSpecified("-a"));
		assertTrue(arguments.applicationArguments.isFlagSpecified("-b"));
		assertTrue(arguments.applicationArguments.isFlagSpecified("-c"));
		assertTrue(arguments.applicationArguments.isFlagSpecified("-d"));
		assertEquals(1883, arguments.applicationArguments.getArgAsInt("-p"));
		assertTrue(arguments.applicationArguments.wereAllFlagsInterrogated());
	}

	@Test
	public void testExtractArguments_ModeAndModeArguments_MultipleFlagsRunTogether_FinalParameterIsArg() {
		Arguments arguments = ArgumentExtractor.extractArguments(latch, "proxy", "-p", "1883", "-abcd", "true");

		assertTrue(arguments.globalOptions.isEmpty());
		assertSame(Mode.PROXY, arguments.mode);
		assertTrue(arguments.applicationArguments.isFlagSpecified("-a"));
		assertTrue(arguments.applicationArguments.isFlagSpecified("-b"));
		assertTrue(arguments.applicationArguments.isFlagSpecified("-c"));
		assertFalse(arguments.applicationArguments.isFlagSpecified("-d"));
		assertEquals(1883, arguments.applicationArguments.getArgAsInt("-p"));
		assertTrue(arguments.applicationArguments.getArgAsBoolean("-d"));
		assertTrue(arguments.applicationArguments.wereAllFlagsInterrogated());
	}

	@Test
	public void testExtractArguments_HelpMode_NoSpecifiedMode() {
		Arguments arguments = ArgumentExtractor.extractArguments(latch, "help");

		assertTrue(arguments.globalOptions.isEmpty());
		assertSame(Mode.HELP, arguments.mode);
		assertTrue(arguments.applicationArguments.isEmpty());
	}

	@Test
	public void testExtractArguments_HelpMode_SpecifiedMode() {
		Arguments arguments = ArgumentExtractor.extractArguments(latch, "help", "mockbroker");

		assertTrue(arguments.globalOptions.isEmpty());
		assertSame(Mode.HELP, arguments.mode);
		assertEquals("mockbroker", arguments.applicationArguments.getArgAsString("-m"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testExtractArguments_ModeAndModeArguments_MultipleValuesToOneParameter() {
		ArgumentExtractor.extractArguments(latch, "proxy", "-p", "1883", "8883");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testExtractArguments_ModeAndModeArguments_DashOnlyForFlagOrArg() {
		ArgumentExtractor.extractArguments(latch, "proxy", "-", "1883");
	}

	@Test
	public void testExtractArguments_GlobalOptionsAndMode() {
		Arguments arguments = ArgumentExtractor.extractArguments(latch, "-v", "-v", "proxy");

		assertEquals(2, arguments.globalOptions.size());
		for (String globalOption : arguments.globalOptions) {
			assertEquals("-v", globalOption);
		}
		assertSame(Mode.PROXY, arguments.mode);
		assertNotNull(arguments.applicationArguments);
	}

	@Test
	public void testExtractArguments_AllOptions() {
		Arguments arguments = ArgumentExtractor.extractArguments(latch, "-v", "-v", "proxy", "-p", "1883", "-a");

		assertEquals(2, arguments.globalOptions.size());
		for (String globalOption : arguments.globalOptions) {
			assertEquals("-v", globalOption);
		}
		assertSame(Mode.PROXY, arguments.mode);
		assertTrue(arguments.applicationArguments.isFlagSpecified("-a"));
		assertEquals(1883, arguments.applicationArguments.getArgAsInt("-p"));
		assertTrue(arguments.applicationArguments.wereAllFlagsInterrogated());
	}

	@Test
	public void testDetermineLoggingLevels_Default() {
		Arguments arguments = ArgumentExtractor.extractArguments(latch, "proxy", "-p", "1883", "-a");
		LoggingLevels levels = arguments.determineLoggingLevels();

		assertEquals(0x38, levels.flags());
	}

	@Test
	public void testDetermineLoggingLevels_Info() {
		Arguments arguments = ArgumentExtractor.extractArguments(latch, "-v", "proxy", "-p", "1883", "-a");
		LoggingLevels levels = arguments.determineLoggingLevels();

		assertEquals(0x3c, levels.flags());
	}

	@Test
	public void testDetermineLoggingLevels_DebugTwoFlags() {
		Arguments arguments = ArgumentExtractor.extractArguments(latch, "-v", "-v", "proxy", "-p", "1883", "-a");
		LoggingLevels levels = arguments.determineLoggingLevels();

		assertEquals(0x3e, levels.flags());
	}

	@Test
	public void testDetermineLoggingLevels_DebugOneFlag() {
		Arguments arguments = ArgumentExtractor.extractArguments(latch, "-vv", "proxy", "-p", "1883", "-a");
		LoggingLevels levels = arguments.determineLoggingLevels();

		assertEquals(0x3e, levels.flags());
	}

	@Test
	public void testDetermineLoggingLevels_DebugEvenWithMoreThanTwoSpecifiers() {
		Arguments arguments = ArgumentExtractor.extractArguments(latch, "-vv", "-v", "proxy", "-p", "1883", "-a");
		LoggingLevels levels = arguments.determineLoggingLevels();

		assertEquals(0x3e, levels.flags());
	}

	@Test
	public void testDetermineLoggingLevels_DefaultIfOneSpecifierIsTooAggressive() {
		Arguments arguments = ArgumentExtractor.extractArguments(latch, "-vvv", "proxy", "-p", "1883", "-a");
		LoggingLevels levels = arguments.determineLoggingLevels();

		assertEquals(0x38, levels.flags());
	}

}
