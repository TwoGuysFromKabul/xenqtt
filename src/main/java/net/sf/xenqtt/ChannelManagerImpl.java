package net.sf.xenqtt;

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

import net.sf.xenqtt.message.MessageHandler;
import net.sf.xenqtt.message.MqttBrokerChannel;
import net.sf.xenqtt.message.MqttChannel;
import net.sf.xenqtt.message.MqttChannelRef;
import net.sf.xenqtt.message.MqttClientChannel;
import net.sf.xenqtt.message.MqttMessage;

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

	/**
	 * @param messageResendIntervalSeconds
	 *            Seconds between attempts to resend a message that is {@link MqttMessage#isAckable()}. 0 to disable message resends
	 */
	public ChannelManagerImpl(long messageResendIntervalSeconds) {
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
	 * @see net.sf.xenqtt.ChannelManager#init()
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
	 * @see net.sf.xenqtt.ChannelManager#shutdown()
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
	 * @see net.sf.xenqtt.ChannelManager#isRunning()
	 */
	@Override
	public boolean isRunning() {
		return ioThread.isAlive();
	}

	// FIXME [jim] - test
	/**
	 * @see net.sf.xenqtt.ChannelManager#newClientChannel(java.lang.String, net.sf.xenqtt.message.MessageHandler)
	 */
	@Override
	public MqttChannelRef newClientChannel(String brokerUri, MessageHandler messageHandler) throws MqttInterruptedException {
		try {
			return newClientChannel(new URI(brokerUri), messageHandler);
		} catch (URISyntaxException e) {
			throw new MqttException("Failed to parse broker URI: " + brokerUri, e);
		}
	}

	// FIXME [jim] - test
	/**
	 * @see net.sf.xenqtt.ChannelManager#newClientChannel(java.net.URI, net.sf.xenqtt.message.MessageHandler)
	 */
	@Override
	public MqttChannelRef newClientChannel(URI brokerUri, MessageHandler messageHandler) throws MqttInterruptedException {

		if (!"tcp".equals(brokerUri.getScheme())) {
			throw new MqttException("Invalid broker URI (scheme must be 'tcp'): " + brokerUri);
		}

		return newClientChannel(brokerUri.getHost(), brokerUri.getPort(), messageHandler);
	}

	/**
	 * @see net.sf.xenqtt.ChannelManager#newClientChannel(java.lang.String, int, net.sf.xenqtt.message.MessageHandler)
	 */
	@Override
	public MqttChannelRef newClientChannel(String host, int port, MessageHandler messageHandler) throws MqttInterruptedException {

		return new NewClientChannelCommand(host, port, messageHandler).await();
	}

	/**
	 * @see net.sf.xenqtt.ChannelManager#newBrokerChannel(java.nio.channels.SocketChannel, net.sf.xenqtt.message.MessageHandler)
	 */
	@Override
	public MqttChannelRef newBrokerChannel(SocketChannel socketChannel, MessageHandler messageHandler) throws MqttInterruptedException {

		return new NewBrokerChannelCommand(socketChannel, messageHandler).await();
	}

	/**
	 * @see net.sf.xenqtt.ChannelManager#send(net.sf.xenqtt.message.MqttChannelRef, net.sf.xenqtt.message.MqttMessage)
	 */
	@Override
	public boolean send(MqttChannelRef channel, MqttMessage message) throws MqttInterruptedException {

		SendCommand cmd = new SendCommand(channel, message);
		cmd.await();
		return cmd.returnValue;
	}

	/**
	 * @throws MqttInterruptedException
	 * @see net.sf.xenqtt.ChannelManager#sendToAll(net.sf.xenqtt.message.MqttMessage)
	 */
	@Override
	public void sendToAll(MqttMessage message) throws MqttInterruptedException {

		new SendToAllCommand(message).await();
	}

	/**
	 * @see net.sf.xenqtt.ChannelManager#close(net.sf.xenqtt.message.MqttChannelRef)
	 */
	@Override
	public void close(MqttChannelRef channel) throws MqttInterruptedException {

		new CloseCommand(channel).await();
	}

	// FIXME [jim] - test
	/**
	 * @see net.sf.xenqtt.ChannelManager#closeAll()
	 */
	@Override
	public void closeAll() {

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
			if (firstCommand != null) {
				firstCommand.execute(now);
				firstCommand = null;
			}
		} finally {
			commandsLock.unlock();
		}
	}

	private abstract class Command<T> {

		private final CountDownLatch done = new CountDownLatch(1);

		private Command<?> next;

		T returnValue;
		Throwable failCause;

		/**
		 * Adds this command to the list of commands then wakes up the selector and waits for the command to be executed
		 */
		final T await() throws MqttInterruptedException {

			commandsLock.lock();
			try {
				next = firstCommand;
				firstCommand = this;
			} finally {
				commandsLock.unlock();
			}

			selector.wakeup();
			try {
				done.await();
			} catch (InterruptedException e) {
				// FIXME [jim] - test that the interrupted flag gets reset, or maybe it shouldn't?
				// reset the thread's interrupted flag
				Thread.currentThread().interrupt();
				throw new MqttInterruptedException(e);
			}

			if (failCause != null) {
				if (failCause instanceof RuntimeException) {
					throw (RuntimeException) failCause;
				}
				if (failCause instanceof Error) {
					throw (Error) failCause;
				}

				throw new RuntimeException("Unexpected exception. This is a bug!", failCause);
			}

			return returnValue;
		}

		/**
		 * Executes the command. This should only be called by the ioThread.
		 */
		final void execute(long now) {
			try {
				// don't just catch Exception because if a different type of checked exception is thrown by doExecute we want it to be obvious because we need
				// to add explicit support for it to the await method in this class.
				returnValue = doExecute(now);
			} catch (RuntimeException e) {
				firstCommand.failCause = e;
			} catch (Error e) {
				firstCommand.failCause = e;
			}

			done.countDown();

			if (next != null) {
				next.execute(now);
			}
		}

		/**
		 * Each extending class implements its business logic in this method
		 */
		abstract T doExecute(long now);
	}

	private final class SendCommand extends Command<Boolean> {

		private final MqttMessage message;
		private final MqttChannel channel;

		public SendCommand(MqttChannelRef channel, MqttMessage message) {
			this.message = message;
			this.channel = (MqttChannel) channel;
		}

		@Override
		Boolean doExecute(long now) {
			return channel.send(message);
		}
	}

	private final class SendToAllCommand extends Command<Void> {

		private final MqttMessage message;

		public SendToAllCommand(MqttMessage message) {
			this.message = message;
		}

		@Override
		Void doExecute(long now) {

			for (MqttChannel channel : channels) {
				try {
					MqttMessage msg = new MqttMessage(message);
					channel.send(msg);
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
		Void doExecute(long now) {

			channel.close();
			return null;
		}
	}

	private final class NewClientChannelCommand extends Command<MqttClientChannel> {

		private final String host;
		private final int port;
		private final MessageHandler messageHandler;

		public NewClientChannelCommand(String host, int port, MessageHandler messageHandler) {
			this.host = host;
			this.port = port;
			this.messageHandler = messageHandler;
		}

		@Override
		MqttClientChannel doExecute(long now) {
			try {
				MqttClientChannel channel = new MqttClientChannel(host, port, messageHandler, selector, messageResendIntervalMillis);
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
		MqttBrokerChannel doExecute(long now) {
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
