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
package net.sf.xenqtt.client;

import java.util.concurrent.atomic.AtomicInteger;

import net.sf.xenqtt.XenqttUtil;

/**
 * <p>
 * A {@link ReconnectStrategy} implementation that attempts to reconnect a fixed intervals up to a maximum number of reconnection attempts.
 * </p>
 * 
 * <p>
 * This class is thread-safe.
 * </p>
 */
public final class FixedReconnectionStrategy implements ReconnectionStrategy {

	private final long reconnectDelayMillis;
	private final int maxReconnectAttempts;
	private final AtomicInteger currentReconnectAttempts;

	/**
	 * Create a new instance of this class.
	 * 
	 * @param reconnectDelayMillis
	 *            The amount of time, in milliseconds, to wait before each reconnect attempt
	 * @param maxReconnectAttempts
	 *            The maximum number of times to attempt a reconnect before no further attempts will be made
	 */
	public FixedReconnectionStrategy(long reconnectDelayMillis, int maxReconnectAttempts) {
		this.reconnectDelayMillis = XenqttUtil.validateGreaterThan("reconnectDelayMillis", reconnectDelayMillis, 0L);
		this.maxReconnectAttempts = XenqttUtil.validateGreaterThanOrEqualTo("maxReconnectAttempts", maxReconnectAttempts, 0);
		currentReconnectAttempts = new AtomicInteger();
	}

	/**
	 * @see net.sf.xenqtt.client.ReconnectionStrategy#connectionLost(net.sf.xenqtt.client.MqttClient, java.lang.Throwable)
	 */
	@Override
	public long connectionLost(MqttClient client, Throwable cause) {
		if (currentReconnectAttempts.get() >= maxReconnectAttempts) {
			return -1;
		}

		currentReconnectAttempts.incrementAndGet();

		return reconnectDelayMillis;
	}

	/**
	 * @see net.sf.xenqtt.client.ReconnectionStrategy#connectionEstablished()
	 */
	@Override
	public void connectionEstablished() {
		currentReconnectAttempts.set(0);
	}

	/**
	 * @see java.lang.Object#clone()
	 * @see ReconnectionStrategy#clone()
	 */
	@Override
	public ReconnectionStrategy clone() {

		return new FixedReconnectionStrategy(reconnectDelayMillis, maxReconnectAttempts);
	}
}
