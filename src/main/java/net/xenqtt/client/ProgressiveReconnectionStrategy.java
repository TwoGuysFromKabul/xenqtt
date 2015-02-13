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
package net.xenqtt.client;

import java.util.concurrent.atomic.AtomicInteger;

import net.xenqtt.XenqttUtil;

/**
 * <p>
 * A {@link ReconnectionStrategy} implementation that allows for progressive reconnection attempts to the broker. Reconnect attempts begin at an aggressive
 * (configurable) interval. Subsequent attempts back off in aggressiveness with the assumption that if the broker is not nearly immediate available that an
 * issue might prevent a reconnect in the near-term but perhaps not in the longer-term.
 * </p>
 *
 * <p>
 * This class is thread-safe.
 * </p>
 */
public final class ProgressiveReconnectionStrategy implements ReconnectionStrategy {

	private final long baseReconnectMillis;
	private final int progressiveFactor;
	private final int maxNumberOfReconnects;
	private final long maxReconnectMillis;
	private final AtomicInteger currentRetry;

	/**
	 * Create a new instance of this class.
	 *
	 * @param baseReconnectMillis
	 *            The base reconnect millis to start at. The first retry is attempted at this interval
	 * @param progressiveFactor
	 *            The progressive factor. This defines how to increase the amount of time between reconnects. For example, if the base is 100 and this is 2 the
	 *            reconnects will occur as such: <code>100, 200, 400, 800, 1600, ...</code>
	 * @param maxNumberOfReconnects
	 *            The maximum number of reconnect attempts to make
	 * @param maxReconnectMillis
	 *            maximum number of millis to wait. Once the progression has reached this value all future retries will be at this interval.
	 */
	public ProgressiveReconnectionStrategy(long baseReconnectMillis, int progressiveFactor, int maxNumberOfReconnects, long maxReconnectMillis) {
		this.baseReconnectMillis = XenqttUtil.validateGreaterThan("baseReconnectMillis", baseReconnectMillis, 0L);
		this.progressiveFactor = XenqttUtil.validateGreaterThan("progressiveFactor", progressiveFactor, 0);
		this.maxNumberOfReconnects = XenqttUtil.validateGreaterThanOrEqualTo("maxNumberOfReconnects", maxNumberOfReconnects, 0);
		this.maxReconnectMillis = XenqttUtil.validateGreaterThanOrEqualTo("maxReconnectMillis", maxReconnectMillis, baseReconnectMillis);
		currentRetry = new AtomicInteger();
	}

	/**
	 * @see net.xenqtt.client.ReconnectionStrategy#connectionLost(net.xenqtt.client.MqttClient, java.lang.Throwable)
	 */
	@Override
	public long connectionLost(MqttClient client, Throwable cause) {
		if (currentRetry.get() >= maxNumberOfReconnects) {
			return -1;
		}

		int retry = currentRetry.getAndIncrement();
		long reconnectMillis = baseReconnectMillis;
		for (int i = 0; (i < retry) && (reconnectMillis < maxReconnectMillis); i++) {
			reconnectMillis *= progressiveFactor;
		}

		return reconnectMillis < maxReconnectMillis ? reconnectMillis : maxReconnectMillis;
	}

	/**
	 * @see net.xenqtt.client.ReconnectionStrategy#connectionEstablished()
	 */
	@Override
	public void connectionEstablished() {
		currentRetry.set(0);
	}

	/**
	 * @see java.lang.Object#clone()
	 * @see ReconnectionStrategy#clone()
	 */
	@Override
	public ReconnectionStrategy clone() {
		return new ProgressiveReconnectionStrategy(baseReconnectMillis, progressiveFactor, maxNumberOfReconnects, maxReconnectMillis);
	}

	/**
	 * @return The base reconnect millis to start at. The first retry is attempted at this interval
	 */
	public long getBaseReconnectMillis() {
		return baseReconnectMillis;
	}

	/**
	 * @return The progressive factor. This defines how to increase the amount of time between reconnects. For example, if the base is 100 and this is 2 the
	 *         reconnects will occur as such: <code>100, 200, 400, 800, 1600, ...</code>
	 */
	public int getProgressiveFactor() {
		return progressiveFactor;
	}

	/**
	 * @return The maximum number of reconnect attempts to make
	 */
	public int getMaxNumberOfReconnects() {
		return maxNumberOfReconnects;
	}

	/**
	 * @return The maximum number of millis to wait. Once the progression has reached this value all future retries will be at this interval
	 */
	public long getMaxReconnectMillis() {
		return maxReconnectMillis;
	}

	/**
	 * @return The current retry count. 0 if no retries have been scheduled.
	 */
	public int getCurrentRetry() {
		return currentRetry.get();
	}

}
