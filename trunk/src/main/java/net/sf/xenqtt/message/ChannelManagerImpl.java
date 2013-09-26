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
package net.sf.xenqtt.message;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import net.sf.xenqtt.Log;
import net.sf.xenqtt.MqttException;
import net.sf.xenqtt.MqttInterruptedException;
import net.sf.xenqtt.MqttTimeoutException;

/**
 * Uses a single thread and non-blocking NIO to manage one or more {@link MqttChannel}s. You must call {@link #init()} before using this manager and
 * {@link #shutdown()} to shut it down.
 */
public final class ChannelManagerImpl implements ChannelManager {

	private final Set<MqttChannel> openChannels = new HashSet<MqttChannel>();
	private final long messageResendIntervalMillis;

	private final Lock commandsLock = new ReentrantLock();
	private Command<?> firstCommand;
	private boolean doShutdown;

	private final CountDownLatch readyLatch = new CountDownLatch(1);
	private final Thread ioThread;
	private final Selector selector;
	private final boolean blocking;
	private final long blockingTimeoutMillis;

	/**
	 * Use this constructor for the asynchronous API
	 * 
	 * @param messageResendIntervalSeconds
	 *            Seconds between attempts to resend a message that is {@link MqttMessage#isAckable()}. 0 to disable message resends
	 */
	public ChannelManagerImpl(long messageResendIntervalSeconds) {
		this(messageResendIntervalSeconds, -1);
	}

	/**
	 * Use this constructor for the synchronous API
	 * 
	 * @param messageResendIntervalSeconds
	 *            Seconds between attempts to resend a message that is {@link MqttMessage#isAckable()}. 0 to disable message resends
	 * @param blockingTimeoutSeconds
	 *            Seconds until a blocked method invocation times out and an {@link MqttTimeoutException} is thrown. -1 will create a non-blocking API, 0 will
	 *            create a blocking API with no timeout, > 0 will create a blocking API with the specified timeout.
	 */
	public ChannelManagerImpl(long messageResendIntervalSeconds, int blockingTimeoutSeconds) {

		this.blocking = blockingTimeoutSeconds >= 0;
		this.blockingTimeoutMillis = blockingTimeoutSeconds <= 0 ? Long.MAX_VALUE : blockingTimeoutSeconds * 1000;
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

		addCommand(new ShutdownCommand());

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

		return addCommand(new NewClientChannelCommand(host, port, messageHandler)).await(blockingTimeoutMillis, TimeUnit.MILLISECONDS);
	}

	/**
	 * @see net.sf.xenqtt.message.ChannelManager#newBrokerChannel(java.nio.channels.SocketChannel, net.sf.xenqtt.message.MessageHandler)
	 */
	@Override
	public MqttChannelRef newBrokerChannel(SocketChannel socketChannel, MessageHandler messageHandler) throws MqttInterruptedException {

		return addCommand(new NewBrokerChannelCommand(socketChannel, messageHandler)).await(blockingTimeoutMillis, TimeUnit.MILLISECONDS);
	}

	/**
	 * @see net.sf.xenqtt.message.ChannelManager#send(net.sf.xenqtt.message.MqttChannelRef, net.sf.xenqtt.message.MqttMessage)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T extends MqttMessage> T send(MqttChannelRef channel, MqttMessage message) throws MqttInterruptedException {

		MqttMessage msg = addCommand(new SendCommand(channel, message)).await(blockingTimeoutMillis, TimeUnit.MILLISECONDS);
		return (T) msg;
	}

	/**
	 * @see net.sf.xenqtt.message.ChannelManager#close(net.sf.xenqtt.message.MqttChannelRef)
	 */
	@Override
	public void close(MqttChannelRef channel) throws MqttInterruptedException {

		addCommand(new CloseCommand(channel, null)).await(blockingTimeoutMillis, TimeUnit.MILLISECONDS);
	}

	/**
	 * @see net.sf.xenqtt.message.ChannelManager#close(net.sf.xenqtt.message.MqttChannelRef, java.lang.Throwable)
	 */
	@Override
	public void close(MqttChannelRef channel, Throwable cause) {

		addCommand(new CloseCommand(channel, cause)).await(blockingTimeoutMillis, TimeUnit.MILLISECONDS);
	}

