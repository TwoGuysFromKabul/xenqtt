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
package net.sf.xenqtt.test;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.xenqtt.ApplicationArguments;
import net.sf.xenqtt.Log;
import net.sf.xenqtt.XenqttUtil;
import net.sf.xenqtt.client.FixedReconnectionStrategy;
import net.sf.xenqtt.client.NullReconnectStrategy;
import net.sf.xenqtt.client.ProgressiveReconnectionStrategy;
import net.sf.xenqtt.client.ReconnectionStrategy;
import net.sf.xenqtt.message.QoS;
import net.sf.xenqtt.test.XenqttTestClient.ClientType;

/**
 * Stores the configuration for the {@link XenqttTestClient Xenqtt test client}.
 */
public final class TestClientConfiguration {

	private static final Pattern DURATION_PATTERN = Pattern.compile("^(?:([0-9]{1,2})[:.])?(?:([0-9]{1,2})[:.])?(?:([0-9]{1,2})[:.])?([0-9]{1,3})$");
	private static final Pattern RECONNECTION_STRATEGY_PATTERN = Pattern
			.compile("^((?i:fixed)|(?i:progressive))\\(([0-9]+),([0-9]+),?(?:([0-9]+))?,?(?:([0-9]+))?\\)$");

	final String clientId;
	final ClientType clientType;
	final String brokerUri;
	final String username;
	final String password;
	final boolean cleanSession;
	final String topicToSubscribeTo;
	final String topicToPublishTo;
	final int publishers;
	final int messagesToPublish;
	final int messagesToReceive;
	final QoS qos;
	final long duration;
	final int messageHandlerThreadPoolSize;
	final int connectTimeoutSeconds;
	final int messageResendIntervalSeconds;
	final int blockingTimeoutSeconds;
	final ReconnectionStrategy reconnectionStrategy;
	final int maxInFlightMessages;
	final boolean unsubscribeAtEnd;

	public TestClientConfiguration(ApplicationArguments arguments) {
		Properties properties = getConfigurationProperties(arguments.getArgAsString("-c", null));
		clientId = arguments.getArgAsString("-i", null);
		clientType = ClientType.getClientType(properties.getProperty("client.type"));
		brokerUri = properties.getProperty("client.brokerUri", "tcp://localhost:1883");
		username = properties.getProperty("client.username", null);
		password = properties.getProperty("client.password", null);
		cleanSession = Boolean.parseBoolean(properties.getProperty("client.cleanSession", "true"));
		topicToSubscribeTo = properties.getProperty("client.subscribeTopic");
		topicToPublishTo = properties.getProperty("client.publishTopic");
		publishers = Integer.parseInt(properties.getProperty("client.publishers", "0"));
		messagesToPublish = Integer.parseInt(properties.getProperty("client.messagesToPublish", "0"));
		messagesToReceive = Integer.parseInt(properties.getProperty("client.messagesToReceive", "0"));
		qos = QoS.lookup(Integer.parseInt(properties.getProperty("client.qos", "0")));
		duration = getDurationMillis(properties.getProperty("client.testDuration", "0"));
		messageHandlerThreadPoolSize = Integer.parseInt(properties.getProperty("client.messageHandlerThreadPoolSize", "0"));
		connectTimeoutSeconds = Integer.parseInt(properties.getProperty("client.connectTimeoutSeconds", "0"));
		messageResendIntervalSeconds = Integer.parseInt(properties.getProperty("client.messageResendIntervalSeconds", "0"));
		blockingTimeoutSeconds = Integer.parseInt(properties.getProperty("client.blockingTimeoutSeconds", "0"));
		reconnectionStrategy = getReconnectionStrategy(properties.getProperty("client.reconnectionStrategy"));
		maxInFlightMessages = Integer.parseInt(properties.getProperty("client.maxInFlightMessages", String.valueOf(Integer.MAX_VALUE)));
		unsubscribeAtEnd = Boolean.parseBoolean(properties.getProperty("client.unsubscribeAtEnd", "false"));

		validate();
	}

