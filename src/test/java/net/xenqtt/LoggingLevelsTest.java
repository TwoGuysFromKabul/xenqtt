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
import net.xenqtt.LoggingLevels;

import org.junit.Test;

public class LoggingLevelsTest {

	@Test
	public void testFlags() {
		LoggingLevels levels = new LoggingLevels(false, false, false, false, false, false);
		assertEquals(0, levels.flags());

		levels = new LoggingLevels(false, false, false, false, false, true);
		assertEquals(0x20, levels.flags());

		levels = new LoggingLevels(false, false, false, false, true, true);
		assertEquals(0x30, levels.flags());

		levels = new LoggingLevels(false, false, false, true, true, true);
		assertEquals(0x38, levels.flags());

		levels = new LoggingLevels(false, false, true, true, true, true);
		assertEquals(0x3c, levels.flags());

		levels = new LoggingLevels(false, true, true, true, true, true);
		assertEquals(0x3e, levels.flags());

		levels = new LoggingLevels(true, true, true, true, true, true);
		assertEquals(0x3f, levels.flags());

		levels = new LoggingLevels(0);
		assertEquals(0, levels.flags());

		levels = new LoggingLevels(0x20);
		assertEquals(0x20, levels.flags());

		levels = new LoggingLevels(0x30);
		assertEquals(0x30, levels.flags());

		levels = new LoggingLevels(0x38);
		assertEquals(0x38, levels.flags());

		levels = new LoggingLevels(0x3c);
		assertEquals(0x3c, levels.flags());

		levels = new LoggingLevels(0x3e);
		assertEquals(0x3e, levels.flags());

		levels = new LoggingLevels(0x3f);
		assertEquals(0x3f, levels.flags());
	}

	@Test
	public void testIsLoggable() {
		LoggingLevels levels = new LoggingLevels(false, false, false, true, true, true);
		assertFalse(levels.isLoggable(0x01));
		assertFalse(levels.isLoggable(0x02));
		assertFalse(levels.isLoggable(0x04));
		assertTrue(levels.isLoggable(0x08));
		assertTrue(levels.isLoggable(0x10));
		assertTrue(levels.isLoggable(0x20));

		levels = new LoggingLevels(0x3f);
		assertTrue(levels.isLoggable(0x01));
		assertTrue(levels.isLoggable(0x02));
		assertTrue(levels.isLoggable(0x04));
		assertTrue(levels.isLoggable(0x08));
		assertTrue(levels.isLoggable(0x10));
		assertTrue(levels.isLoggable(0x20));
	}

}
