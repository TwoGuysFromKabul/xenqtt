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
package net.sf.xenqtt.proxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.xenqtt.Log;
import net.sf.xenqtt.message.ChannelManager;
import net.sf.xenqtt.message.ChannelManagerImpl;
import net.sf.xenqtt.message.ConnAckMessage;
import net.sf.xenqtt.message.ConnectMessage;
import net.sf.xenqtt.message.ConnectReturnCode;
import net.sf.xenqtt.message.DisconnectMessage;
import net.sf.xenqtt.message.IdentifiableMqttMessage;
import net.sf.xenqtt.message.MessageHandler;
import net.sf.xenqtt.message.MqttChannel;
import net.sf.xenqtt.message.MqttMessage;
import net.sf.xenqtt.message.PubAckMessage;
import net.sf.xenqtt.message.PubCompMessage;
import net.sf.xenqtt.message.PubMessage;
import net.sf.xenqtt.message.PubRecMessage;
import net.sf.xenqtt.message.PubRelMessage;
import net.sf.xenqtt.message.SubAckMessage;
import net.sf.xenqtt.message.SubscribeMessage;
import net.sf.xenqtt.message.UnsubAckMessage;
import net.sf.xenqtt.message.UnsubscribeMessage;

/**
 * Each instance of this class controls one proxy session. A session consists of one connection to a broker and connections to all the clients in a cluster.
 */
final class ProxySession implements MessageHandler {

	private enum ConnectionState {
		PENDING, CONNECTED, DISCONNECTED
	}

	private final Map<Integer, MessageDest> messageDestByBrokerMessageId = new HashMap<Integer, MessageDest>();
	private final Map<Integer, MessageSource> messageSourceByBrokerMessageId = new HashMap<Integer, MessageSource>();
	private final Map<MqttChannel, ConnectMessage> connectMessageByChannelPendingAttach = new ConcurrentHashMap<MqttChannel, ConnectMessage>();
	private final Set<MqttChannel> channelsPendingBroker = new HashSet<MqttChannel>();

	private final ChannelManager channelManager = new ChannelManagerImpl(0);
	private final String brokerUri;
	private final String clientId;
	private final ConnectMessage originalConnectMessage;

	private final List<MqttChannel> brokerChannels = new ArrayList<MqttChannel>();
	private MqttChannel clientChannel;
	private ConnectReturnCode brokerConnectReturnCode;

	private ConnectionState brokerConnectionState = ConnectionState.PENDING;

	private int nextIdToBroker = 1;
	private int nextBrokerChannelIndex;

	private volatile boolean sessionClosed;

	public ProxySession(String brokerUri, ConnectMessage connectMessage) {

		this.brokerUri = brokerUri;
		this.originalConnectMessage = connectMessage;
		this.clientId = originalConnectMessage.getClientId();
	}

	/**
	 * Initializes this session
	 */
	public void init() {
		channelManager.init();
		clientChannel = (MqttChannel) channelManager.newClientChannel(brokerUri, this);
	}

	/**
	 * Shuts down this session
	 */
	public void shutdown() {
		channelManager.shutdown();
	}

