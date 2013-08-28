package net.sf.xenqtt.proxy;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import net.sf.xenqtt.message.ConnAckMessage;
import net.sf.xenqtt.message.ConnectMessage;
import net.sf.xenqtt.message.ConnectReturnCode;
import net.sf.xenqtt.message.MessageHandler;
import net.sf.xenqtt.message.MqttChannel;
import net.sf.xenqtt.message.MqttChannelImpl;

/**
 * The connections to the broker and all the clients for one client ID is a gateway session. This class may be accessed by at most 2 threads: an external thread
 * that interacts with the {@link GatewaySession} API (the API thread) and the internal thread that handles messaging for the session (the session thread).
 */
class GatewaySessionImpl extends Thread implements GatewaySession {

	private final Map<Integer, ChannelAndId> clientChannelsByMessageId = new HashMap<Integer, ChannelAndId>();

	// commands will be set to null when the session is closing/closed
	private final Lock commandLock = new ReentrantLock();
	private Queue<Command> commands = new ArrayDeque<Command>();

	private final List<MqttChannel> clientChannels = new ArrayList<MqttChannel>();
	private final Selector selector = Selector.open();

	private final ConnectMessage firstConnectMessage;

	private final BrokerMessageHandler brokerHandler;
	private final MessageHandler clientHandler;
	private final MqttChannel brokerChannel;

	/**
	 * @param brokerUri
	 *            The URI of the broker to proxy clients to
	 * @param channel
	 *            The channel for the first client connection in the session
	 * @param message
	 *            The {@link ConnectMessage} received from the channel
	 * @throws IOException
	 */
	public GatewaySessionImpl(String brokerHost, int brokerPort, MqttChannel channel, ConnectMessage message) throws IOException {
		super("GatewaySession-" + message.getClientId());

		this.firstConnectMessage = message;
		this.brokerHandler = createBrokerMessageHandler(clientChannels, clientChannelsByMessageId);
		this.brokerChannel = new MqttChannelImpl(brokerHost, brokerPort, brokerHandler, selector);
		this.clientHandler = createClientMessageHandler(clientChannelsByMessageId, brokerChannel);

		brokerChannel.send(firstConnectMessage);
	}

	/**
	 * @see net.sf.xenqtt.proxy.GatewaySession#addClient(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.ConnectMessage)
	 */
	@Override
	public final boolean addClient(MqttChannel channel, ConnectMessage connectMessage) {

		return doCommand(new AddClientCommand(channel, connectMessage));
	}

	/**
	 * @see net.sf.xenqtt.proxy.GatewaySession#isOpen()
	 */
	@Override
	public boolean isOpen() {

		commandLock.lock();
		try {
			return commands != null;
		} finally {
			commandLock.unlock();
		}
	}

	/**
	 * @see net.sf.xenqtt.proxy.GatewaySession#close()
	 */
	@Override
	public final void close() {

		doCommand(new CloseCommand());

		for (;;) {
			try {
				join();
				return;
			} catch (InterruptedException ignore) {
			}
		}
	}

	/**
	 * Overridden by unit tests to inject a mock broker message handler
	 */
	BrokerMessageHandler createBrokerMessageHandler(List<MqttChannel> clientChannels, Map<Integer, ChannelAndId> clientChannelsByMessageId) {
		return new BrokerMessageHandlerImpl(clientChannels, clientChannelsByMessageId);
	}

	/**
	 * Overridden by unit tests to inject mock client message handlers
	 */
	MessageHandler createClientMessageHandler(Map<Integer, ChannelAndId> clientChannelsByMessageId, MqttChannel broker) {
		return new ClientMessageHandler(clientChannelsByMessageId, broker);
	}

	/**
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {

		while (brokerChannel.isOpen()) {

			try {
				selector.select();
				Set<SelectionKey> keys = selector.selectedKeys();
				for (SelectionKey key : keys) {
					read(key);
				}

				for (SelectionKey key : keys) {
					write(key);
				}
				keys.clear();
			} catch (Exception e) {
				e.printStackTrace();
				// FIXME [jim] - log or something
				break;
			}
		}

		commandLock.lock();
		try {
			commands = null;
		} finally {
			commandLock.unlock();
		}

		brokerChannel.close();
		for (MqttChannel channel : clientChannels) {
			channel.close();
		}

		clientChannels.clear();
		clientChannelsByMessageId.clear();
		try {
			selector.close();
		} catch (Exception ignore) {
		}
	}

	private void read(SelectionKey key) {

		if (!key.isValid() || !key.isReadable()) {
			return;
		}

		MqttChannel channel = (MqttChannel) key.attachment();

		try {
			if (!channel.read()) {
				brokerHandler.closeClientChannel(channel);
			}
		} catch (Exception e) {
			e.printStackTrace();
			// FIXME [jim] - log or something
			brokerHandler.closeClientChannel(channel);
		}
	}

	private void write(SelectionKey key) {

		if (!key.isValid() || !key.isWritable()) {
			return;
		}

		MqttChannel channel = (MqttChannel) key.attachment();

		try {
			channel.write();
		} catch (Exception e) {
			e.printStackTrace();
			// FIXME [jim] - log or something
			brokerHandler.closeClientChannel(channel);
		}

	}

	private void addNewClient(MqttChannel channel, ConnectMessage connectMessage) {

		try {
			if (!connectMessage.equals(firstConnectMessage)) {
				channel.send(new ConnAckMessage(ConnectReturnCode.OTHER));
				return;
			}

			channel.register(selector, clientHandler);
			clientChannels.add(channel);
			brokerHandler.newClientChannel(channel);

		} catch (Exception e) {
			e.printStackTrace();
			// FIXME [jim] - log or something
		}
	}

	private boolean doCommand(Command command) {

		commandLock.lock();
		try {
			if (commands != null) {
				commands.add(command);
				selector.wakeup();
				return true;
			}
		} finally {
			commandLock.unlock();
		}

		return false;
	}

	private interface Command {
		/**
		 * @return True for the session to continue, false to
		 */
		void run() throws Exception;
	}

	private final class AddClientCommand implements Command {

		private final MqttChannel channel;
		private final ConnectMessage connectMessage;

		public AddClientCommand(MqttChannel channel, ConnectMessage connectMessage) {
			this.channel = channel;
			this.connectMessage = connectMessage;
		}

		@Override
		public void run() throws Exception {
			addNewClient(channel, connectMessage);
		}
	}

	private final class CloseCommand implements Command {

		@Override
		public void run() throws Exception {

			brokerChannel.close();
		}
	}
}