	/**
	 * @see net.sf.xenqtt.message.ChannelManager#cancelBlockingCommands(net.sf.xenqtt.message.MqttChannelRef)
	 */
	@Override
	public void cancelBlockingCommands(MqttChannelRef channel) {

		addCommand(new CancelBlockingCommandsCommand(channel)).await(blockingTimeoutMillis, TimeUnit.MILLISECONDS);
	}

	/**
	 * @see net.sf.xenqtt.message.ChannelManager#getUnsentMessages(net.sf.xenqtt.message.MqttChannelRef)
	 */
	@Override
	public List<MqttMessage> getUnsentMessages(MqttChannelRef channel) {
		return addCommand(new GetUnsentMessagesCommand(channel)).await(blockingTimeoutMillis, TimeUnit.MILLISECONDS);
	}

	/**
	 * @see net.sf.xenqtt.message.ChannelManager#transfer(net.sf.xenqtt.message.MqttChannelRef, net.sf.xenqtt.message.MqttChannelRef)
	 */
	@Override
	public void transfer(MqttChannelRef oldChannel, MqttChannelRef newChannel) {
		// FIXME [jim] - test transfer channel
		addCommand(new TransferCommand(oldChannel, newChannel)).await(blockingTimeoutMillis, TimeUnit.MILLISECONDS);
	}

	private void closeAll() {

		Log.debug("Channel manager closing all channels");
		for (MqttChannel channel : openChannels) {
			try {
				channel.close();
			} catch (Exception ignore) {
			} finally {
				channel.cancelBlockingCommands();
			}
		}
	}

