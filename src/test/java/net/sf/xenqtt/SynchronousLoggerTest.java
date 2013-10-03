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

import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SynchronousLoggerTest {

	static final LoggingLevels TRACE_LEVEL = new LoggingLevels(0x3f);
	static final LoggingLevels DEBUG_LEVEL = new LoggingLevels(0x3e);
	static final LoggingLevels INFO_LEVEL = new LoggingLevels(0x3c);
	static final LoggingLevels WARN_LEVEL = new LoggingLevels(0x38);
	static final LoggingLevels ERROR_LEVEL = new LoggingLevels(0x30);
	static final LoggingLevels FATAL_LEVEL = new LoggingLevels(0x20);

	@Mock LoggingDelegate delegate;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testLog_TraceEnabled() throws Exception {
		SynchronousLogger logger = new SynchronousLogger(TRACE_LEVEL, delegate);
		logger.init();

		logger.log(LoggingLevels.TRACE_FLAG, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.DEBUG_FLAG, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.INFO_FLAG, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.WARN_FLAG, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.ERROR_FLAG, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.FATAL_FLAG, "I am the very model of a modern Major %s", "General");

		verify(delegate).trace(null, "I am the very model of a modern Major General");
		verify(delegate).debug(null, "I am the very model of a modern Major General");
		verify(delegate).info(null, "I am the very model of a modern Major General");
		verify(delegate).warn(null, "I am the very model of a modern Major General");
		verify(delegate).error(null, "I am the very model of a modern Major General");
		verify(delegate).fatal(null, "I am the very model of a modern Major General");
		verifyNoMoreInteractions(delegate);
	}

	@Test
	public void testLog_TraceEnabled_Exception() throws Exception {
		SynchronousLogger logger = new SynchronousLogger(TRACE_LEVEL, delegate);
		logger.init();

		Throwable t = new Throwable("Ouchies!");
		logger.log(LoggingLevels.TRACE_FLAG, t, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.DEBUG_FLAG, t, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.INFO_FLAG, t, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.WARN_FLAG, t, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.ERROR_FLAG, t, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.FATAL_FLAG, t, "I am the very model of a modern Major %s", "General");

		verify(delegate).trace(t, "I am the very model of a modern Major General");
		verify(delegate).debug(t, "I am the very model of a modern Major General");
		verify(delegate).info(t, "I am the very model of a modern Major General");
		verify(delegate).warn(t, "I am the very model of a modern Major General");
		verify(delegate).error(t, "I am the very model of a modern Major General");
		verify(delegate).fatal(t, "I am the very model of a modern Major General");
		verifyNoMoreInteractions(delegate);
	}

	@Test
	public void testLog_DebugEnabled() throws Exception {
		SynchronousLogger logger = new SynchronousLogger(DEBUG_LEVEL, delegate);
		logger.init();

		logger.log(LoggingLevels.TRACE_FLAG, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.DEBUG_FLAG, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.INFO_FLAG, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.WARN_FLAG, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.ERROR_FLAG, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.FATAL_FLAG, "I am the very model of a modern Major %s", "General");

		verify(delegate).debug(null, "I am the very model of a modern Major General");
		verify(delegate).info(null, "I am the very model of a modern Major General");
		verify(delegate).warn(null, "I am the very model of a modern Major General");
		verify(delegate).error(null, "I am the very model of a modern Major General");
		verify(delegate).fatal(null, "I am the very model of a modern Major General");
		verifyNoMoreInteractions(delegate);
	}

	@Test
	public void testLog_DebugEnabled_Exception() throws Exception {
		SynchronousLogger logger = new SynchronousLogger(DEBUG_LEVEL, delegate);
		logger.init();

		Throwable t = new Throwable("Ouchies!");
		logger.log(LoggingLevels.TRACE_FLAG, t, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.DEBUG_FLAG, t, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.INFO_FLAG, t, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.WARN_FLAG, t, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.ERROR_FLAG, t, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.FATAL_FLAG, t, "I am the very model of a modern Major %s", "General");

		verify(delegate).debug(t, "I am the very model of a modern Major General");
		verify(delegate).info(t, "I am the very model of a modern Major General");
		verify(delegate).warn(t, "I am the very model of a modern Major General");
		verify(delegate).error(t, "I am the very model of a modern Major General");
		verify(delegate).fatal(t, "I am the very model of a modern Major General");
		verifyNoMoreInteractions(delegate);
	}

	@Test
	public void testLog_InfoEnabled() throws Exception {
		SynchronousLogger logger = new SynchronousLogger(INFO_LEVEL, delegate);
		logger.init();

		logger.log(LoggingLevels.TRACE_FLAG, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.DEBUG_FLAG, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.INFO_FLAG, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.WARN_FLAG, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.ERROR_FLAG, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.FATAL_FLAG, "I am the very model of a modern Major %s", "General");

		verify(delegate).info(null, "I am the very model of a modern Major General");
		verify(delegate).warn(null, "I am the very model of a modern Major General");
		verify(delegate).error(null, "I am the very model of a modern Major General");
		verify(delegate).fatal(null, "I am the very model of a modern Major General");
		verifyNoMoreInteractions(delegate);
	}

	@Test
	public void testLog_InfoEnabled_Exception() throws Exception {
		SynchronousLogger logger = new SynchronousLogger(INFO_LEVEL, delegate);
		logger.init();

		Throwable t = new Throwable("Ouchies!");
		logger.log(LoggingLevels.TRACE_FLAG, t, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.DEBUG_FLAG, t, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.INFO_FLAG, t, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.WARN_FLAG, t, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.ERROR_FLAG, t, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.FATAL_FLAG, t, "I am the very model of a modern Major %s", "General");

		verify(delegate).info(t, "I am the very model of a modern Major General");
		verify(delegate).warn(t, "I am the very model of a modern Major General");
		verify(delegate).error(t, "I am the very model of a modern Major General");
		verify(delegate).fatal(t, "I am the very model of a modern Major General");
		verifyNoMoreInteractions(delegate);
	}

	@Test
	public void testLog_WarnEnabled() throws Exception {
		SynchronousLogger logger = new SynchronousLogger(WARN_LEVEL, delegate);
		logger.init();

		logger.log(LoggingLevels.TRACE_FLAG, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.DEBUG_FLAG, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.INFO_FLAG, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.WARN_FLAG, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.ERROR_FLAG, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.FATAL_FLAG, "I am the very model of a modern Major %s", "General");

		verify(delegate).warn(null, "I am the very model of a modern Major General");
		verify(delegate).error(null, "I am the very model of a modern Major General");
		verify(delegate).fatal(null, "I am the very model of a modern Major General");
		verifyNoMoreInteractions(delegate);
	}

	@Test
	public void testLog_WarnEnabled_Exception() throws Exception {
		SynchronousLogger logger = new SynchronousLogger(WARN_LEVEL, delegate);
		logger.init();

		Throwable t = new Throwable("Ouchies!");
		logger.log(LoggingLevels.TRACE_FLAG, t, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.DEBUG_FLAG, t, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.INFO_FLAG, t, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.WARN_FLAG, t, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.ERROR_FLAG, t, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.FATAL_FLAG, t, "I am the very model of a modern Major %s", "General");

		verify(delegate).warn(t, "I am the very model of a modern Major General");
		verify(delegate).error(t, "I am the very model of a modern Major General");
		verify(delegate).fatal(t, "I am the very model of a modern Major General");
		verifyNoMoreInteractions(delegate);
	}

	@Test
	public void testLog_ErrorEnabled() throws Exception {
		SynchronousLogger logger = new SynchronousLogger(ERROR_LEVEL, delegate);
		logger.init();

		logger.log(LoggingLevels.TRACE_FLAG, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.DEBUG_FLAG, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.INFO_FLAG, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.WARN_FLAG, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.ERROR_FLAG, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.FATAL_FLAG, "I am the very model of a modern Major %s", "General");

		verify(delegate).error(null, "I am the very model of a modern Major General");
		verify(delegate).fatal(null, "I am the very model of a modern Major General");
		verifyNoMoreInteractions(delegate);
	}

	@Test
	public void testLog_ErrorEnabled_Exception() throws Exception {
		SynchronousLogger logger = new SynchronousLogger(ERROR_LEVEL, delegate);
		logger.init();

		Throwable t = new Throwable("Ouchies!");
		logger.log(LoggingLevels.TRACE_FLAG, t, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.DEBUG_FLAG, t, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.INFO_FLAG, t, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.WARN_FLAG, t, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.ERROR_FLAG, t, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.FATAL_FLAG, t, "I am the very model of a modern Major %s", "General");

		verify(delegate).error(t, "I am the very model of a modern Major General");
		verify(delegate).fatal(t, "I am the very model of a modern Major General");
		verifyNoMoreInteractions(delegate);
	}

	@Test
	public void testLog_FatalEnabled() throws Exception {
		SynchronousLogger logger = new SynchronousLogger(FATAL_LEVEL, delegate);
		logger.init();

		logger.log(LoggingLevels.TRACE_FLAG, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.DEBUG_FLAG, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.INFO_FLAG, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.WARN_FLAG, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.ERROR_FLAG, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.FATAL_FLAG, "I am the very model of a modern Major %s", "General");

		verify(delegate).fatal(null, "I am the very model of a modern Major General");
		verifyNoMoreInteractions(delegate);
	}

	@Test
	public void testLog_FatalEnabled_Exception() throws Exception {
		SynchronousLogger logger = new SynchronousLogger(FATAL_LEVEL, delegate);
		logger.init();

		Throwable t = new Throwable("Ouchies!");
		logger.log(LoggingLevels.TRACE_FLAG, t, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.DEBUG_FLAG, t, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.INFO_FLAG, t, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.WARN_FLAG, t, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.ERROR_FLAG, t, "I am the very model of a modern Major %s", "General");
		logger.log(LoggingLevels.FATAL_FLAG, t, "I am the very model of a modern Major %s", "General");

		verify(delegate).fatal(t, "I am the very model of a modern Major General");
		verifyNoMoreInteractions(delegate);
	}

}
