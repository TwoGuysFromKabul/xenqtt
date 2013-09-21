package net.sf.xenqtt.mockbroker;

import static net.sf.xenqtt.mockbroker.BrokerEventType.*;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.xenqtt.message.ConnAckMessage;
import net.sf.xenqtt.message.ConnectMessage;
import net.sf.xenqtt.message.ConnectReturnCode;
import net.sf.xenqtt.message.DisconnectMessage;
import net.sf.xenqtt.message.MessageHandler;
import net.sf.xenqtt.message.MqttChannel;
import net.sf.xenqtt.message.PubAckMessage;
import net.sf.xenqtt.message.PubCompMessage;
import net.sf.xenqtt.message.PubMessage;
import net.sf.xenqtt.message.PubRecMessage;
import net.sf.xenqtt.message.PubRelMessage;
import net.sf.xenqtt.message.QoS;
import net.sf.xenqtt.message.SubAckMessage;
import net.sf.xenqtt.message.SubscribeMessage;
import net.sf.xenqtt.message.UnsubAckMessage;
import net.sf.xenqtt.message.UnsubscribeMessage;

/**
 * Handles callbacks from {@link MqttChannel}s for the {@link MockBroker}
 */
final class BrokerMessageHandler implements MessageHandler {

	private static final Charset ASCII = Charset.forName("ASCII");

	private final Map<String, Topic> topicByName = new HashMap<String, Topic>();
	private final Map<String, Client> clientById = new HashMap<String, Client>();
	private final Map<MqttChannel, Client> clientByChannel = new IdentityHashMap<MqttChannel, Client>();

	private final MockBrokerHandler brokerHandler;
	private final BrokerEvents events;
	private final ConcurrentHashMap<String, String> credentials;
	private final boolean allowAnonymousAccess;

