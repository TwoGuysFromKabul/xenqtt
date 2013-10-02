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

import net.sf.xenqtt.MqttException;
import net.sf.xenqtt.MqttTimeoutException;
import net.sf.xenqtt.XenqttUtil;
import net.sf.xenqtt.message.ConnAckMessage;
import net.sf.xenqtt.message.ConnectMessage;
import net.sf.xenqtt.message.MqttMessage;

/**
 * Use this class to configure the {@link MqttClient} implementations and the {@link MqttClientFactory}. This is an advanced operation. The defaults should be
 * fine for most cases.
 */
public final class MqttClientConfig implements Cloneable {

	private ReconnectionStrategy reconnectionStrategy = new ProgressiveReconnectionStrategy(50, 5, Integer.MAX_VALUE, 30000);
	private int connectTimeoutSeconds = 30;
	private int messageResendIntervalSeconds = 30;
	private int blockingTimeoutSeconds = 0;
	private int keepAliveSeconds = 300;

	/**
	 * @return The algorithm used to reconnect to the broker if the connection is lost.
	 *         <p>
	 *         Defaults to a {@link ProgressiveReconnectionStrategy} where the initial reconnection attempt happens after 50 millis then increases by a factor
	 *         of 5 up to 30 seconds where it continues to retry indefinitely.
	 */
	public ReconnectionStrategy getReconnectionStrategy() {
		return reconnectionStrategy;
	}

	/**
	 * @param reconnectionStrategy
	 *            The algorithm used to reconnect to the broker if the connection is lost.
	 *            <p>
	 *            Defaults to a {@link ProgressiveReconnectionStrategy} where the initial reconnection attempt happens after 50 millis then increases by a
	 *            factor of 5 up to 30 seconds where it continues to retry indefinitely.
	 * 
	 * @return this object
	 */
	public MqttClientConfig setReconnectionStrategy(ReconnectionStrategy reconnectionStrategy) {
		this.reconnectionStrategy = XenqttUtil.validateNotNull("reconnectionStrategy", reconnectionStrategy);
		return this;
	}

	/**
	 * @return Seconds to wait for an {@link ConnAckMessage ack} to a {@link ConnectMessage connect message} before timing out and closing the channel. 0 to
	 *         wait forever.
	 *         <p>
	 *         Defaults to 30 seconds.
	 */
	public int getConnectTimeoutSeconds() {
		return connectTimeoutSeconds;
	}

	/**
	 * @return {@link #getConnectTimeoutSeconds()} converted to milliseconds
	 */
	public int getConnectTimeoutMillis() {
		return connectTimeoutSeconds * 1000;
	}

	/**
	 * @param connectTimeoutSeconds
	 *            Seconds to wait for an {@link ConnAckMessage ack} to a {@link ConnectMessage connect message} before timing out and closing the channel. 0 to
	 *            wait forever.
	 *            <p>
	 *            Defaults to 30 seconds.
	 * 
	 * @return this object
	 */
	public MqttClientConfig setConnectTimeoutSeconds(int connectTimeoutSeconds) {
		this.connectTimeoutSeconds = XenqttUtil.validateGreaterThanOrEqualTo("connectTimeoutSeconds", connectTimeoutSeconds, 0);
		return this;
	}

	/**
	 * @return Seconds between attempts to resend a message that is {@link MqttMessage#isAckable()}. The minimum allowable value for this setting is 2.
	 *         <p>
	 *         Defaults to 30 seconds.
	 */
	public int getMessageResendIntervalSeconds() {
		return messageResendIntervalSeconds;
	}

	/**
	 * @param messageResendIntervalSeconds
	 *            Seconds between attempts to resend a message that is {@link MqttMessage#isAckable()}. The minimum allowable value for this setting is 2.
	 *            <p>
	 *            Defaults to 30 seconds.
	 * 
	 * @return this object
	 */
	public MqttClientConfig setMessageResendIntervalSeconds(int messageResendIntervalSeconds) {
		this.messageResendIntervalSeconds = XenqttUtil.validateGreaterThanOrEqualTo("messageResendIntervalSeconds", messageResendIntervalSeconds, 2);
		return this;
	}

