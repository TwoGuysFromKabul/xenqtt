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
package net.xenqtt.proxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.xenqtt.Log;
import net.xenqtt.message.ChannelManager;
import net.xenqtt.message.ChannelManagerImpl;
import net.xenqtt.message.ConnAckMessage;
import net.xenqtt.message.ConnectMessage;
import net.xenqtt.message.ConnectReturnCode;
import net.xenqtt.message.DisconnectMessage;
import net.xenqtt.message.IdentifiableMqttMessage;
import net.xenqtt.message.MessageHandler;
import net.xenqtt.message.MqttChannel;
import net.xenqtt.message.MqttMessage;
import net.xenqtt.message.PubAckMessage;
import net.xenqtt.message.PubCompMessage;
import net.xenqtt.message.PubMessage;
import net.xenqtt.message.PubRecMessage;
import net.xenqtt.message.PubRelMessage;
import net.xenqtt.message.SubAckMessage;
import net.xenqtt.message.SubscribeMessage;
import net.xenqtt.message.UnsubAckMessage;
import net.xenqtt.message.UnsubscribeMessage;

/**
 * Each instance of this class controls one proxy session. A session consists of one connection to a broker and connections to all the clients in a cluster.
 */
class ProxySession implements MessageHandler {

	private enum ConnectionState {
		PENDING, CONNECTED, DISCONNECTED
	}

	private final int maxInFlightBrokerMessages;

	private final Map<Integer, MessageSource> messageSourceByBrokerMessageId = new HashMap<Integer, MessageSource>();
	private final Map<MqttChannel, ConnectMessage> connectMessageByChannelPendingAttach = new ConcurrentHashMap<MqttChannel, ConnectMessage>();
	private final Set<MqttChannel> channelsPendingBroker = new HashSet<MqttChannel>();

	private final ChannelManager channelManager;
	private final String brokerUri;
	private final String clientId;
	private final ConnectMessage originalConnectMessage;

	private final List<MqttChannel> channelsToClients = new ArrayList<MqttChannel>();
	private MqttChannel channelToBroker;
	private ConnectReturnCode brokerConnectReturnCode;

	private ConnectionState brokerConnectionState = ConnectionState.PENDING;

	private int nextIdToBroker = 1;
	private int nextBrokerChannelIndex;
	private long enablePauseMessageTime;

	private volatile boolean sessionClosed;

	/**
	 * Create a new instance of this class.
	 * 
	 * @param brokerUri
	 *            The URI of the broker the proxy should connect to
	 * @param connectMessage
	 *            The {@link ConnectMessage connect message} the proxy will use for the broker
	 * @param maxInFlightBrokerMessages
	 *            Maximum number of messages that may be in-flight to the broker at a time
	 */
	public ProxySession(String brokerUri, ConnectMessage connectMessage, int maxInFlightBrokerMessages) {

		this(brokerUri, connectMessage, new ChannelManagerImpl(0), maxInFlightBrokerMessages);
	}

	/**
	 * For unit testing only
	 */
	ProxySession(String brokerUri, ConnectMessage connectMessage, ChannelManager channelManager, int maxInFlightBrokerMessages) {

		this.brokerUri = brokerUri;
		this.originalConnectMessage = connectMessage;
		this.channelManager = channelManager;
		this.maxInFlightBrokerMessages = maxInFlightBrokerMessages;
		this.clientId = originalConnectMessage.getClientId();
	}

	/**
	 * Initializes this session
	 */
	public void init() {
		channelManager.init();
		channelManager.newClientChannel(brokerUri, this);
	}

	/**
	 * Shuts down this session. Closes all connections.
	 */
	public void shutdown() {
		channelManager.shutdown();
		sessionClosed = true;
	}

	/**
	 * @return True if the session is closed. False otherwise. A session is closed when the connection to the broker is closed or when {@link #shutdown()} is
	 *         called.
	 */
	public boolean isClosed() {

		return sessionClosed;
	}

	/**
	 * @return This session's client ID
	 */
	public String getClientId() {
		return clientId;
	}

