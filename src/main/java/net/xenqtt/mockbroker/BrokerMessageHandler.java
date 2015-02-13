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

import static net.xenqtt.mockbroker.BrokerEventType.*;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.xenqtt.message.ConnAckMessage;
import net.xenqtt.message.ConnectMessage;
import net.xenqtt.message.ConnectReturnCode;
import net.xenqtt.message.DisconnectMessage;
import net.xenqtt.message.MessageHandler;
import net.xenqtt.message.MqttChannel;
import net.xenqtt.message.MqttMessage;
import net.xenqtt.message.PubAckMessage;
import net.xenqtt.message.PubCompMessage;
import net.xenqtt.message.PubMessage;
import net.xenqtt.message.PubRecMessage;
import net.xenqtt.message.PubRelMessage;
import net.xenqtt.message.QoS;
import net.xenqtt.message.SubAckMessage;
import net.xenqtt.message.SubscribeMessage;
import net.xenqtt.message.UnsubAckMessage;
import net.xenqtt.message.UnsubscribeMessage;

/**
 * Handles callbacks from {@link MqttChannel}s for the {@link MockBroker}
 */
final class BrokerMessageHandler implements MessageHandler {

	private final Map<MqttChannel, Client> clientByChannel = new IdentityHashMap<MqttChannel, Client>();
	private final Map<String, Client> clientById = new HashMap<String, Client>();
	private final TopicManager topicManager = new TopicManager(clientById);

	private final MockBrokerHandler brokerHandler;
	private final BrokerEvents events;
	private final ConcurrentHashMap<String, String> credentials;
	private final boolean allowAnonymousAccess;
	private final boolean ignoreCredentials;
	private final int maxInFlightMessages;

	/**
	 * Create a new instance of this class.
	 * 
	 * @param brokerHandler
	 *            The {@link MockBrokerHandler broker handler} to use in this {@link BrokerMessageHandler message handler}
	 * @param events
	 *            The {@link BrokerEvents events} associated with the broker
	 * @param credentials
	 *            The credentials to use in this broker message handler
	 * @param allowAnonymousAccess
	 *            Whether or not anonymous access is allowed ({@code true}) or not ({@code false})
	 * @param ignoreCredentials
	 *            If true then {@link ConnectMessage} with any username/password will be accepted. Otherwise only valid credentials will be accepted.
	 * @param maxInFlightMessages
	 *            The maximum number of in-flight messages that are allowed
	 */
	BrokerMessageHandler(MockBrokerHandler brokerHandler, BrokerEvents events, ConcurrentHashMap<String, String> credentials, boolean allowAnonymousAccess,
			boolean ignoreCredentials, int maxInFlightMessages) {
		this.credentials = credentials;
		this.allowAnonymousAccess = allowAnonymousAccess;
		this.ignoreCredentials = ignoreCredentials;
		this.maxInFlightMessages = maxInFlightMessages;
		this.brokerHandler = brokerHandler == null ? new MockBrokerHandler() : brokerHandler;
		this.events = events;
	}

	/**
	 * @see net.xenqtt.message.MessageHandler#connect(net.xenqtt.message.MqttChannel, net.xenqtt.message.ConnectMessage)
	 */
	@Override
	public void connect(MqttChannel channel, ConnectMessage message) throws Exception {

		Client client = clientByChannel.get(channel);
		client.clientId = message.getClientId();
		client.cleanSession = message.isCleanSession();

		Client oldClient = clientById.put(client.clientId, client);
		if (oldClient != null) {
			oldClient.close();
		}

		client.messageReceived(message);

		if (brokerHandler.connect(client, message)) {
			return;
		}

		ConnectReturnCode returnCode = ConnectReturnCode.ACCEPTED;
		String user = message.getUserName();
		String password = message.getPassword();
		if (user == null) {
			if (!allowAnonymousAccess) {
				returnCode = ConnectReturnCode.NOT_AUTHORIZED;
			}
		} else if (!ignoreCredentials && (password == null || !password.equals(credentials.get(user)))) {
			returnCode = ConnectReturnCode.BAD_CREDENTIALS;
		}
		if (client.clientId.length() < 1 || client.clientId.length() > 23) {
			returnCode = ConnectReturnCode.IDENTIFIER_REJECTED;
		}
		if (message.getProtocolVersion() != 3) {
			returnCode = ConnectReturnCode.UNACCEPTABLE_PROTOCOL_VERSION;
		}

		client.send(new ConnAckMessage(returnCode));
		if (returnCode == ConnectReturnCode.ACCEPTED) {
			topicManager.connected(client, message);
		}

	}

	/**
	 * @see net.xenqtt.message.MessageHandler#connAck(net.xenqtt.message.MqttChannel, net.xenqtt.message.ConnAckMessage)
	 */
	@Override
	public void connAck(MqttChannel channel, ConnAckMessage message) throws Exception {
		Client client = getClient(channel);
		client.messageReceived(message);
		brokerHandler.unexpectedMessage(client, message);
	}

	/**
	 * @see net.xenqtt.message.MessageHandler#publish(net.xenqtt.message.MqttChannel, net.xenqtt.message.PubMessage)
	 */
	@Override
	public void publish(MqttChannel channel, PubMessage message) throws Exception {

		Client client = getClient(channel);
		client.messageReceived(message);

		if (brokerHandler.publish(client, message)) {
			return;
		}

		topicManager.publish(message);

		if (message.getQoSLevel() > 0) {
			client.send(new PubAckMessage(message.getMessageId()));
		}
	}

