package net.sf.xenqtt.client;

import java.util.concurrent.atomic.AtomicInteger;

import net.sf.xenqtt.XenqttUtil;

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
	private final AtomicInteger currentRetry;

	ProgressiveReconnectionStrategy(long baseReconnectMillis, int maxNumberOfRetries) {
		this(baseReconnectMillis, 2, maxNumberOfRetries);
	}

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
	 */
	public ProgressiveReconnectionStrategy(long baseReconnectMillis, int progressiveFactor, int maxNumberOfReconnects) {
		this.baseReconnectMillis = XenqttUtil.validateGreaterThan("baseReconnectMillis", baseReconnectMillis, 0L);
		this.progressiveFactor = XenqttUtil.validateGreaterThan("progressiveFactor", progressiveFactor, 0);
		this.maxNumberOfReconnects = XenqttUtil.validateGreaterThanOrEqualTo("maxNumberOfReconnects", maxNumberOfReconnects, 0);
		currentRetry = new AtomicInteger();
	}

	/**
	 * @see net.sf.xenqtt.client.ReconnectionStrategy#connectionLost(net.sf.xenqtt.client.MqttClient, java.lang.Throwable)
	 */
	@Override
	public long connectionLost(MqttClient client, Throwable cause) {
		if (currentRetry.get() >= maxNumberOfReconnects) {
			return -1;
		}

		int retry = currentRetry.getAndIncrement();
		long reconnectMillis = baseReconnectMillis;
		for (int i = 0; i < retry; i++) {
			reconnectMillis *= progressiveFactor;
		}

		return reconnectMillis;
	}

	/**
	 * @see net.sf.xenqtt.client.ReconnectionStrategy#connectionEstablished()
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
		return new ProgressiveReconnectionStrategy(baseReconnectMillis, progressiveFactor, maxNumberOfReconnects);
	}
}