	/**
	 * Called when a new client connection is received by the {@link ServerMessageHandler}
	 * 
	 * @return True if the connection is accepted by the session. False if the session is closed
	 */
	public boolean newConnection(MqttChannel channel, ConnectMessage connectMessage) {

		if (sessionClosed) {
			return false;
		}

		connectMessageByChannelPendingAttach.put(channel, connectMessage);
		channelManager.attachChannel(channel, this);

		return true;
	}

	/**
	 * @see net.xenqtt.message.MessageHandler#connect(net.xenqtt.message.MqttChannel, net.xenqtt.message.ConnectMessage)
	 */
	@Override
	public void connect(MqttChannel channel, ConnectMessage message) throws Exception {

		if (channel == channelToBroker) {
			Log.warn("Received a %s message from the broker at %s. This should never happen. clientId=%s", message.getMessageType(),
					channel.getRemoteAddress(), clientId);
		} else {
			Log.warn("Received a %s message from clustered client %s. This should never happen. clientId=%s", message.getMessageType(),
					channel.getRemoteAddress(), clientId);
		}
	}

	/**
	 * @see net.xenqtt.message.MessageHandler#connAck(net.xenqtt.message.MqttChannel, net.xenqtt.message.ConnAckMessage)
	 */
	@Override
	public void connAck(MqttChannel channel, ConnAckMessage message) throws Exception {

		if (channel != channelToBroker) {
			Log.warn("Received a %s message from clustered client %s. This should never happen. clientId=%s", message.getMessageType(),
					channel.getRemoteAddress(), clientId);
			return;
		}

		brokerConnectReturnCode = message.getReturnCode();

		if (brokerConnectReturnCode == ConnectReturnCode.ACCEPTED) {
			brokerConnectionState = ConnectionState.CONNECTED;
		} else {
			Log.info("Broker %s rejected connect attempt with return code %s", brokerUri, brokerConnectReturnCode);
			brokerConnectionState = ConnectionState.DISCONNECTED;
		}

		for (MqttChannel channelToClient : channelsPendingBroker) {
			newSessionClient(channelToClient);
		}
		channelsPendingBroker.clear();
	}

	/**
	 * @see net.xenqtt.message.MessageHandler#publish(net.xenqtt.message.MqttChannel, net.xenqtt.message.PubMessage)
	 */
	@Override
	public void publish(MqttChannel channel, PubMessage message) throws Exception {
		forwardMessage(channel, message);
	}

	/**
	 * @see net.xenqtt.message.MessageHandler#pubAck(net.xenqtt.message.MqttChannel, net.xenqtt.message.PubAckMessage)
	 */
	@Override
	public void pubAck(MqttChannel channel, PubAckMessage message) throws Exception {
		forwardMessage(channel, message);
	}

	/**
	 * @see net.xenqtt.message.MessageHandler#pubRec(net.xenqtt.message.MqttChannel, net.xenqtt.message.PubRecMessage)
	 */
	@Override
	public void pubRec(MqttChannel channel, PubRecMessage message) throws Exception {
		// qos 2 not supported
	}

	/**
	 * @see net.xenqtt.message.MessageHandler#pubRel(net.xenqtt.message.MqttChannel, net.xenqtt.message.PubRelMessage)
	 */
	@Override
	public void pubRel(MqttChannel channel, PubRelMessage message) throws Exception {
		// qos 2 not supported
	}

	/**
	 * @see net.xenqtt.message.MessageHandler#pubComp(net.xenqtt.message.MqttChannel, net.xenqtt.message.PubCompMessage)
	 */
	@Override
	public void pubComp(MqttChannel channel, PubCompMessage message) throws Exception {
		// qos 2 not supported
	}

	/**
	 * @see net.xenqtt.message.MessageHandler#subscribe(net.xenqtt.message.MqttChannel, net.xenqtt.message.SubscribeMessage)
	 */
	@Override
	public void subscribe(MqttChannel channel, SubscribeMessage message) throws Exception {
		forwardMessage(channel, message);
	}

	/**
	 * @see net.xenqtt.message.MessageHandler#subAck(net.xenqtt.message.MqttChannel, net.xenqtt.message.SubAckMessage)
	 */
	@Override
	public void subAck(MqttChannel channel, SubAckMessage message) throws Exception {
		forwardMessage(channel, message);
	}