	/**
	 * @return Seconds until a blocked method invocation times out and an {@link MqttTimeoutException} is thrown. 0 will wait forever. Applies only to
	 *         asynchronous clients. It is ignored by asynchronous clients.
	 *         <p>
	 *         Defaults to 0 (wait forever).
	 */
	public int getBlockingTimeoutSeconds() {
		return blockingTimeoutSeconds;
	}

	/**
	 * @param blockingTimeoutSeconds
	 *            Seconds until a blocked method invocation times out and an {@link MqttTimeoutException} is thrown. 0 will wait forever. Applies only to
	 *            asynchronous clients. It is ignored by asynchronous clients.
	 *            <p>
	 *            Defaults to 0 (wait forever).
	 * 
	 * @return this object
	 */
	public MqttClientConfig setBlockingTimeoutSeconds(int blockingTimeoutSeconds) {
		this.blockingTimeoutSeconds = XenqttUtil.validateGreaterThanOrEqualTo("blockingTimeoutSeconds", blockingTimeoutSeconds, 0);
		return this;
	}

	/**
	 * @return The Keep Alive timer, measured in seconds, defines the maximum time interval between messages received from a client. It enables the broker to
	 *         detect that the network connection to a client has dropped, without having to wait for the long TCP/IP timeout. In the absence of a data-related
	 *         message during the time period, this client sends a PINGREQ message, which the broker acknowledges with a PINGRESP message.
	 *         <p>
	 *         If the broker does not receive a message from the client within one and a half times the Keep Alive time period (the client is allowed "grace" of
	 *         half a time period), it disconnects the client. This action does not impact any of the client's subscriptions.
	 *         <p>
	 *         If this client does not receive a PINGRESP message within a Keep Alive time period after sending a PINGREQ, it closes the TCP/IP socket
	 *         connection.
	 *         <p>
	 *         The Keep Alive timer is a 16-bit value that represents the number of seconds for the time period. The actual value is application-specific, but a
	 *         typical value is a few minutes. The maximum value is approximately 18 hours. A value of zero (0) means the client is not disconnected.
	 *         <p>
	 *         Defaults to 300 seconds (5 minutes).
	 */
	public int getKeepAliveSeconds() {
		return keepAliveSeconds;
	}

	/**
	 * @param keepAliveSeconds
	 *            The Keep Alive timer, measured in seconds, defines the maximum time interval between messages received from a client. It enables the broker to
	 *            detect that the network connection to a client has dropped, without having to wait for the long TCP/IP timeout. In the absence of a
	 *            data-related message during the time period, this client sends a PINGREQ message, which the broker acknowledges with a PINGRESP message.
	 *            <p>
	 *            If the broker does not receive a message from the client within one and a half times the Keep Alive time period (the client is allowed "grace"
	 *            of half a time period), it disconnects the client. This action does not impact any of the client's subscriptions.
	 *            <p>
	 *            If this client does not receive a PINGRESP message within a Keep Alive time period after sending a PINGREQ, it closes the TCP/IP socket
	 *            connection.
	 *            <p>
	 *            The Keep Alive timer is a 16-bit value that represents the number of seconds for the time period. The actual value is application-specific,
	 *            but a typical value is a few minutes. The maximum value is approximately 18 hours. A value of zero (0) means the client is not disconnected.
	 *            <p>
	 *            Defaults to 300 seconds (5 minutes).
	 * 
	 * @return this object
	 */
	public MqttClientConfig setKeepAliveSeconds(int keepAliveSeconds) {
		this.keepAliveSeconds = XenqttUtil.validateGreaterThanOrEqualTo("keepAliveSeconds", keepAliveSeconds, 0);
		return this;
	}

	/**
	 * @see java.lang.Object#clone()
	 */
	@Override
	public MqttClientConfig clone() {

		try {
			MqttClientConfig other = (MqttClientConfig) super.clone();
			other.reconnectionStrategy = reconnectionStrategy.clone();
			return other;
		} catch (CloneNotSupportedException e) {
			throw new MqttException("Unable to clone client config", e);
		}
	}
}
