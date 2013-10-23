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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.sf.xenqtt.SimpleBroker;
import net.sf.xenqtt.message.ConnAckMessage;
import net.sf.xenqtt.message.ConnectMessage;
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
 * {@link SimpleBroker} extension that handles creating/managing {@link ProxySession sessions}.
 */
public class ProxyBroker extends SimpleBroker implements MessageHandler {

	private final String brokerUri;

	private final Map<String, ProxySession> proxySessionByClientId = new HashMap<String, ProxySession>();

	private final int maxInFlightBrokerMessages;

	/**
	 * @param brokerUri
	 *            The URI of the broker the proxy should connect to
	 * @param port
	 *            The port for the server to listen on. 0 will choose an arbitrary available port which you can get from {@link #getPort()} after calling
	 *            {@link #init()}.
	 * @param maxInFlightBrokerMessages
	 *            Maximum number of messages that may be in-flight to the broker at a time
	 */
	public ProxyBroker(String brokerUri, int port, int maxInFlightBrokerMessages) {
		super(0, port);
		this.brokerUri = brokerUri;
		this.maxInFlightBrokerMessages = maxInFlightBrokerMessages;
	}

	/**
	 * Initializes the broker
	 */
	public void init() {

		super.init(this, "ProxyServer");
	}

	/**
	 * @see net.sf.xenqtt.SimpleBroker#shutdown(long)
	 */
	@Override
	public boolean shutdown(long millis) {

		for (ProxySession session : proxySessionByClientId.values()) {
			session.shutdown();
		}

		return super.shutdown(millis);
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#connect(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.ConnectMessage)
	 */
	@Override
	public void connect(MqttChannel channel, ConnectMessage message) throws Exception {

		String clientId = message.getClientId();
		ProxySession session = proxySessionByClientId.get(clientId);
		if (session == null) {
			session = newProxySession(brokerUri, message, maxInFlightBrokerMessages);
			session.init();
			proxySessionByClientId.put(clientId, session);
		}

		manager.detachChannel(channel);

		if (!session.newConnection(channel, message)) {
			proxySessionByClientId.remove(clientId);
			session.shutdown();
			connect(channel, message);
			return;
		}

		shutdownClosedSessions();
	}

	/**
	 * Unit tests override to inject mock {@link ProxySession sessions}
	 * 
	 * @return A new {@link ProxySession} instance
	 */
	ProxySession newProxySession(String brokerUri, ConnectMessage message, int maxInFlightBrokerMessages) {
		return new ProxySession(brokerUri, message, maxInFlightBrokerMessages);
	}

	private void shutdownClosedSessions() {

		Iterator<ProxySession> iter = proxySessionByClientId.values().iterator();
		while (iter.hasNext()) {
			ProxySession session = iter.next();
			if (session.isClosed()) {
				session.shutdown();
				iter.remove();
			}
		}
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#connAck(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.ConnAckMessage)
	 */
	@Override
	public void connAck(MqttChannel channel, ConnAckMessage message) throws Exception {
		// Should never happen
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#publish(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.PubMessage)
	 */
	@Override
	public void publish(MqttChannel channel, PubMessage message) throws Exception {
		// Should never happen
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#pubAck(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.PubAckMessage)
	 */
	@Override
	public void pubAck(MqttChannel channel, PubAckMessage message) throws Exception {
		// Should never happen
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#pubRec(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.PubRecMessage)
	 */
	@Override
	public void pubRec(MqttChannel channel, PubRecMessage message) throws Exception {
		// Should never happen
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#pubRel(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.PubRelMessage)
	 */
	@Override
	public void pubRel(MqttChannel channel, PubRelMessage message) throws Exception {
		// Should never happen
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#pubComp(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.PubCompMessage)
	 */
	@Override
	public void pubComp(MqttChannel channel, PubCompMessage message) throws Exception {
		// Should never happen
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#subscribe(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.SubscribeMessage)
	 */
	@Override
	public void subscribe(MqttChannel channel, SubscribeMessage message) throws Exception {
		// Should never happen
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#subAck(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.SubAckMessage)
	 */
	@Override
	public void subAck(MqttChannel channel, SubAckMessage message) throws Exception {
		// Should never happen
	}

	@Override
	public void unsubscribe(MqttChannel channel, UnsubscribeMessage message) throws Exception {
		// Should never happen
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#unsubAck(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.UnsubAckMessage)
	 */
	@Override
	public void unsubAck(MqttChannel channel, UnsubAckMessage message) throws Exception {
		// Should never happen
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#disconnect(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.DisconnectMessage)
	 */
	@Override
	public void disconnect(MqttChannel channel, DisconnectMessage message) throws Exception {
		// Should never happen
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
		// ignore
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#channelAttached(net.sf.xenqtt.message.MqttChannel)
	 */
	@Override
	public void channelAttached(MqttChannel channel) {
		// this should never happen
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#channelDetached(net.sf.xenqtt.message.MqttChannel)
	 */
	@Override
	public void channelDetached(MqttChannel channel) {
		// ignore
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#messageSent(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.MqttMessage)
	 */
	@Override
	public void messageSent(MqttChannel channel, MqttMessage message) {
		// ignore
	}
}