	/**
	 * @see net.xenqtt.message.MessageHandler#unsubscribe(net.xenqtt.message.MqttChannel, net.xenqtt.message.UnsubscribeMessage)
	 */
	@Override
	public void unsubscribe(MqttChannel channel, UnsubscribeMessage message) throws Exception {
		forwardMessage(channel, message);
	}

	/**
	 * @see net.xenqtt.message.MessageHandler#unsubAck(net.xenqtt.message.MqttChannel, net.xenqtt.message.UnsubAckMessage)
	 */
	@Override
	public void unsubAck(MqttChannel channel, UnsubAckMessage message) throws Exception {
		forwardMessage(channel, message);
	}

	/**
	 * @see net.xenqtt.message.MessageHandler#disconnect(net.xenqtt.message.MqttChannel, net.xenqtt.message.DisconnectMessage)
	 */
	@Override
	public void disconnect(MqttChannel channel, DisconnectMessage message) throws Exception {
		// ignore, disconnected clients are handled in channelClosed(...)
	}

	/**
	 * @see net.xenqtt.message.MessageHandler#channelOpened(net.xenqtt.message.MqttChannel)
	 */
	@Override
	public void channelOpened(MqttChannel channel) {
		channelToBroker = channel;
		channelToBroker.send(originalConnectMessage);
	}

	/**
	 * @see net.xenqtt.message.MessageHandler#channelClosed(net.xenqtt.message.MqttChannel, java.lang.Throwable)
	 */
	@Override
	public void channelClosed(MqttChannel channel, Throwable cause) {

		if (channel == channelToBroker) {
			brokerConnectionState = ConnectionState.DISCONNECTED;

			for (MqttChannel channelToClient : channelsToClients) {
				channelToClient.close();
			}
			channelsToClients.clear();

			sessionClosed = true;

		} else if (brokerConnectionState != ConnectionState.DISCONNECTED) {

			Iterator<MessageSource> iter = messageSourceByBrokerMessageId.values().iterator();
			while (iter.hasNext()) {
				MessageSource source = iter.next();
				if (source.sourceChannel == channel) {
					iter.remove();
					if (messageSourceByBrokerMessageId.size() == maxInFlightBrokerMessages - 1) {
						resumeRead();
					}
				}
			}

			channelsToClients.remove(channel);

			if (channelsToClients.isEmpty()) {
				channelToBroker.send(new DisconnectMessage());
			} else {
				distributeMessagesForChannel(channel);
			}
		}
	}

