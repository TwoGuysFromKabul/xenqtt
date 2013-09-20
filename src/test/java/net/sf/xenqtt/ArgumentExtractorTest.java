package net.sf.xenqtt;

import static org.junit.Assert.*;
import net.sf.xenqtt.ArgumentExtractor.Arguments;
import net.sf.xenqtt.ArgumentExtractor.Mode;

import org.junit.Test;

public class ArgumentExtractorTest {

	@Test
	public void testExtractArguments_ModeOnly() {
		Arguments arguments = ArgumentExtractor.extractArguments("proxy");

		assertTrue(arguments.globalOptions.isEmpty());
		assertSame(Mode.PROXY, arguments.mode);
		assertEquals(0, arguments.modeArguments.length);
	}

	@Test
	public void testExtractArguments_InvalidMode() {
		assertNull(ArgumentExtractor.extractArguments("proxyer"));
	}

	@Test
	public void testExtractArguments_GlobalOptionsNoMode() {
		assertNull(ArgumentExtractor.extractArguments("-v", "-v"));
	}

	@Test
	public void testExtractArguments_ModeAndModeArguments() {
		Arguments arguments = ArgumentExtractor.extractArguments("proxy", "1", "2", "3");

		assertTrue(arguments.globalOptions.isEmpty());
		assertSame(Mode.PROXY, arguments.mode);
		assertEquals(3, arguments.modeArguments.length);
		int expectedArgument = 1;
		for (String modeArgument : arguments.modeArguments) {
			assertEquals(expectedArgument++, Integer.parseInt(modeArgument));
		}
	}

	@Test
	public void testExtractArguments_ModeAndModeArguments_SomeArgumentsStartWithADash() {
		Arguments arguments = ArgumentExtractor.extractArguments("proxy", "1", "-2", "3");

		assertTrue(arguments.globalOptions.isEmpty());
		assertSame(Mode.PROXY, arguments.mode);
		assertEquals(3, arguments.modeArguments.length);
		int expectedArgument = 1;
		for (String modeArgument : arguments.modeArguments) {
			if (expectedArgument != 2) {
				assertEquals(expectedArgument++, Integer.parseInt(modeArgument));
			} else {
				assertEquals(expectedArgument++, Math.abs(Integer.parseInt(modeArgument)));
			}
		}
	}

	@Test
	public void testExtractArguments_GlobalOptionsAndMode() {
		Arguments arguments = ArgumentExtractor.extractArguments("-v", "-v", "proxy");

		assertEquals(2, arguments.globalOptions.size());
		for (String globalOption : arguments.globalOptions) {
			assertEquals("-v", globalOption);
		}
		assertSame(Mode.PROXY, arguments.mode);
		assertEquals(0, arguments.modeArguments.length);
	}

	@Test
	public void testExtractArguments_AllOptions() {
		Arguments arguments = ArgumentExtractor.extractArguments("-v", "-v", "proxy", "1", "2", "3");

		assertEquals(2, arguments.globalOptions.size());
		for (String globalOption : arguments.globalOptions) {
			assertEquals("-v", globalOption);
		}
		assertSame(Mode.PROXY, arguments.mode);
		assertEquals(3, arguments.modeArguments.length);
		int expectedArgument = 1;
		for (String modeArgument : arguments.modeArguments) {
			assertEquals(expectedArgument++, Integer.parseInt(modeArgument));
		}
	}

	@Test
	public void testExtractArguments_AllOptions_SomeModeArgumentsStartWithADash() {
		Arguments arguments = ArgumentExtractor.extractArguments("-v", "-v", "proxy", "1", "-2", "3");

		assertEquals(2, arguments.globalOptions.size());
		for (String globalOption : arguments.globalOptions) {
			assertEquals("-v", globalOption);
		}
		assertSame(Mode.PROXY, arguments.mode);
		assertEquals(3, arguments.modeArguments.length);
		int expectedArgument = 1;
		for (String modeArgument : arguments.modeArguments) {
			if (expectedArgument != 2) {
				assertEquals(expectedArgument++, Integer.parseInt(modeArgument));
			} else {
				assertEquals(expectedArgument++, Math.abs(Integer.parseInt(modeArgument)));
			}
		}
	}

	@Test
	public void testDetermineLoggingLevels_Default() {
		Arguments arguments = ArgumentExtractor.extractArguments("proxy", "1", "-2", "3");
		LoggingLevels levels = arguments.determineLoggingLevels();

		assertEquals(0x38, levels.flags());
	}

	@Test
	public void testDetermineLoggingLevels_Info() {
		Arguments arguments = ArgumentExtractor.extractArguments("-v", "proxy", "1", "-2", "3");
		LoggingLevels levels = arguments.determineLoggingLevels();

		assertEquals(0x3c, levels.flags());
	}

	@Test
	public void testDetermineLoggingLevels_DebugTwoFlags() {
		Arguments arguments = ArgumentExtractor.extractArguments("-v", "-v", "proxy", "1", "-2", "3");
		LoggingLevels levels = arguments.determineLoggingLevels();

		assertEquals(0x3e, levels.flags());
	}

	@Test
	public void testDetermineLoggingLevels_DebugOneFlag() {
		Arguments arguments = ArgumentExtractor.extractArguments("-vv", "proxy", "1", "-2", "3");
		LoggingLevels levels = arguments.determineLoggingLevels();

		assertEquals(0x3e, levels.flags());
	}

	@Test
	public void testDetermineLoggingLevels_DebugEvenWithMoreThanTwoSpecifiers() {
		Arguments arguments = ArgumentExtractor.extractArguments("-vv", "-v", "proxy", "1", "-2", "3");
		LoggingLevels levels = arguments.determineLoggingLevels();

		assertEquals(0x3e, levels.flags());
	}

	@Test
	public void testDetermineLoggingLevels_DefaultIfOneSpecifierIsTooAggressive() {
		Arguments arguments = ArgumentExtractor.extractArguments("-vvv", "proxy", "1", "-2", "3");
		LoggingLevels levels = arguments.determineLoggingLevels();

		assertEquals(0x38, levels.flags());
	}

}
