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
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import net.sf.xenqtt.Log;
import net.sf.xenqtt.MqttCommandCancelledException;
import net.sf.xenqtt.MqttException;
import net.sf.xenqtt.MqttInterruptedException;
import net.sf.xenqtt.MqttInvocationError;
import net.sf.xenqtt.MqttInvocationException;
import net.sf.xenqtt.MqttTimeoutException;
import net.sf.xenqtt.client.LatencyStat;
import net.sf.xenqtt.client.MessageStats;

/**
 * Uses a single thread and non-blocking NIO to manage one or more {@link MqttChannel}s. You must call {@link #init()} before using this manager and
 * {@link #shutdown()} to shut it down.
 */
public final class ChannelManagerImpl implements ChannelManager {

	private final Set<MqttChannel> openChannels = new HashSet<MqttChannel>();
	private final long messageResendIntervalMillis;

	private final BlockingQueue<Command<?>> commands = new LinkedBlockingQueue<Command<?>>();
	private boolean doShutdown;

	private final CountDownLatch readyLatch = new CountDownLatch(1);
	private final Thread ioThread;
	private final Selector selector;
	private final boolean blocking;
	private final long blockingTimeoutMillis;

	private final MessageStatsImpl stats;

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
		this.stats = new MessageStatsImpl(openChannels);
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

