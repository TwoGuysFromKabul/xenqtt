package net.sf.xenqtt.message;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import net.sf.xenqtt.Log;
import net.sf.xenqtt.MqttException;
import net.sf.xenqtt.MqttInterruptedException;

/**
 * Uses a single thread and non-blocking NIO to manage one or more {@link MqttChannel}s. You must call {@link #init()} before using this manager and
 * {@link #shutdown()} to shut it down.
 */
public final class ChannelManagerImpl implements ChannelManager {

	private final Set<MqttChannel> channels = new HashSet<MqttChannel>();
	private final long messageResendIntervalMillis;

	private final Lock commandsLock = new ReentrantLock();
	private Command<?> firstCommand;

	private final CountDownLatch readyLatch = new CountDownLatch(1);
	private final Thread ioThread;
	private final Selector selector;
	private final boolean blocking;

	/**
	 * @param messageResendIntervalSeconds
	 *            Seconds between attempts to resend a message that is {@link MqttMessage#isAckable()}. 0 to disable message resends
	 * @param blocking
	 *            If true then this channel manager operates in blocking mode. See the {@link ChannelManager} javadoc for explanations of how blocking mode
	 *            affects the various methods.
	 */
	public ChannelManagerImpl(long messageResendIntervalSeconds, boolean blocking) {
		this.blocking = blocking;
		this.messageResendIntervalMillis = messageResendIntervalSeconds * 1000;
		ioThread = new Thread(new Runnable() {

			@Override
			public void run() {
				doIO();
			}
		}, "MqttChannelManager");

		try {
			selector = Selector.open();
		} catch (IOException e) {
			throw new MqttException("Failed to open selector", e);
		}
	}

	/**
	 * @see net.sf.xenqtt.message.ChannelManager#init()
	 */
	@Override
	public void init() {

		ioThread.start();

		for (;;) {
			try {
				readyLatch.await();
				break;
			} catch (InterruptedException ignore) {
			}
		}
	}

	/**
	 * @see net.sf.xenqtt.message.ChannelManager#shutdown()
	 */
	@Override
	public void shutdown() {

		try {
			selector.close();
		} catch (IOException ignore) {
		}

		try {
			ioThread.join();
		} catch (InterruptedException e) {
			// restore the ioThread's interrupted status
			ioThread.interrupt();
		}
	}

	/**
	 * @see net.sf.xenqtt.message.ChannelManager#isRunning()
	 */
	@Override
	public boolean isRunning() {
		return ioThread.isAlive();
	}

	/**
	 * @see net.sf.xenqtt.message.ChannelManager#newClientChannel(java.lang.String, net.sf.xenqtt.message.MessageHandler)
	 */
	@Override
	public MqttChannelRef newClientChannel(String brokerUri, MessageHandler messageHandler) throws MqttInterruptedException {
		try {
			return newClientChannel(new URI(brokerUri), messageHandler);
		} catch (URISyntaxException e) {
			throw new MqttException("Failed to parse broker URI: " + brokerUri, e);
		}
	}

	/**
	 * @see net.sf.xenqtt.message.ChannelManager#newClientChannel(java.net.URI, net.sf.xenqtt.message.MessageHandler)
	 */
	@Override
	public MqttChannelRef newClientChannel(URI brokerUri, MessageHandler messageHandler) throws MqttInterruptedException {

		if (!"tcp".equals(brokerUri.getScheme())) {
			throw new MqttException("Invalid broker URI (scheme must be 'tcp'): " + brokerUri);
		}

		return newClientChannel(brokerUri.getHost(), brokerUri.getPort(), messageHandler);
	}

	/**
	 * @see net.sf.xenqtt.message.ChannelManager#newClientChannel(java.lang.String, int, net.sf.xenqtt.message.MessageHandler)
	 */
	@Override
	public MqttChannelRef newClientChannel(String host, int port, MessageHandler messageHandler) throws MqttInterruptedException {

		return addCommand(new NewClientChannelCommand(host, port, messageHandler)).await();
	}

	/**
	 * @see net.sf.xenqtt.message.ChannelManager#newBrokerChannel(java.nio.channels.SocketChannel, net.sf.xenqtt.message.MessageHandler)
	 */
	@Override
	public MqttChannelRef newBrokerChannel(SocketChannel socketChannel, MessageHandler messageHandler) throws MqttInterruptedException {

		return addCommand(new NewBrokerChannelCommand(socketChannel, messageHandler)).await();
	}

	/**
	 * @see net.sf.xenqtt.message.ChannelManager#send(net.sf.xenqtt.message.MqttChannelRef, net.sf.xenqtt.message.MqttMessage)
	 */
	@Override
	public boolean send(MqttChannelRef channel, MqttMessage message) throws MqttInterruptedException {

		return addCommand(new SendCommand(channel, message)).await();
	}

	/**
	 * @throws MqttInterruptedException
	 * @see net.sf.xenqtt.message.ChannelManager#sendToAll(net.sf.xenqtt.message.MqttMessage)
	 */
	@Override
	public void sendToAll(MqttMessage message) throws MqttInterruptedException {

		addCommand(new SendToAllCommand(message)).await();
	}

	/**
	 * @see net.sf.xenqtt.message.ChannelManager#close(net.sf.xenqtt.message.MqttChannelRef)
	 */
	@Override
	public void close(MqttChannelRef channel) throws MqttInterruptedException {

		addCommand(new CloseCommand(channel)).await();
	}

	private void closeAll() {

		Log.debug("Channel manager closing all channels");
		for (MqttChannel channel : channels) {
			try {
				channel.close();
			} catch (Exception ignore) {
			}
		}
	}