	private Properties getConfigurationProperties(String configurationFile) {
		if (configurationFile == null) {
			throw new IllegalArgumentException("The configuration file was not specified.");
		}

		Properties properties = new Properties();
		try {
			File file = new File(configurationFile);
			if (!file.canRead()) {
				throw new RuntimeException(String.format("Unable to read the override file specified: %s", configurationFile));
			}

			properties.load(new FileInputStream(file));

			return properties;
		} catch (Exception ex) {
			Log.error(ex, "Unable to load the configuration properties for the test client.");
			throw new IllegalStateException(ex);
		}
	}

	private ReconnectionStrategy getReconnectionStrategy(String strategy) {
		if (strategy == null || strategy.equalsIgnoreCase("none")) {
			return new NullReconnectStrategy();
		}

		Matcher matcher = RECONNECTION_STRATEGY_PATTERN.matcher(strategy);
		if (!matcher.matches()) {
			throw new IllegalArgumentException(String.format("Invalid strategy specified: %s", strategy));
		}

		String desiredStrategy = matcher.group(1);
		if ("fixed".equalsIgnoreCase(desiredStrategy)) {
			long delayMillis = Long.parseLong(matcher.group(2));
			int attempts = Integer.parseInt(matcher.group(3));

			return new FixedReconnectionStrategy(delayMillis, attempts);
		} else if ("progressive".equalsIgnoreCase(desiredStrategy)) {
			long baseDelayMillis = Long.parseLong(matcher.group(2));
			int factor = Integer.parseInt(matcher.group(3));
			int attempts = Integer.parseInt(matcher.group(4));
			long maxMillis = Integer.parseInt(matcher.group(5));

			return new ProgressiveReconnectionStrategy(baseDelayMillis, factor, attempts, maxMillis);
		} else {
			throw new IllegalArgumentException(String.format("Unrecognized strategy: %s", desiredStrategy));
		}
	}

	private void validate() {
		if (XenqttUtil.isBlank(clientId)) {
			throw new IllegalArgumentException("The client ID cannot be omitted.");
		}

		if (XenqttUtil.isNull(topicToPublishTo) && XenqttUtil.isNull(topicToSubscribeTo)) {
			throw new IllegalStateException("Both the topic to subscribe to and the topic to publish to cannot be null.");
		}

		if (messagesToReceive == 0 && messagesToPublish == 0 && duration == 0) {
			throw new IllegalStateException("Test duration undefined. There are no message send/receive limits and no duration.");
		}
	}

	private long getDurationMillis(String testDuration) {
		Matcher matcher = DURATION_PATTERN.matcher(testDuration);
		if (!matcher.matches()) {
			throw new IllegalArgumentException(String.format("Invalid test duration format: %s", testDuration));
		}

		int[] components = getTemporalComponents(matcher);

		return computeDuration(components);
	}

	private int[] getTemporalComponents(Matcher matcher) {
		int count = matcher.groupCount();
		int[] components = new int[count];
		for (int i = 1; i <= count; i++) {
			String value = matcher.group(i);
			components[i - 1] = value != null ? Integer.parseInt(value) : 0;
		}

		return components;
	}

	private long computeDuration(int[] components) {
		long duration = components[components.length - 1];
		long multiplicationFactor = 1000;
		for (int i = components.length - 2; i >= 0; i--) {
			duration += (components[i] * multiplicationFactor);
			if (!inHours(i, components.length)) {
				multiplicationFactor *= 60;
			} else {
				multiplicationFactor *= 24;
			}
		}

		return duration;
	}

	private boolean inHours(int index, int length) {
		if (length != 4) {
			return false;
		}

		return index == 0;
	}

	boolean isTimeBasedTest() {
		return messagesToPublish == 0 && messagesToReceive == 0 && duration > 0;
	}

}
