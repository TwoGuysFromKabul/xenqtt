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
package net.xenqtt.message;

import static org.junit.Assert.*;
import net.xenqtt.message.LatencyStatImpl;

import org.junit.Test;

public class LatencyStatImplTest {

	LatencyStatImpl stat = new LatencyStatImpl();

	@Test
	public void testGetMin() {
		stat.processLatency(1);
		stat.processLatency(7);
		stat.processLatency(4);
		stat.processLatency(5);
		stat.processLatency(2);
		stat.processLatency(3);
		stat.processLatency(6);

		assertEquals(1, stat.getMin());
	}

	@Test
	public void testGetMin_NoReportedLatencies() {
		assertEquals(0, stat.getMin());
	}

	@Test
	public void testGetMax() {
		stat.processLatency(1);
		stat.processLatency(7);
		stat.processLatency(4);
		stat.processLatency(5);
		stat.processLatency(2);
		stat.processLatency(3);
		stat.processLatency(6);

		assertEquals(7, stat.getMax());
	}

	@Test
	public void testGetMax_NoReportedLatencies() {
		assertEquals(0, stat.getMax());
	}

	@Test
	public void testGetAverage() {
		stat.processLatency(1);
		stat.processLatency(7);
		stat.processLatency(4);
		stat.processLatency(5);
		stat.processLatency(2);
		stat.processLatency(3);
		stat.processLatency(6);

		assertEquals(4.0, stat.getAverage(), 0.0);
	}

	@Test
	public void testGetAverage_NoReportedLatencies() {
		assertEquals(0.0, stat.getAverage(), 0.0);
	}

	@Test
	public void testGetCount() {
		stat.processLatency(1);
		stat.processLatency(7);
		stat.processLatency(4);
		stat.processLatency(5);
		stat.processLatency(2);
		stat.processLatency(3);
		stat.processLatency(6);

		assertEquals(7, stat.getCount());
	}

	@Test
	public void testGetCount_NoReportedLatencies() {
		assertEquals(0, stat.getCount());
	}

	@Test
	public void testReset() {
		stat.processLatency(1);
		stat.processLatency(7);
		stat.processLatency(4);
		stat.processLatency(5);
		stat.processLatency(2);
		stat.processLatency(3);
		stat.processLatency(6);

		assertEquals(7, stat.getCount());
		assertEquals(1, stat.getMin());
		assertEquals(7, stat.getMax());
		assertEquals(4.0, stat.getAverage(), 0.0);

		stat.reset();

		assertEquals(0, stat.getCount());
		assertEquals(0, stat.getMin());
		assertEquals(0, stat.getMax());
		assertEquals(0.0, stat.getAverage(), 0.0);
	}

	@Test
	public void testClone() throws Exception {
		stat.processLatency(1);
		stat.processLatency(7);
		stat.processLatency(4);
		stat.processLatency(5);
		stat.processLatency(2);
		stat.processLatency(3);
		stat.processLatency(6);

		assertEquals(7, stat.getCount());
		assertEquals(1, stat.getMin());
		assertEquals(7, stat.getMax());
		assertEquals(4.0, stat.getAverage(), 0.0);

		LatencyStatImpl clone = stat.clone();
		assertEquals(7, clone.getCount());
		assertEquals(1, clone.getMin());
		assertEquals(7, clone.getMax());
		assertEquals(4.0, clone.getAverage(), 0.0);

		stat.processLatency(-1);
		stat.processLatency(9);
		assertEquals(7, clone.getCount());
		assertEquals(1, clone.getMin());
		assertEquals(7, clone.getMax());
		assertEquals(4.0, clone.getAverage(), 0.0);
	}

}