		addCommand(new TransferCommand(oldChannel, newChannel)).await(blockingTimeoutMillis, TimeUnit.MILLISECONDS);
	}

	/**
	 * @see net.sf.xenqtt.message.ChannelManager#detachChannel(net.sf.xenqtt.message.MqttChannelRef)
	 */
	@Override
	public void detachChannel(MqttChannelRef channel) throws MqttCommandCancelledException, MqttTimeoutException, MqttInterruptedException,
			MqttInvocationException, MqttInvocationError {

		addCommand(new DetachChannelCommand(channel)).await(blockingTimeoutMillis, TimeUnit.MILLISECONDS);
	}

	/**
	 * @see net.sf.xenqtt.message.ChannelManager#attachChannel(net.sf.xenqtt.message.MqttChannelRef, net.sf.xenqtt.message.MessageHandler)
	 */
	@Override
	public void attachChannel(MqttChannelRef channel, MessageHandler messageHandler) throws MqttCommandCancelledException, MqttTimeoutException,
			MqttInterruptedException, MqttInvocationException, MqttInvocationError {

		addCommand(new AttachChannelCommand(channel, messageHandler)).await(blockingTimeoutMillis, TimeUnit.MILLISECONDS);
	}

	/**
	 * @see net.sf.xenqtt.message.ChannelManager#getStats(boolean)
	 */
	@Override
	public MessageStats getStats(boolean reset) {
		return addCommand(new GetStatsCommand(stats, reset)).await(blockingTimeoutMillis, TimeUnit.MILLISECONDS);
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

		Log.debug("Channel manager thread started");

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

		} catch (Throwable t) {
			Log.fatal(t, "Channel manager thread caught a fatal exception and is dying");
		}

		Log.debug("Channel manager thread stopping");

		closeAll();

		try {
			selector.close();
		} catch (Exception ignore) {
		}

		startCleanupThread();
	}

	private void startCleanupThread() {

		Thread cleanupThread = new Thread("CommandCleanup") {
			@Override
			public void run() {
				try {
					for (;;) {
						Command<?> command = commands.take();
						command.cancel();
					}
				} catch (Exception ignore) {
				}
			};
		};
		cleanupThread.setDaemon(true);
		cleanupThread.start();
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

		removeFromOpenChannels(channel);
	}

	private void executeCommands(long now) {

		int size = commands.size();
		for (int i = 0; i < size; i++) {
			Command<?> command = commands.poll();
			if (command == null) {
				break;
			}
			command.execute(now);
			if (command.unblockImmediately) {
				command.complete();
			}
		}
	}

	private <T, C extends Command<T>> C addCommand(C command) {

		commands.add(command);
		selector.wakeup();

		return command;
	}

	private void addToOpenChannels(MqttChannel channel) {
		channel = channel instanceof DelegatingMqttChannel ? ((DelegatingMqttChannel) channel).delegate : channel;
		openChannels.add(channel);
	}

	private void removeFromOpenChannels(MqttChannel channel) {
		channel = channel instanceof DelegatingMqttChannel ? ((DelegatingMqttChannel) channel).delegate : channel;
		openChannels.remove(channel);
	}

	private abstract class Command<T> extends AbstractBlockingCommand<T> {

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
		public void doExecute(long now) {
			channel.send(message, this, now);
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
		public void doExecute(long now) {

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
		public void doExecute(long now) {

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
		public void doExecute(long now) {

			List<MqttMessage> unsentMessages = oldChannel.getUnsentMessages();
			for (MqttMessage message : unsentMessages) {
				message.blockingCommand.setFailureCause(null);
				newChannel.send(message, message.blockingCommand, now);
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
		public void doExecute(long now) {

			List<MqttMessage> unsentMessages = channel.getUnsentMessages();
			setResult(unsentMessages);
		}
	}

	private final class DetachChannelCommand extends Command<List<MqttMessage>> {

		private final MqttChannel channel;

		public DetachChannelCommand(MqttChannelRef channel) {
			super(true);
			this.channel = (MqttChannel) channel;
		}

		@Override
		public void doExecute(long now) {

			removeFromOpenChannels(channel);
			channel.deregister();
		}
	}

	private final class AttachChannelCommand extends Command<List<MqttMessage>> {

		private final MqttChannel channel;
		private final MessageHandler messageHandler;

		public AttachChannelCommand(MqttChannelRef channel, MessageHandler messageHandler) {
			super(true);
			this.messageHandler = messageHandler;
			this.channel = (MqttChannel) channel;
		}

		@Override
		public void doExecute(long now) {
			channel.register(selector, messageHandler);
			addToOpenChannels(channel);
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
		public void doExecute(long now) throws Exception {
			MqttChannel c = new MqttClientChannel(host, port, messageHandler, selector, messageResendIntervalMillis, this, stats);
			channel = new DelegatingMqttChannel(c);
			addToOpenChannels(c);
			setResult(channel);
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
		public void doExecute(long now) {
			try {
				MqttBrokerChannel channel = new MqttBrokerChannel(socketChannel, messageHandler, selector, messageResendIntervalMillis, stats);
				addToOpenChannels(channel);
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
		public void doExecute(long now) {
			doShutdown = true;
		}
	}

	private final class GetStatsCommand extends Command<MessageStats> {

		private final MessageStatsImpl stats;
		private final boolean reset;

		public GetStatsCommand(MessageStatsImpl stats, boolean reset) {
			super(true);
			this.stats = stats;
			this.reset = reset;
		}

		@Override
		public void doExecute(long now) {
			try {
				MessageStats stats = this.stats.clone();
				if (reset) {
					this.stats.reset();
				}

				setResult(stats);
			} catch (Exception ex) {
				Log.error(ex, "Unable to get a snapshot of the current stats.");
			}
		}

	}

	private final class MessageStatsImpl implements MutableMessageStats {

		private final Set<MqttChannel> registeredChannels;
		private final long messagesQueuedToSend;
		private final long messagesInFlight;
		private final MessageStat messagesSent;
		private final MessageStat messagesReceived;
		private final LatencyStatImpl ackLatency;

		public MessageStatsImpl(Set<MqttChannel> registeredChannels) {
			this.registeredChannels = registeredChannels;
			messagesQueuedToSend = 0;
			messagesInFlight = 0;
			messagesSent = new MessageStat();
			messagesReceived = new MessageStat();
			ackLatency = new LatencyStatImpl();
		}

		/**
		 * @see net.sf.xenqtt.client.MessageStats#getMessagesQueuedToSend()
		 */
		@Override
		public long getMessagesQueuedToSend() {
			return messagesQueuedToSend;
		}

		/**
		 * @see net.sf.xenqtt.client.MessageStats#getMessagesInFlight()
		 */
		@Override
		public long getMessagesInFlight() {
			return messagesInFlight;
		}

		/**
		 * @see net.sf.xenqtt.client.MessageStats#getMessagesSent()
		 */
		@Override
		public long getMessagesSent() {
			return messagesSent.value;
		}

		/**
		 * @see net.sf.xenqtt.client.MessageStats#getMessagesResent()
		 */
		@Override
		public long getMessagesResent() {
			return messagesSent.resendOrDup;
		}

		/**
		 * @see net.sf.xenqtt.client.MessageStats#getMessagesReceived()
		 */
		@Override
		public long getMessagesReceived() {
			return messagesReceived.value;
		}

		/**
		 * @see net.sf.xenqtt.client.MessageStats#getDuplicateMessagesReceived()
		 */
		@Override
		public long getDuplicateMessagesReceived() {
			return messagesReceived.resendOrDup;
		}

		/**
		 * @see net.sf.xenqtt.client.MessageStats#getMinAckLatencyMillis()
		 */
		@Override
		public long getMinAckLatencyMillis() {
			return ackLatency.getMin();
		}

		/**
		 * @see net.sf.xenqtt.client.MessageStats#getMaxAckLatencyMillis()
		 */
		@Override
		public long getMaxAckLatencyMillis() {
			return ackLatency.getMax();
		}

		/**
		 * @see net.sf.xenqtt.client.MessageStats#getAverageAckLatencyMillis()
		 */
		@Override
		public double getAverageAckLatencyMillis() {
			return ackLatency.getAverage();
		}

		/**
		 * @see net.sf.xenqtt.message.MutableMessageStats#messageSent(boolean)
		 */
		@Override
		public void messageSent(boolean resent) {
			messagesSent.messageInteraction(resent);
		}

		/**
		 * @see net.sf.xenqtt.message.MutableMessageStats#messageAcked(long)
		 */
		@Override
		public void messageAcked(long ackLatency) {
			this.ackLatency.processLatency(ackLatency);
		}

		/**
		 * @see net.sf.xenqtt.message.MutableMessageStats#messageReceived(boolean)
		 */
		@Override
		public void messageReceived(boolean duplicate) {
			messagesReceived.messageInteraction(duplicate);
		}

		/**
		 * @see net.sf.xenqtt.message.MutableMessageStats#reset()
		 */
		@Override
		public void reset() {
			messagesSent.reset();
			messagesReceived.reset();
			ackLatency.reset();
		}

		/**
		 * Returns a clone of this {@link MessageStatsImpl stats} instance. This method is invoked when a stats snapshot is requested. The clone is a deep
		 * copy.
		 * 
		 * @return A deep copy of this instance
		 * 
		 * @see java.lang.Object#clone()
		 */
		@Override
		public MessageStatsImpl clone() {
			long queuedToSend = getQueuedToSend();
			long inFlight = getInFlight();

			try {
				return new MessageStatsImpl(queuedToSend, inFlight, messagesSent.clone(), messagesReceived.clone(), ackLatency.clone());
			} catch (Exception ex) {
				Log.error(ex, "Unable to get the statistics snapshot");
				return null;
			}
		}

		private long getQueuedToSend() {
			if (registeredChannels == null) {
				return 0;
			}

			long queuedForSend = 0;
			for (MqttChannel channel : registeredChannels) {
				queuedForSend += channel.sendQueueDepth();
			}

			return queuedForSend;
		}

		private long getInFlight() {
			if (registeredChannels == null) {
				return 0;
			}

			long inFlight = 0;
			for (MqttChannel channel : registeredChannels) {
				inFlight += channel.inFlightMessageCount();
			}

			return inFlight;
		}

		/**
		 * Create a new instance of this class. This constructor is used when making a deep copy of this {@link MessageStatsImpl stats} instance.
		 * 
		 * @param messagesQueuedToSend
		 *            The messages currently queued for sending at the time of construction
		 * @param messagesInFlight
		 *            The messages in-flight at the time of construction
		 * @param messagesReceived
		 *            The messages received at the time of construction along with any resends
		 * @param messagesQueued
		 *            The messages that have been queued at the time of construction along with duplicates
		 * @param processQueueLatency
		 *            The latency of the process queue
		 * @param sendLatency
		 *            The latency of sending messages
		 * @param ackLatency
		 *            The latency around acks
		 */
		private MessageStatsImpl(long messagesQueuedToSend, long messagesInFlight, MessageStat messagesSent, MessageStat messagesReceived,
				LatencyStatImpl ackLatency) {
			this.messagesQueuedToSend = messagesQueuedToSend;
			this.messagesInFlight = messagesInFlight;
			this.messagesSent = messagesSent;
			this.messagesReceived = messagesReceived;
			this.ackLatency = ackLatency;
			registeredChannels = null;
		}

		private final class MessageStat implements Cloneable {

			private long value;
			private long resendOrDup;

			private void messageInteraction(boolean resendOrDup) {
				value++;
				if (resendOrDup) {
					this.resendOrDup++;
				}
			}

			private void reset() {
				value = resendOrDup = 0;
			}

			@Override
			public MessageStat clone() throws CloneNotSupportedException {
				return (MessageStat) super.clone();
			}

		}

		private final class LatencyStatImpl implements LatencyStat, Cloneable {

			private long count;
			private long sum;
			private long min;
			private long max;

			private void processLatency(long latency) {
				count++;
				sum += latency;
				if (min == 0 || latency < min) {
					min = latency;
				}

				if (max == 0 || latency > max) {
					max = latency;
				}
			}

			@Override
			public long getCount() {
				return count;
			}

			@Override
			public long getMin() {
				return min;
			}

			/**
			 * @see net.sf.xenqtt.client.LatencyStat#getMax()
			 */
			@Override
			public long getMax() {
				return max;
			}

			/**
			 * @see net.sf.xenqtt.client.LatencyStat#getAverage()
			 */
			@Override
			public double getAverage() {
				if (count == 0) {
					return 0.0;
				}

				return (sum * 1.0) / count;
			}

			private void reset() {
				count = sum = min = max = 0;
			}

			@Override
			public LatencyStatImpl clone() throws CloneNotSupportedException {
				return (LatencyStatImpl) super.clone();
			}

		}

	}
}