	private void doIO() {

		Log.info("Channel manager thread started");

		readyLatch.countDown();

		try {

			long maxIdleTime = Long.MAX_VALUE;

			while (!doShutdown) {

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
			Log.info("Channel manager thread stopping");
		} catch (Throwable t) {
			Log.fatal(t, "Channel manager thread caught a fatal exception and is dying");
		}

		closeAll();

		try {
			selector.close();
		} catch (Exception ignore) {
		}
	}

	private void doConnect(long now, Set<SelectionKey> keys) {

		Iterator<SelectionKey> iter = keys.iterator();
		while (iter.hasNext()) {
			SelectionKey key = iter.next();
			try {
				if (key.isConnectable()) {
					MqttChannel channel = (MqttChannel) key.attachment();
					if (!channel.finishConnect()) {
						channelClosed(channel);
						iter.remove();
					}
				}
			} catch (CancelledKeyException e) {
				iter.remove();
			}
		}
	}

	private void doRead(long now, Set<SelectionKey> keys) {

		Iterator<SelectionKey> iter = keys.iterator();
		while (iter.hasNext()) {
			SelectionKey key = iter.next();
			try {
				if (key.isReadable()) {
					MqttChannel channel = (MqttChannel) key.attachment();
					if (!channel.read(now)) {
						channelClosed(channel);
						iter.remove();
					}
				}
			} catch (CancelledKeyException e) {
				iter.remove();
			}
		}
	}

	private void doWrite(long now, Set<SelectionKey> keys) {

		Iterator<SelectionKey> iter = keys.iterator();
		while (iter.hasNext()) {
			SelectionKey key = iter.next();
			try {
				if (key.isWritable()) {
					MqttChannel channel = (MqttChannel) key.attachment();
					if (!channel.write(now)) {
						channelClosed(channel);
						iter.remove();
					}
				}
			} catch (CancelledKeyException e) {
				iter.remove();
			}
		}
	}

	private long doHouseKeeping(long now, Set<SelectionKey> keys) {

		long maxIdleTime = Long.MAX_VALUE;

		for (SelectionKey key : keys) {
			MqttChannel channel = (MqttChannel) key.attachment();
			long time = channel.houseKeeping(now);
			if (time < 0) {
				channelClosed(channel);
			} else if (time < maxIdleTime) {
				maxIdleTime = time;
			}
		}

		return maxIdleTime;
	}

	private void channelClosed(MqttChannel channel) {

		openChannels.remove(channel);
	}

	private void executeCommands(long now) {

		commandsLock.lock();
		try {
			while (firstCommand != null) {
				firstCommand.execute();
				if (firstCommand.unblockImmediately) {
					firstCommand.complete();
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
		private final boolean unblockImmediately;

		public Command(boolean unblockImmediately) {
			this.unblockImmediately = unblockImmediately;
		}
	}

	private final class SendCommand extends Command<MqttMessage> {

		private final MqttMessage message;
		private final MqttChannel channel;

		public SendCommand(MqttChannelRef channel, MqttMessage message) {
			super(!blocking);
			this.message = message;
			this.channel = (MqttChannel) channel;
		}

		@Override
		public void doExecute() {
			channel.send(message, this);
		}
	}

	private final class CloseCommand extends Command<Void> {

		private final MqttChannel channel;
		private final Throwable cause;

		public CloseCommand(MqttChannelRef channel, Throwable cause) {
			super(true);
			this.cause = cause;
			this.channel = (MqttChannel) channel;
		}

		@Override
		public void doExecute() {

			channel.close(cause);
		}
	}

	private final class CancelBlockingCommandsCommand extends Command<Void> {

		private final MqttChannel channel;

		public CancelBlockingCommandsCommand(MqttChannelRef channel) {
			super(true);
			this.channel = (MqttChannel) channel;
		}

		@Override
		public void doExecute() {

			channel.cancelBlockingCommands();
		}
	}

	private final class TransferCommand extends Command<Void> {

		private final DelegatingMqttChannel oldChannel;
		private final DelegatingMqttChannel newChannel;

		public TransferCommand(MqttChannelRef oldChannel, MqttChannelRef newChannel) {
			super(true);
			this.oldChannel = (DelegatingMqttChannel) oldChannel;
			this.newChannel = (DelegatingMqttChannel) newChannel;
		}

		@Override
		public void doExecute() {

			List<MqttMessage> unsentMessages = oldChannel.getUnsentMessages();
			for (MqttMessage message : unsentMessages) {
				message.blockingCommand.setFailureCause(null);
				newChannel.send(message, message.blockingCommand);
			}
			oldChannel.delegate = newChannel.delegate;
		}
	}

	private final class GetUnsentMessagesCommand extends Command<List<MqttMessage>> {

		private final MqttChannel channel;

		public GetUnsentMessagesCommand(MqttChannelRef channel) {
			super(true);
			this.channel = (MqttChannel) channel;
		}

		@Override
		public void doExecute() {

			List<MqttMessage> unsentMessages = channel.getUnsentMessages();
			setResult(unsentMessages);
		}
	}

	private final class NewClientChannelCommand extends Command<MqttChannel> {

		private final String host;
		private final int port;
		private final MessageHandler messageHandler;
		private MqttChannel channel;

		public NewClientChannelCommand(String host, int port, MessageHandler messageHandler) {
			super(!blocking);
			this.host = host;
			this.port = port;
			this.messageHandler = messageHandler;
		}

		@Override
		public void doExecute() {
			try {
				channel = new DelegatingMqttChannel(new MqttClientChannel(host, port, messageHandler, selector, messageResendIntervalMillis, this));
				openChannels.add(channel);
				setResult(channel);
			} catch (Exception e) {
				throw new MqttException("MQTT Client channel creation failed", e);
			}
		}
	}

	private final class NewBrokerChannelCommand extends Command<MqttBrokerChannel> {

		private final SocketChannel socketChannel;
		private final MessageHandler messageHandler;

		public NewBrokerChannelCommand(SocketChannel socketChannel, MessageHandler messageHandler) {
			super(true);
			this.socketChannel = socketChannel;
			this.messageHandler = messageHandler;
		}

		@Override
		public void doExecute() {
			try {
				MqttBrokerChannel channel = new MqttBrokerChannel(socketChannel, messageHandler, selector, messageResendIntervalMillis);
				openChannels.add(channel);
				setResult(channel);
			} catch (Exception e) {
				try {
					socketChannel.close();
				} catch (IOException ignore) {
				}
				throw new MqttException("MQTT broker channel creation failed", e);
			}
		}
	}

	private final class ShutdownCommand extends Command<Void> {

		public ShutdownCommand() {
			super(true);
		}

		@Override
		public void doExecute() {
			doShutdown = true;
		}
	}
}