	/**
	 * @return True if the session is closed. False otherwise. A session is closed when the connection to the broker is closed. It has nothing to do with
	 *         {@link #shutdown()}.
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
	 * @see net.sf.xenqtt.message.MessageHandler#connect(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.ConnectMessage)
	 */
	@Override
	public void connect(MqttChannel channel, ConnectMessage message) throws Exception {

		if (channel == clientChannel) {
			Log.warn("Received a %s message from the broker at %s. This should never happen. clientId=%s", message.getMessageType(),
					channel.getRemoteAddress(), clientId);
		} else {
			Log.warn("Received a %s message from clustered client %s. This should never happen. clientId=%s", message.getMessageType(),
					channel.getRemoteAddress(), clientId);
		}
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#connAck(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.ConnAckMessage)
	 */
	@Override
	public void connAck(MqttChannel channel, ConnAckMessage message) throws Exception {

		if (channel != clientChannel) {
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

		for (MqttChannel brokerChannel : channelsPendingBroker) {
			newSessionClient(brokerChannel);
		}
		channelsPendingBroker.clear();
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#publish(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.PubMessage)
	 */
	@Override
	public void publish(MqttChannel channel, PubMessage message) throws Exception {
		forwardMessage(channel, message);
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#pubAck(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.PubAckMessage)
	 */
	@Override
	public void pubAck(MqttChannel channel, PubAckMessage message) throws Exception {
		forwardMessage(channel, message);
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#pubRec(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.PubRecMessage)
	 */
	@Override
	public void pubRec(MqttChannel channel, PubRecMessage message) throws Exception {
		// TODO qos 2 not supported
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#pubRel(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.PubRelMessage)
	 */
	@Override
	public void pubRel(MqttChannel channel, PubRelMessage message) throws Exception {
		// TODO qos 2 not supported
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#pubComp(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.PubCompMessage)
	 */
	@Override
	public void pubComp(MqttChannel channel, PubCompMessage message) throws Exception {
		// TODO qos 2 not supported
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#subscribe(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.SubscribeMessage)
	 */
	@Override
	public void subscribe(MqttChannel channel, SubscribeMessage message) throws Exception {
		forwardMessage(channel, message);
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#subAck(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.SubAckMessage)
	 */
	@Override
	public void subAck(MqttChannel channel, SubAckMessage message) throws Exception {
		forwardMessage(channel, message);
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#unsubscribe(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.UnsubscribeMessage)
	 */
	@Override
	public void unsubscribe(MqttChannel channel, UnsubscribeMessage message) throws Exception {
		forwardMessage(channel, message);
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#unsubAck(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.UnsubAckMessage)
	 */
	@Override
	public void unsubAck(MqttChannel channel, UnsubAckMessage message) throws Exception {
		forwardMessage(channel, message);
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#disconnect(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.DisconnectMessage)
	 */
	@Override
	public void disconnect(MqttChannel channel, DisconnectMessage message) throws Exception {
		// ignore, disconnected clients are handled in channelClosed(...)
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#channelOpened(net.sf.xenqtt.message.MqttChannel)
	 */
	@Override
	public void channelOpened(MqttChannel channel) {
		// ignore
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#channelClosed(net.sf.xenqtt.message.MqttChannel, java.lang.Throwable)
	 */
	@Override
	public void channelClosed(MqttChannel channel, Throwable cause) {

		if (channel == clientChannel) {
			brokerConnectionState = ConnectionState.DISCONNECTED;

			for (MqttChannel brokerChannel : brokerChannels) {
				brokerChannel.close();
			}
			brokerChannels.clear();

			sessionClosed = true;

		} else if (brokerConnectionState != ConnectionState.DISCONNECTED) {
			brokerChannels.remove(channel);

			if (brokerChannels.isEmpty()) {
				clientChannel.send(new DisconnectMessage());
			} else {
				distributeMessagesForChannel(channel);
			}
		}
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#channelAttached(net.sf.xenqtt.message.MqttChannel)
	 */
	@Override
	public void channelAttached(MqttChannel channel) {

		ConnectMessage connectMessage = connectMessageByChannelPendingAttach.remove(channel);

		if (connectMessage == null) {
			Log.warn("Channel attached with no connect message. This is most likely a bug. clientId: %s, channel: %s", clientId, channel);
			channelManager.send(channel, new ConnAckMessage(ConnectReturnCode.OTHER));
			return;
		}

		if (connectMessage.isCleanSession()) {
			Log.warn("Proxied connections cannot have the clean session flag set in the connect message; clientId: %s", clientId);
			channelManager.send(channel, new ConnAckMessage(ConnectReturnCode.OTHER));
			return;
		}

		if (originalConnectMessage.getProtocolVersion() != connectMessage.getProtocolVersion()) {
			Log.warn("Connect message protocol version does not match; clientId: %s, expected: %d, actual: %d", clientId,
					originalConnectMessage.getProtocolVersion(), connectMessage.getProtocolVersion());
			channelManager.send(channel, new ConnAckMessage(ConnectReturnCode.UNACCEPTABLE_PROTOCOL_VERSION));
			return;
		}

		if (!stringEquqls(originalConnectMessage.getProtocolName(), connectMessage.getProtocolName())) {
			Log.warn("Connect message protocol name does not match; clientId: %s, expected: %d, actual: %d", clientId,
					originalConnectMessage.getProtocolName(), connectMessage.getProtocolName());
			channelManager.send(channel, new ConnAckMessage(ConnectReturnCode.OTHER));
			return;
		}

		if (originalConnectMessage.isUserNameFlag() != connectMessage.isUserNameFlag()
				|| originalConnectMessage.isPasswordFlag() != connectMessage.isPasswordFlag()
				|| !stringEquqls(originalConnectMessage.getUserName(), connectMessage.getUserName())
				|| !stringEquqls(originalConnectMessage.getPassword(), connectMessage.getPassword())) {
			Log.warn("Connect message username/password does not match; clientId: %s, expected: %s/%s, actual: %s/%s", clientId,
					originalConnectMessage.getUserName(), originalConnectMessage.getPassword(), connectMessage.getUserName(), connectMessage.getPassword());
			channelManager.send(channel, new ConnAckMessage(ConnectReturnCode.BAD_CREDENTIALS));
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
			channelManager.send(channel, new ConnAckMessage(ConnectReturnCode.OTHER));
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
	 * @see net.sf.xenqtt.message.MessageHandler#channelDetached(net.sf.xenqtt.message.MqttChannel)
	 */
	@Override
	public void channelDetached(MqttChannel channel) {
		// this should never happen
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#messageSent(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.MqttMessage)
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
			channelManager.send(channel, new ConnAckMessage(returnCode));
		} else {
			Log.info("New client connection accepted into cluster; clientId: %s, address: %s", clientId, channel.getRemoteAddress());
			channelManager.send(channel, new ConnAckMessage(brokerConnectReturnCode));
			brokerChannels.add(channel);
		}
	}

	private void forwardMessage(MqttChannel channel, IdentifiableMqttMessage message) {

		if (channel == clientChannel) {
			forwardToClient(message);
		} else {
			forwardToBroker(channel, message);
		}
	}

	private void forwardToBroker(MqttChannel clientChannel, IdentifiableMqttMessage message) {

		if (message.isAckable()) {
			int clientMessageId = message.getMessageId();
			int brokerMessageId = nextIdToBroker();
			message.setMessageId(brokerMessageId);
			messageSourceByBrokerMessageId.put(brokerMessageId, new MessageSource(clientMessageId, clientChannel));
		}

		if (message.isAck()) {
			messageDestByBrokerMessageId.remove(message.getMessageId());
		}

		clientChannel.send(message);
	}

	private void forwardToClient(IdentifiableMqttMessage message) {

		if (message.isAck()) {

			int brokerMessageId = message.getMessageId();
			MessageSource messageSource = messageSourceByBrokerMessageId.remove(brokerMessageId);
			if (messageSource != null) {
				message.setMessageId(messageSource.sourceMessageId);
				messageSource.sourceChannel.send(message);
			}

		} else {
			MqttChannel channel = getLeastBusyChannel();
			if (channel != null) {
				if (message.isAckable()) {
					messageDestByBrokerMessageId.put(message.getMessageId(), new MessageDest(channel, message));
				}
				channel.send(message);
			}
		}
	}

	private MqttChannel getLeastBusyChannel() {

		int leastBusyMessageCount = Integer.MAX_VALUE;
		MqttChannel leastBusyChannel = null;

		int index = nextBrokerChannelIndex;
		int size = brokerChannels.size();
		for (int count = 0; count < size; count++) {
			int i = index++ % size;
			MqttChannel brokerChannel = brokerChannels.get(i);
			int msgCount = brokerChannel.inFlightMessageCount() + brokerChannel.sendQueueDepth();
			if (msgCount < leastBusyMessageCount) {
				leastBusyMessageCount = msgCount;
				leastBusyChannel = brokerChannel;
				nextBrokerChannelIndex = i + 1;
			}
		}

		return leastBusyChannel;
	}

	private int nextIdToBroker() {

		// TODO [jim] - need to deal with what happens when we have an in-flight message that is already using a message ID that we come around to again. Is
		// this even realistic?
		if (nextIdToBroker > 0xffff) {
			nextIdToBroker = 1;
		}

		return nextIdToBroker++;
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

		for (MessageDest dest : messageDestByBrokerMessageId.values()) {

			if (dest.channel == channel) {
				MqttChannel newChannel = getLeastBusyChannel();
				if (newChannel != null) {
					dest.channel = newChannel;
					newChannel.send(dest.message);
				}
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

	private static class MessageDest {

		private final MqttMessage message;
		private MqttChannel channel;

		public MessageDest(MqttChannel channel, MqttMessage message) {
			this.channel = channel;
			this.message = message;
		}
	}
}