	BrokerMessageHandler(MockBrokerHandler brokerHandler, BrokerEvents events, ConcurrentHashMap<String, String> credentials, boolean allowAnonymousAccess) {
		this.credentials = credentials;
		this.allowAnonymousAccess = allowAnonymousAccess;
		this.brokerHandler = brokerHandler == null ? new MockBrokerHandler() : brokerHandler;
		this.events = events;
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#connect(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.ConnectMessage)
	 */
	@Override
	public void connect(MqttChannel channel, ConnectMessage message) throws Exception {

		Client client = clientByChannel.get(channel);
		client.clientId = message.getClientId();
		client.cleanSession = message.isCleanSession();

		clientById.put(client.clientId, client);
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
		} else if (password == null || !password.equals(credentials.get(user))) {
			returnCode = ConnectReturnCode.BAD_CREDENTIALS;
		}
		if (client.clientId.length() < 1 || client.clientId.length() > 23) {
			returnCode = ConnectReturnCode.IDENTIFIER_REJECTED;
		}
		if (message.getProtocolVersion() != 3) {
			returnCode = ConnectReturnCode.UNACCEPTABLE_PROTOCOL_VERSION;
		}

		if (message.isWillMessageFlag()) {
			Topic topic = getTopic(message.getWillTopic());
			String willMessage = message.getWillMessage();
			byte[] payload = willMessage == null ? null : willMessage.getBytes(ASCII);
			PubMessage msg = new PubMessage(message.getWillQoS(), message.isWillRetain(), message.getWillTopic(), 0, payload);
			topic.publish(msg, clientById);
		}

		client.send(new ConnAckMessage(returnCode));
		for (Topic topic : topicByName.values()) {
			topic.connected(client);
		}
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#connAck(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.ConnAckMessage)
	 */
	@Override
	public void connAck(MqttChannel channel, ConnAckMessage message) throws Exception {
		Client client = getClient(channel);
		client.messageReceived(message);
		brokerHandler.unexpectedMessage(client, message);
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#publish(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.PubMessage)
	 */
	@Override
	public void publish(MqttChannel channel, PubMessage message) throws Exception {

		Client client = getClient(channel);
		client.messageReceived(message);

		if (brokerHandler.publish(client, message)) {
			return;
		}

		Topic topic = getTopic(message.getTopicName());
		topic.publish(message, clientById);

		if (message.getQoSLevel() > 0) {
			client.send(new PubAckMessage(message.getMessageId()));
		}
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#pubAck(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.PubAckMessage)
	 */
	@Override
	public void pubAck(MqttChannel channel, PubAckMessage message) throws Exception {

		Client client = getClient(channel);
		client.messageReceived(message);

		if (brokerHandler.pubAck(client, message)) {
			return;
		}

		int messageId = message.getMessageId();
		for (Topic topic : topicByName.values()) {
			if (topic.pubAcked(client, messageId)) {
				return;
			}
		}
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#pubRec(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.PubRecMessage)
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
	 * @see net.sf.xenqtt.message.MessageHandler#pubRel(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.PubRelMessage)
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
	 * @see net.sf.xenqtt.message.MessageHandler#pubComp(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.PubCompMessage)
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
	 * @see net.sf.xenqtt.message.MessageHandler#subscribe(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.SubscribeMessage)
	 */
	@Override
	public void subscribe(MqttChannel channel, SubscribeMessage message) throws Exception {

		Client client = clientByChannel.get(channel);

		client.messageReceived(message);

		if (brokerHandler.subscribe(client, message)) {
			return;
		}

		String[] topics = message.getTopics();
		QoS[] requestedQoses = message.getRequestedQoSes();
		QoS[] grantedQoses = new QoS[requestedQoses.length];

		for (int i = 0; i < topics.length; i++) {
			Topic topic = getTopic(topics[i]);
			Subscription subscription = getSubscription(client.clientId, topics[i]);
			grantedQoses[i] = topic.subscribe(requestedQoses[i], client, subscription);
		}

		client.send(new SubAckMessage(message.getMessageId(), grantedQoses));
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#subAck(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.SubAckMessage)
	 */
	@Override
	public void subAck(MqttChannel channel, SubAckMessage message) throws Exception {

		Client client = getClient(channel);
		client.messageReceived(message);

		brokerHandler.unexpectedMessage(client, message);
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#unsubscribe(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.UnsubscribeMessage)
	 */
	@Override
	public void unsubscribe(MqttChannel channel, UnsubscribeMessage message) throws Exception {

		Client client = getClient(channel);
		client.messageReceived(message);

		if (brokerHandler.unsubscribe(client, message)) {
			return;
		}

		for (String topicName : message.getTopics()) {
			Topic topic = topicByName.get(topicName);
			if (topic != null) {
				topic.unsubscribe(client);
			}
		}

		client.send(new UnsubAckMessage(message.getMessageId()));
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#unsubAck(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.UnsubAckMessage)
	 */
	@Override
	public void unsubAck(MqttChannel channel, UnsubAckMessage message) throws Exception {

		Client client = getClient(channel);
		client.messageReceived(message);

		brokerHandler.unexpectedMessage(client, message);
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#disconnect(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.DisconnectMessage)
	 */
	@Override
	public void disconnect(MqttChannel channel, DisconnectMessage message) throws Exception {

		Client client = getClient(channel);
		client.messageReceived(message);

		brokerHandler.disconnect(client, message);
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#channelOpened(net.sf.xenqtt.message.MqttChannel)
	 */
	@Override
	public void channelOpened(MqttChannel channel) {

		Client client = new Client(channel, events);
		clientByChannel.put(channel, client);
		events.addEvent(CHANNEL_OPENED, client);

		brokerHandler.channelOpened(client);
	}

	/**
	 * @see net.sf.xenqtt.message.MessageHandler#channelClosed(net.sf.xenqtt.message.MqttChannel, java.lang.Throwable)
	 */
	@Override
	public void channelClosed(MqttChannel channel, Throwable cause) {

		Client client = clientByChannel.remove(channel);
		events.addEvent(CHANNEL_CLOSED, client);
		clientById.remove(client.clientId);

		if (client.cleanSession) {
			for (Topic topic : topicByName.values()) {
				topic.unsubscribe(client);
			}
		}

		brokerHandler.channelClosed(client, cause);
	}

	private Client getClient(MqttChannel channel) {
		return clientByChannel.get(channel);
	}

	private Topic getTopic(String topicName) {

		Topic topic = topicByName.get(topicName);
		if (topic == null) {
			topic = new Topic(topicName);
			topicByName.put(topicName, topic);
		}

		return topic;
	}

	/**
	 * If there is a subscription existing in another topic that matches the specified topic (including wildcard matching) it is returned. Otherwise a new
	 * subscription is created.
	 */
	private Subscription getSubscription(String clientId, String topicName) {

		Subscription subscription = null;

		if (topicName.charAt(0) == '/') {
			topicName += "r";
		}
		// FIXME [jim] - need to validate topic names - no + or # expect as appropriate wildcards, no //, etc
		// FIXME [jim] - add a quicksplit method?
		String[] topicLevels = topicName.split("/");
		for (Topic topic : topicByName.values()) {
			if (topic.nameMatches(topicLevels)) {
				subscription = topic.getSubscription(clientId);
				break;
			}
		}

		return subscription != null ? subscription : new Subscription(clientId);
	}
}