	private void doIO() {

		readyLatch.countDown();

		try {

			long maxIdleTime = Long.MAX_VALUE;

			for (;;) {

				if (maxIdleTime == Long.MAX_VALUE) {
					selector.select();
				} else {
					selector.select(maxIdleTime);
				}

				long now = System.currentTimeMillis();

				executeCommands(now);

				Set<SelectionKey> keys = selector.selectedKeys();
				doConnect(now, keys);
				doRead(now, keys);
				doWrite(now, keys);
				maxIdleTime = doHouseKeeping(now, selector.keys());

				keys.clear();
			}

		} catch (ClosedSelectorException e) {
			Log.info("Channel manager thread shutting down");
		} catch (Throwable t) {
			Log.fatal(t, "Channel manager thread caught a fatal exception and is dying");
		}

		closeAll();
	}

	private void doConnect(long now, Set<SelectionKey> keys) {

		Iterator<SelectionKey> iter = keys.iterator();
		while (iter.hasNext()) {
			SelectionKey key = iter.next();
			if (key.isConnectable()) {
				MqttChannel channel = (MqttChannel) key.attachment();
				if (!channel.finishConnect()) {
					iter.remove();
				}
			}
		}
	}

	private void doRead(long now, Set<SelectionKey> keys) {

		Iterator<SelectionKey> iter = keys.iterator();
		while (iter.hasNext()) {
			SelectionKey key = iter.next();
			if (key.isReadable()) {
				MqttChannel channel = (MqttChannel) key.attachment();
				if (!channel.read(now)) {
					channels.remove(channel);
					iter.remove();
				}
			}
		}
	}

	private void doWrite(long now, Set<SelectionKey> keys) {

		Iterator<SelectionKey> iter = keys.iterator();
		while (iter.hasNext()) {
			SelectionKey key = iter.next();
			if (key.isWritable()) {
				MqttChannel channel = (MqttChannel) key.attachment();
				if (!channel.write(now)) {
					channels.remove(channel);
					iter.remove();
				}
			}
		}
	}

	private long doHouseKeeping(long now, Set<SelectionKey> keys) {

		long maxIdleTime = Long.MAX_VALUE;

		for (SelectionKey key : keys) {
			MqttChannel channel = (MqttChannel) key.attachment();
			long time = channel.houseKeeping(now);
			if (time < 0) {
				channels.remove(channel);
			} else if (time < maxIdleTime) {
				maxIdleTime = time;
			}
		}

		return maxIdleTime;
	}

	private void executeCommands(long now) {

		commandsLock.lock();
		try {
			while (firstCommand != null) {
				firstCommand.execute();
				if (!blocking) {
					firstCommand.complete(null);
				}
				firstCommand = firstCommand.next;
			}
		} finally {
			commandsLock.unlock();
		}
	}

	private <T, C extends Command<T>> C addCommand(C command) {

		commandsLock.lock();
		try {
			command.next = firstCommand;
			firstCommand = command;
		} finally {
			commandsLock.unlock();
		}

		selector.wakeup();

		return command;
	}

	private abstract class Command<T> extends AbstractBlockingCommand<T> {

		private Command<?> next;
	}

	private final class SendCommand extends Command<Boolean> {

		private final MqttMessage message;
		private final MqttChannel channel;

		public SendCommand(MqttChannelRef channel, MqttMessage message) {
			this.message = message;
			this.channel = (MqttChannel) channel;
		}

		@Override
		public Boolean doExecute() {
			return channel.send(message, null);
		}
	}

	private final class SendToAllCommand extends Command<Void> {

		private final MqttMessage message;

		public SendToAllCommand(MqttMessage message) {
			this.message = message;
		}

		@Override
		public Void doExecute() {

			for (MqttChannel channel : channels) {
				try {
					MqttMessage msg = new MqttMessage(message);
					channel.send(msg, null);
				} catch (Exception e) {
					Log.error(e, "Failed to send message to %s: %s", channel, message);
				}
			}

			return null;
		}
	}

	private final class CloseCommand extends Command<Void> {

		private final MqttChannel channel;

		public CloseCommand(MqttChannelRef channel) {
			this.channel = (MqttChannel) channel;
		}

		@Override
		public Void doExecute() {

			channel.close();
			return null;
		}
	}

	private final class NewClientChannelCommand extends Command<MqttClientChannel> {

		private final String host;
		private final int port;
		private final MessageHandler messageHandler;
		private MqttClientChannel channel;

		public NewClientChannelCommand(String host, int port, MessageHandler messageHandler) {
			this.host = host;
			this.port = port;
			this.messageHandler = messageHandler;
		}

		@Override
		public MqttClientChannel doExecute() {
			try {
				channel = new MqttClientChannel(host, port, messageHandler, selector, messageResendIntervalMillis, null);
				channels.add(channel);
				return channel;
			} catch (Exception e) {
				throw new MqttException("MQTT Client channel creation failed", e);
			}
		}
	}

	private final class NewBrokerChannelCommand extends Command<MqttBrokerChannel> {

		private final SocketChannel socketChannel;
		private final MessageHandler messageHandler;

		public NewBrokerChannelCommand(SocketChannel socketChannel, MessageHandler messageHandler) {
			this.socketChannel = socketChannel;
			this.messageHandler = messageHandler;
		}

		@Override
		public MqttBrokerChannel doExecute() {
			try {
				MqttBrokerChannel channel = new MqttBrokerChannel(socketChannel, messageHandler, selector, messageResendIntervalMillis);
				channels.add(channel);
				return channel;
			} catch (Exception e) {
				try {
					socketChannel.close();
				} catch (IOException ignore) {
				}
				throw new MqttException("MQTT broker channel creation failed", e);
			}
		}
	}
}