	/**
	 * @see net.xenqtt.message.MessageHandler#channelAttached(net.xenqtt.message.MqttChannel)
	 */
	@Override
	public void channelAttached(MqttChannel channel) {

		ConnectMessage connectMessage = connectMessageByChannelPendingAttach.remove(channel);

		if (connectMessage == null) {
			Log.warn("Channel attached with no connect message. This is most likely a bug. clientId: %s, channel: %s", clientId, channel);
			channel.send(new ConnAckMessage(ConnectReturnCode.OTHER));
			return;
		}

		if (connectMessage.isCleanSession()) {
			Log.warn("Proxied connections cannot have the clean session flag set in the connect message; clientId: %s", clientId);
			channel.send(new ConnAckMessage(ConnectReturnCode.OTHER));
			return;
		}

		if (originalConnectMessage.getProtocolVersion() != connectMessage.getProtocolVersion()) {
			Log.warn("Connect message protocol version does not match; clientId: %s, expected: %d, actual: %d", clientId,
					originalConnectMessage.getProtocolVersion(), connectMessage.getProtocolVersion());
			channel.send(new ConnAckMessage(ConnectReturnCode.UNACCEPTABLE_PROTOCOL_VERSION));
			return;
		}

		if (!stringEquqls(originalConnectMessage.getProtocolName(), connectMessage.getProtocolName())) {
			Log.warn("Connect message protocol name does not match; clientId: %s, expected: %s, actual: %s", clientId,
					originalConnectMessage.getProtocolName(), connectMessage.getProtocolName());
			channel.send(new ConnAckMessage(ConnectReturnCode.OTHER));
			return;
		}

		if (originalConnectMessage.isUserNameFlag() != connectMessage.isUserNameFlag()
				|| originalConnectMessage.isPasswordFlag() != connectMessage.isPasswordFlag()
				|| !stringEquqls(originalConnectMessage.getUserName(), connectMessage.getUserName())
				|| !stringEquqls(originalConnectMessage.getPassword(), connectMessage.getPassword())) {
			Log.warn("Connect message username/password does not match; clientId: %s, expected: %s/%s, actual: %s/%s", clientId,
					originalConnectMessage.getUserName(), originalConnectMessage.getPassword(), connectMessage.getUserName(), connectMessage.getPassword());
			channel.send(new ConnAckMessage(ConnectReturnCode.BAD_CREDENTIALS));
			return;
		}

		if (originalConnectMessage.isWillMessageFlag() != connectMessage.isWillMessageFlag()
				|| originalConnectMessage.isWillRetain() != connectMessage.isWillRetain()
				|| originalConnectMessage.getWillQoSLevel() != connectMessage.getWillQoSLevel()
				|| !stringEquqls(originalConnectMessage.getWillTopic(), connectMessage.getWillTopic())
				|| !stringEquqls(originalConnectMessage.getWillMessage(), connectMessage.getWillMessage())) {

			Log.warn(
					"Connect message will message config does not match; clientId: %s, expected topic|qos|retain|message: %s|%s|%s|%s, actual topic|qos|retain|message: %s|%s|%s|%s",
					connectMessage.getClientId(), originalConnectMessage.getWillTopic(), originalConnectMessage.getWillQoS(),
					originalConnectMessage.isWillRetain(), originalConnectMessage.getWillMessage(), connectMessage.getWillTopic(), connectMessage.getWillQoS(),
					connectMessage.isWillRetain(), connectMessage.getWillMessage());
			channel.send(new ConnAckMessage(ConnectReturnCode.OTHER));
			return;
		}

		if (brokerConnectionState == ConnectionState.PENDING) {
			Log.debug("New client connection waiting for broker connection to complete; client ID: %s", clientId);
			channelsPendingBroker.add(channel);
			return;
		}

		newSessionClient(channel);
	}

	/**
	 * @see net.xenqtt.message.MessageHandler#channelDetached(net.xenqtt.message.MqttChannel)
	 */
	@Override
	public void channelDetached(MqttChannel channel) {
		// this should never happen
	}

	/**
	 * @see net.xenqtt.message.MessageHandler#messageSent(net.xenqtt.message.MqttChannel, net.xenqtt.message.MqttMessage)
	 */
	@Override
	public void messageSent(MqttChannel channel, MqttMessage message) {
		// ignore
	}

	private void newSessionClient(MqttChannel channel) {

		assert brokerConnectionState != ConnectionState.PENDING;

		if (brokerConnectionState == ConnectionState.DISCONNECTED) {
			Log.warn("Attempting to connect a clustered client to a session with a closed broker connection. clientId: %s, address: %s", clientId,
					channel.getRemoteAddress());
			ConnectReturnCode returnCode = brokerConnectReturnCode == null ? ConnectReturnCode.OTHER : brokerConnectReturnCode;
			if (returnCode == ConnectReturnCode.ACCEPTED) {
				returnCode = ConnectReturnCode.SERVER_UNAVAILABLE;
			}
			channel.send(new ConnAckMessage(returnCode));
		} else {
			Log.info("New client connection accepted into cluster; clientId: %s, address: %s", clientId, channel.getRemoteAddress());
			channel.send(new ConnAckMessage(brokerConnectReturnCode));
			if (messageSourceByBrokerMessageId.size() == maxInFlightBrokerMessages) {
				channel.pauseRead();
			}
			channelsToClients.add(channel);
		}
	}

	private void forwardMessage(MqttChannel channel, IdentifiableMqttMessage message) {

		if (channel == channelToBroker) {
			forwardToClient(message);
		} else {
			forwardToBroker(channel, message);
		}
	}

