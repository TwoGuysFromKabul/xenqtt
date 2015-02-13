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

import net.xenqtt.client.LatencyStat;

/**
 * A {@link LatencyStat} implementation that facilitates the aggregation and processing of latency data for MQTT messages exchanged between the client and the
 * broker.
 */
final class LatencyStatImpl implements LatencyStat, Cloneable {

	private long count;
	private long sum;
	private long min;
	private long max;

	/**
	 * @see net.xenqtt.client.LatencyStat#getCount()
	 */
	@Override
	public long getCount() {
		return count;
	}

	/**
	 * @see net.xenqtt.client.LatencyStat#getMin()
	 */
	@Override
	public long getMin() {
		return min;
	}

	/**
	 * @see net.xenqtt.client.LatencyStat#getMax()
	 */
	@Override
	public long getMax() {
		return max;
	}

	/**
	 * @see net.xenqtt.client.LatencyStat#getAverage()
	 */
	@Override
	public double getAverage() {
		if (count == 0) {
			return 0.0;
		}

		return (sum * 1.0) / count;
	}

	/**
	 * Process a reported latency. This involves updating the min and max values received thus far and incrementing the count.
	 * 
	 * @param latency
	 *            The latency that was reported
	 */
	void processLatency(long latency) {
		count++;
		sum += latency;
		if (min == 0 || latency < min) {
			min = latency;
		}

		if (max == 0 || latency > max) {
			max = latency;
		}
	}

	/**
	 * Reset this {@link LatencyStatImpl instance}. This involves zeroing out all relevant internal values.
	 */
	void reset() {
		count = sum = min = max = 0;
	}

	/**
	 * @see java.lang.Object#clone()
	 */
	@Override
	public LatencyStatImpl clone() throws CloneNotSupportedException {
		return (LatencyStatImpl) super.clone();
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		return String.format("{count: %d, min: %d, max: %d, avg: %.3f}", count, min, max, getAverage());
	}
}
