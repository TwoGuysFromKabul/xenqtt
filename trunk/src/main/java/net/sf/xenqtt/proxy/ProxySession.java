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

import java.util.HashSet;
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

	private final Map<MqttChannel, ConnectMessage> connectMessageByChannelPendingAttach = new ConcurrentHashMap<MqttChannel, ConnectMessage>();
	private final Set<MqttChannel> channelsPendingBroker = new HashSet<MqttChannel>();

	private final ChannelManager channelManager = new ChannelManagerImpl(0);
	private final String clientId;
	private final ConnectMessage originalConnectMessage;

	private ConnAckMessage originalConnAckMessage;

	private MqttChannel clientChannel;
	private ConnectionState brokerConnectionState = ConnectionState.PENDING;

	public ProxySession(ConnectMessage connectMessage) {

		this.originalConnectMessage = connectMessage;
		this.clientId = originalConnectMessage.getClientId();
	}

	/**
	 * FIXME [jim] - needs javadoc
	 */
	public void init() {
		channelManager.init();
	}

	/**
	 * FIXME [jim] - needs javadoc
	 */
	public void shutdown() {
		channelManager.shutdown();
	}

	/**
	 * Called when a new client connection is received by the {@link ServerMessageHandler}
	 */
	public void newConnection(MqttChannel channel, ConnectMessage connectMessage) {

		connectMessageByChannelPendingAttach.put(channel, connectMessage);
		channelManager.attachChannel(channel, this);
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#connect(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.ConnectMessage)
	 */
	@Override
	public void connect(MqttChannel channel, ConnectMessage message) throws Exception {
		// TODO Auto-generated method stub

	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#connAck(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.ConnAckMessage)
	 */
	@Override
	public void connAck(MqttChannel channel, ConnAckMessage message) throws Exception {
		// TODO Auto-generated method stub

		if (channel != clientChannel) {
			Log.warn("Received a %s message from a client. This should never happen. clientId=%s", message.getMessageType(), clientId);
			return;
		}

		originalConnAckMessage = message;

		// FIXME [jim] - copy and send the original connect messsage
		// for(MqttChannel brokerChannel : channelsPendingBroker) {
		// brokerChannel.send(message, blockingCommand, now)
		// }
		channelsPendingBroker.clear();
		if (message.getReturnCode() == ConnectReturnCode.ACCEPTED) {
			brokerConnectionState = ConnectionState.CONNECTED;

		} else {
			brokerConnectionState = ConnectionState.DISCONNECTED;
		}

	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#publish(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.PubMessage)
	 */
	@Override
	public void publish(MqttChannel channel, PubMessage message) throws Exception {
		// TODO Auto-generated method stub

	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#pubAck(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.PubAckMessage)
	 */
	@Override
	public void pubAck(MqttChannel channel, PubAckMessage message) throws Exception {
		// TODO Auto-generated method stub

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
		// TODO Auto-generated method stub

	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#subAck(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.SubAckMessage)
	 */
	@Override
	public void subAck(MqttChannel channel, SubAckMessage message) throws Exception {
		// TODO Auto-generated method stub

	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#unsubscribe(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.UnsubscribeMessage)
	 */
	@Override
	public void unsubscribe(MqttChannel channel, UnsubscribeMessage message) throws Exception {
		// TODO Auto-generated method stub

	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#unsubAck(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.UnsubAckMessage)
	 */
	@Override
	public void unsubAck(MqttChannel channel, UnsubAckMessage message) throws Exception {
		// TODO Auto-generated method stub

	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#disconnect(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.DisconnectMessage)
	 */
	@Override
	public void disconnect(MqttChannel channel, DisconnectMessage message) throws Exception {
		// TODO Auto-generated method stub

	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#channelOpened(net.sf.xenqtt.message.MqttChannel)
	 */
	@Override
	public void channelOpened(MqttChannel channel) {

		this.clientChannel = channel;
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#channelClosed(net.sf.xenqtt.message.MqttChannel, java.lang.Throwable)
	 */
	@Override
	public void channelClosed(MqttChannel channel, Throwable cause) {
		// TODO Auto-generated method stub

	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#channelAttached(net.sf.xenqtt.message.MqttChannel)
	 */
	@Override
	public void channelAttached(MqttChannel channel) {

		if (brokerConnectionState == ConnectionState.DISCONNECTED) {
			Log.warn("Attempting to connect a clustered client to a session with a closed broker connection. clientId: %s, channel: %s", clientId, channel);
			channelManager.send(channel, new ConnAckMessage(ConnectReturnCode.OTHER));
			return;
		}

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

		Log.info("New client connection accepted into cluster with client ID: %s", clientId);
		channelManager.send(channel, new ConnAckMessage(ConnectReturnCode.ACCEPTED));
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
}