	private void forwardToBroker(MqttChannel channelToClient, IdentifiableMqttMessage message) {

		if (message.isAckable()) {
			int clientMessageId = message.getMessageId();
			int brokerMessageId = nextIdToBroker();
			message.setMessageId(brokerMessageId);
			messageSourceByBrokerMessageId.put(brokerMessageId, new MessageSource(clientMessageId, channelToClient));
			if (messageSourceByBrokerMessageId.size() == maxInFlightBrokerMessages) {
				pauseRead();
			}
		}

		channelToBroker.send(message);
	}

	private void forwardToClient(IdentifiableMqttMessage message) {

		if (message.isAck()) {

			int brokerMessageId = message.getMessageId();
			MessageSource messageSource = messageSourceByBrokerMessageId.remove(brokerMessageId);
			if (messageSource != null) {
				message.setMessageId(messageSource.sourceMessageId);
				messageSource.sourceChannel.send(message);
				if (messageSourceByBrokerMessageId.size() == maxInFlightBrokerMessages - 1) {
					resumeRead();
				}
			}

		} else {
			MqttChannel channelToClient = getLeastBusyChannelToClient();
			if (channelToClient != null) {
				channelToClient.send(message);
			}
		}
	}

	private void pauseRead() {

		long now = System.currentTimeMillis();
		if (now > enablePauseMessageTime) {
			enablePauseMessageTime = now + 60000;
			Log.warn(
					"There are too many in-flight (unacknowledged) messages to the broker in cluster with client ID %s. No more message IDs are available. This means the broker is unable to keep up with the rate your clients are publishing messages. The proxy is pausing accepting messages from clients in the cluster until the broker acknowledges an existing in-flight message. This will not cause data loss but will make message publishing slow down. This log message will be disabled for 60 seconds.",
					clientId);
		}

		for (MqttChannel channel : channelsToClients) {
			channel.pauseRead();
		}
	}

	private void resumeRead() {

		for (MqttChannel channel : channelsToClients) {
			channel.resumeRead();
		}
	}

	private MqttChannel getLeastBusyChannelToClient() {

		int leastBusyMessageCount = Integer.MAX_VALUE;
		MqttChannel leastBusyChannel = null;

		int index = nextBrokerChannelIndex;
		int size = channelsToClients.size();
		for (int count = 0; count < size; count++) {
			int i = index++ % size;
			MqttChannel channelToClient = channelsToClients.get(i);
			int msgCount = channelToClient.inFlightMessageCount() + channelToClient.sendQueueDepth();
			if (msgCount < leastBusyMessageCount) {
				leastBusyMessageCount = msgCount;
				leastBusyChannel = channelToClient;
				nextBrokerChannelIndex = i + 1;
			}
		}

		return leastBusyChannel;
	}

	private int nextIdToBroker() {

		for (int i = 0; i < maxInFlightBrokerMessages; i++) {
			if (nextIdToBroker > maxInFlightBrokerMessages) {
				nextIdToBroker = 1;
			}
			int id = nextIdToBroker++;
			if (!messageSourceByBrokerMessageId.containsKey(id)) {
				return id;
			}
		}

		Log.error("Unable to generate message ID to broker. THIS IS A BUG!!");

		return 0;
	}

	private boolean stringEquqls(String string1, String string2) {

		if (string1 == string2) {
			return true;
		}

		if (string1 == null || string2 == null) {
			return false;
		}

		return string1.equals(string2);
	}

	private void distributeMessagesForChannel(MqttChannel channel) {

		List<MqttMessage> messages = channel.getUnsentMessages();
		for (MqttMessage message : messages) {
			MqttChannel newChannel = getLeastBusyChannelToClient();
			if (newChannel != null) {
				newChannel.send(message);
			}
		}
	}

	private static class MessageSource {

		private final int sourceMessageId;
		private final MqttChannel sourceChannel;

		public MessageSource(int sourceMessageId, MqttChannel sourceChannel) {
			this.sourceMessageId = sourceMessageId;
			this.sourceChannel = sourceChannel;
		}
	}
}