	/**
	 * @see net.xenqtt.message.MessageHandler#pubAck(net.xenqtt.message.MqttChannel, net.xenqtt.message.PubAckMessage)
	 */
	@Override
	public void pubAck(MqttChannel channel, PubAckMessage message) throws Exception {

		Client client = getClient(channel);
		client.messageReceived(message);

		if (brokerHandler.pubAck(client, message)) {
			return;
		}

		topicManager.pubAcked(client, message);
	}

	/**
	 * @see net.xenqtt.message.MessageHandler#pubRec(net.xenqtt.message.MqttChannel, net.xenqtt.message.PubRecMessage)
	 */
	@Override
	public void pubRec(MqttChannel channel, PubRecMessage message) throws Exception {
		// QOS 2 not supported

		Client client = getClient(channel);
		client.messageReceived(message);

		if (brokerHandler.pubRec(client, message)) {
			return;
		}
	}

	/**
	 * @see net.xenqtt.message.MessageHandler#pubRel(net.xenqtt.message.MqttChannel, net.xenqtt.message.PubRelMessage)
	 */
	@Override
	public void pubRel(MqttChannel channel, PubRelMessage message) throws Exception {
		// QOS 2 not supported

		Client client = getClient(channel);
		client.messageReceived(message);

		if (brokerHandler.pubRel(client, message)) {
			return;
		}

	}

	/**
	 * @see net.xenqtt.message.MessageHandler#pubComp(net.xenqtt.message.MqttChannel, net.xenqtt.message.PubCompMessage)
	 */
	@Override
	public void pubComp(MqttChannel channel, PubCompMessage message) throws Exception {
		// QOS 2 not supported

		Client client = getClient(channel);
		client.messageReceived(message);

		if (brokerHandler.pubComp(client, message)) {
			return;
		}
	}

	/**
	 * @see net.xenqtt.message.MessageHandler#subscribe(net.xenqtt.message.MqttChannel, net.xenqtt.message.SubscribeMessage)
	 */
	@Override
	public void subscribe(MqttChannel channel, SubscribeMessage message) throws Exception {

		Client client = clientByChannel.get(channel);

		client.messageReceived(message);

		if (brokerHandler.subscribe(client, message)) {
			return;
		}

		QoS[] grantedQoses = topicManager.subscribe(client, message);

		client.send(new SubAckMessage(message.getMessageId(), grantedQoses));
	}

	/**
	 * @see net.xenqtt.message.MessageHandler#subAck(net.xenqtt.message.MqttChannel, net.xenqtt.message.SubAckMessage)
	 */
	@Override
	public void subAck(MqttChannel channel, SubAckMessage message) throws Exception {

		Client client = getClient(channel);
		client.messageReceived(message);

		brokerHandler.unexpectedMessage(client, message);
	}

	/**
	 * @see net.xenqtt.message.MessageHandler#unsubscribe(net.xenqtt.message.MqttChannel, net.xenqtt.message.UnsubscribeMessage)
	 */
	@Override
	public void unsubscribe(MqttChannel channel, UnsubscribeMessage message) throws Exception {

		Client client = getClient(channel);
		client.messageReceived(message);

		if (brokerHandler.unsubscribe(client, message)) {
			return;
		}

		topicManager.unsubscribe(client, message);

		client.send(new UnsubAckMessage(message.getMessageId()));
	}

	/**
	 * @see net.xenqtt.message.MessageHandler#unsubAck(net.xenqtt.message.MqttChannel, net.xenqtt.message.UnsubAckMessage)
	 */
	@Override
	public void unsubAck(MqttChannel channel, UnsubAckMessage message) throws Exception {

		Client client = getClient(channel);
		client.messageReceived(message);

		brokerHandler.unexpectedMessage(client, message);
	}

	/**
	 * @see net.xenqtt.message.MessageHandler#disconnect(net.xenqtt.message.MqttChannel, net.xenqtt.message.DisconnectMessage)
	 */
	@Override
	public void disconnect(MqttChannel channel, DisconnectMessage message) throws Exception {

		Client client = getClient(channel);
		client.messageReceived(message);

		brokerHandler.disconnect(client, message);

		client.close();
	}

	/**
	 * @see net.xenqtt.message.MessageHandler#channelOpened(net.xenqtt.message.MqttChannel)
	 */
	@Override
	public void channelOpened(MqttChannel channel) {

		Client client = new Client(channel, events, maxInFlightMessages);
		clientByChannel.put(channel, client);
		events.addEvent(CHANNEL_OPENED, client);

		brokerHandler.channelOpened(client);
	}

	/**
	 * @see net.xenqtt.message.MessageHandler#channelClosed(net.xenqtt.message.MqttChannel, java.lang.Throwable)
	 */
	@Override
	public void channelClosed(MqttChannel channel, Throwable cause) {

		Client client = clientByChannel.remove(channel);
		events.addEvent(CHANNEL_CLOSED, client);

		Client otherClient = clientById.get(client.clientId);
		if (client == otherClient) {
			clientById.remove(client.clientId);
		}

		topicManager.clientClosed(client);

		brokerHandler.channelClosed(client, cause);
	}

	private Client getClient(MqttChannel channel) {
		return clientByChannel.get(channel);
	}

	/**
	 * @see net.xenqtt.message.MessageHandler#channelAttached(net.xenqtt.message.MqttChannel)
	 */
	@Override
	public void channelAttached(MqttChannel channel) {
		// this should never be called for the mock broker
	}

	@Override
	public void channelDetached(MqttChannel channel) {
		// this should never be called for the mock broker
	}

	/**
	 * @see net.xenqtt.message.MessageHandler#messageSent(net.xenqtt.message.MqttChannel, net.xenqtt.message.MqttMessage)
	 */
	@Override
	public void messageSent(MqttChannel channel, MqttMessage message) {
	}
}
