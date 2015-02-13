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
package net.xenqtt.mockbroker;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import net.xenqtt.SimpleBroker;
import net.xenqtt.XenqttUtil;
import net.xenqtt.message.ConnectMessage;
import net.xenqtt.message.MqttMessage;

/**
 * Mock MQTT broker used to test MQTT clients and applications. If debug level logging is enabled all broker events will be logged.
 */
public final class MockBroker extends SimpleBroker {

	private final ConcurrentHashMap<String, String> credentials = new ConcurrentHashMap<String, String>();

	private final BrokerEvents events;
	private final BrokerMessageHandler messageHandler;

	/**
	 * Creates a broker with the following config:
	 * 
	 * <ul>
	 * <li>no {@link MockBrokerHandler}</li>
	 * <li>15 second message resend interval</li>
	 * <li>Any available port. Use {@link #getPort()} to get the port.</li>
	 * <li>allows anonymous access</li>
	 * <li>does not ignore credentials</li>
	 * <li>captures broker events</li>
	 * <li>allows 50 in-flight messages</li>
	 * </ul>
	 */
	public MockBroker() {
		this(null, 15, 0, true, false, true, 50);
	}

	/**
	 * Creates a broker with the specified {@link MockBrokerHandler handler} and the following config:
	 * 
	 * <ul>
	 * <li>15 second message resend interval</li>
	 * <li>Any available port. Use {@link #getPort()} to get the port.</li>
	 * <li>allows anonymous access</li>
	 * <li>does not ignore credentials</li>
	 * <li>captures broker events</li>
	 * <li>allows 50 in-flight messages</li>
	 * </ul>
	 */
	public MockBroker(MockBrokerHandler brokerHandler) {
		this(brokerHandler, 15, 0, true, false, true, 50);
	}

	/**
	 * Create a new instance of this class.
	 * 
	 * @param brokerHandler
	 *            Called when events happen. Can be {@code null} if you don't need to do any custom message handling.
	 * @param messageResendIntervalSeconds
	 *            Seconds between attempts to resend a message that is {@link MqttMessage#isAckable()}. 0 to disable message resends.
	 * @param port
	 *            The port for the server to listen on. 0 will choose an arbitrary available port which you can get from {@link MockBroker#getPort()} after
	 *            calling {@link #init()}.
	 * @param allowAnonymousAccess
	 *            If true then {@link ConnectMessage} with no username/password will be accepted. Otherwise only valid credentials will be accepted.
	 * @param ignoreCredentials
	 *            If true then {@link ConnectMessage} with any username/password will be accepted. Otherwise only valid credentials will be accepted.
	 * @param captureBrokerEvents
	 *            If {@code true} then capture all events within the broker; otherwise, do not capture any events
	 * @param maxInFlightMessages
	 *            Maximum number of concurrent publish messages the broker will have in-flight to the client. This is an approximation. The actual maximum
	 *            number of in-flight messages may vary slightly.
	 */
	public MockBroker(MockBrokerHandler brokerHandler, long messageResendIntervalSeconds, int port, boolean allowAnonymousAccess, boolean ignoreCredentials,
			boolean captureBrokerEvents, int maxInFlightMessages) {

		super(messageResendIntervalSeconds, port);

		XenqttUtil.validateGreaterThan("maxInFlightMessages", maxInFlightMessages, 0);

		this.events = captureBrokerEvents ? new BrokerEventsImpl() : new NullBrokerEvents();
		this.messageHandler = new BrokerMessageHandler(brokerHandler, events, credentials, allowAnonymousAccess, ignoreCredentials, maxInFlightMessages);
	}

	/**
	 * Starts the mock broker
	 */
	public void init() {
		super.init(messageHandler, "MockBrokerServer");
	}

	/**
	 * Adds the specified credentials for authentication by {@link ConnectMessage}s. If password is {@code null} any existing credentials for the user are
	 * removed.
	 */
	public void addCredentials(String userName, String password) {
		if (password == null) {
			credentials.remove(userName);
		} else {
			XenqttUtil.validateNotNull("userName", userName);

			credentials.put(userName, password);
		}
	}

	/**
	 * @return All broker events. This list is a copy.
	 */
	public List<BrokerEvent> getEvents() {
		return events.getEvents();
	}

	/**
	 * @return All broker events for the specified client ID. This list is a copy.
	 */
	public List<BrokerEvent> getEvents(String clientId) {
		return events.getEvents(clientId);
	}

	/**
	 * Removes all broker events
	 */
	public void clearEvents() {
		events.clearEvents();
	}

	/**
	 * Removes the specified broker events.
	 */
	public void removeEvents(Collection<BrokerEvent> eventsToRemove) {
		XenqttUtil.validateNotNull("eventsToRemove", eventsToRemove);

		events.removeEvents(eventsToRemove);
	}
}
