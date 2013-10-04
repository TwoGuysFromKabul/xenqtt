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

import java.util.concurrent.Executor;

import net.sf.xenqtt.XenqttUtil;

/**
 * An {@link MqttClient} that interacts with an MQTT broker in a synchronous fashion. All MQTT-related operations happen in a blocking style where method
 * invocations will return once the operation completes.
 */
public final class SyncMqttClient extends AbstractMqttClient {

	/**
	 * Constructs an instance of this class using an {@link Executor} owned by this class with the default {@link MqttClientConfig config}.
	 * 
	 * @param brokerUri
	 *            The URL to the broker to connect to. For example, tcp://q.m2m.io:1883
	 * @param listener
	 *            Handles {@link PublishMessage publish messages} received by this client. Use {@link MqttClientListener#NULL_LISTENER} if you don't want to
	 *            receive messages or be notified of events.
	 * @param messageHandlerThreadPoolSize
	 *            The number of threads used to handle incoming messages and invoke the {@link MqttClientListener listener's} methods
	 */
	public SyncMqttClient(String brokerUri, MqttClientListener listener, int messageHandlerThreadPoolSize) {
		this(brokerUri, listener, messageHandlerThreadPoolSize, new MqttClientConfig());
	}

	/**
	 * Constructs an instance of this class using an {@link Executor} owned by this class with a custom {@link MqttClientConfig config}.
	 * 
	 * @param brokerUri
	 *            The URL to the broker to connect to. For example, tcp://q.m2m.io:1883
	 * @param listener
	 *            Handles {@link PublishMessage publish messages} received by this client. Use {@link MqttClientListener#NULL_LISTENER} if you don't want to
	 *            receive messages or be notified of events.
	 * @param messageHandlerThreadPoolSize
	 *            The number of threads used to handle incoming messages and invoke the {@link MqttClientListener listener's} methods
	 * @param config
	 *            The configuration for the client
	 */
	public SyncMqttClient(String brokerUri, MqttClientListener listener, int messageHandlerThreadPoolSize, MqttClientConfig config) {
		super(XenqttUtil.validateNotEmpty("brokerUri", brokerUri), //
				XenqttUtil.validateNotNull("listener", listener), //
				XenqttUtil.validateGreaterThan("messageHandlerThreadPoolSize", messageHandlerThreadPoolSize, 0), //
				config);
	}

	/**
	 * Constructs an instance of this class using a user provided {@link Executor} with the default {@link MqttClientConfig config}.
	 * 
	 * @param brokerUri
	 *            The URL to the broker to connect to. For example, tcp://q.m2m.io:1883
	 * @param listener
	 *            Handles {@link PublishMessage publish messages} received by this client. Use {@link MqttClientListener#NULL_LISTENER} if you don't want to
	 *            receive messages or be notified of events.
	 * @param executor
	 *            The executor used to handle incoming messages and invoke the {@link MqttClientListener listener's} methods. This class will NOT shut down the
	 *            executor.
	 */
	public SyncMqttClient(String brokerUri, MqttClientListener listener, Executor executor) {
		this(brokerUri, listener, executor, new MqttClientConfig());
	}

	/**
	 * Constructs an instance of this class using a user provided {@link Executor} with a custom {@link MqttClientConfig config}.
	 * 
	 * @param brokerUri
	 *            The URL to the broker to connect to. For example, tcp://q.m2m.io:1883
	 * @param listener
	 *            Handles {@link PublishMessage publish messages} received by this client. Use {@link MqttClientListener#NULL_LISTENER} if you don't want to
	 *            receive messages or be notified of events.
	 * @param executor
	 *            The executor used to handle incoming messages and invoke the {@link MqttClientListener listener's} methods. This class will NOT shut down the
	 *            executor.
	 * @param config
	 *            The configuration for the client
	 */
	public SyncMqttClient(String brokerUri, MqttClientListener listener, Executor executor, MqttClientConfig config) {
		super(XenqttUtil.validateNotEmpty("brokerUri", brokerUri), //
				XenqttUtil.validateNotNull("listener", listener), //
				XenqttUtil.validateNotNull("executor", executor), //
				config);
	}
}
