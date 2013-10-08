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

import java.net.URI;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnresolvedAddressException;
import java.util.List;

import net.sf.xenqtt.MqttCommandCancelledException;
import net.sf.xenqtt.MqttInterruptedException;
import net.sf.xenqtt.MqttInvocationError;
import net.sf.xenqtt.MqttInvocationException;
import net.sf.xenqtt.MqttTimeoutException;

/**
 * <p>
 * Specifies a type that manages zero or more {@link MqttChannelRef channels} that communicate via the MQTT protocol.
 * </p>
 * 
 * <p>
 * All of the methods in this specification are thread-safe. Implementations may be blocking or non-blocking
 * </p>
 */
public interface ChannelManager {

	/**
	 * Create a new client side {@link MqttChannelRef} for use in exchanging data using the MQTT protocol. This is the client end of the connection. The broker
	 * will have the remote end of the connection. This method only blocks long enough for the channel to be created, not for the TCP connection to happen.
	 * 
	 * @param brokerUri
	 *            URI of the broker to connect to. For example, tcp://q.m2m.io:1883.
	 * @param messageHandler
	 *            The {@link MessageHandler message handler} to use for all received messages
	 * 
	 * @return The newly-created channel. The channel may only be safely accessed from the {@link MessageHandler} callback methods.
	 * 
	 * @throws MqttCommandCancelledException
	 *             The channel manager uses a command pattern to process this request on the IO thread. If the command is cancelled for some reason, like the
	 *             channel closes, this exception is thrown.
	 * @throws MqttTimeoutException
	 *             Thrown when using a synchronous implementation and the timeout specified for a blocked method expires
	 * @throws MqttInterruptedException
	 *             Thrown when the calling thread is interrupted
	 * @throws MqttInvocationException
	 *             The channel manager uses a command pattern to process this request on the IO thread. Any {@link Exception} thrown while the command is being
	 *             processed will be wrapped in an {@link MqttInvocationException}.
	 * @throws MqttInvocationError
	 *             The channel manager uses a command pattern to process this request on the IO thread. Any {@link Error} thrown while the command is being
	 *             processed will be wrapped in an {@link MqttInvocationError}.
	 */
	MqttChannelRef newClientChannel(String brokerUri, MessageHandler messageHandler) throws MqttCommandCancelledException, MqttTimeoutException,
			MqttInterruptedException, MqttInvocationException, MqttInvocationError;

	/**
	 * Create a new client side {@link MqttChannelRef} for use in exchanging data using the MQTT protocol. This is the client end of the connection. The broker
	 * will have the remote end of the connection. This method only blocks long enough for the channel to be created, not for the TCP connection to happen. This
	 * will throw a {@link RuntimeException} wrapping any exception thrown while initializing the connection like {@link UnresolvedAddressException}
	 * 
	 * @param brokerUri
	 *            URI of the broker to connect to. For example, tcp://q.m2m.io:1883.
	 * @param messageHandler
	 *            The {@link MessageHandler message handler} to use for all received messages
	 * 
	 * @return The newly-created channel. The channel may only be safely accessed from the {@link MessageHandler} callback methods.
	 * 
	 * @throws MqttCommandCancelledException
	 *             The channel manager uses a command pattern to process this request on the IO thread. If the command is cancelled for some reason, like the
	 *             channel closes, this exception is thrown.
	 * @throws MqttTimeoutException
	 *             Thrown when using a synchronous implementation and the timeout specified for a blocked method expires
	 * @throws MqttInterruptedException
	 *             Thrown when the calling thread is interrupted
	 * @throws MqttInvocationException
	 *             The channel manager uses a command pattern to process this request on the IO thread. Any {@link Exception} thrown while the command is being
	 *             processed will be wrapped in an {@link MqttInvocationException}.
	 * @throws MqttInvocationError
	 *             The channel manager uses a command pattern to process this request on the IO thread. Any {@link Error} thrown while the command is being
	 *             processed will be wrapped in an {@link MqttInvocationError}.
	 */
	MqttChannelRef newClientChannel(URI brokerUri, MessageHandler messageHandler) throws MqttCommandCancelledException, MqttTimeoutException,
			MqttInterruptedException, MqttInvocationException, MqttInvocationError;

	/**
	 * Create a new client side {@link MqttChannelRef} for use in exchanging data using the MQTT protocol. This is the client end of the connection. The broker
	 * will have the remote end of the connection. This method only blocks long enough for the channel to be created, not for the TCP connection to happen. This
	 * will throw a {@link RuntimeException} wrapping any exception thrown while initializing the connection like {@link UnresolvedAddressException}
	 * 
	 * @param host
	 *            The host name to connect to
	 * @param port
	 *            The port to connect to
	 * @param messageHandler
	 *            The {@link MessageHandler message handler} to use for all received messages
	 * 
	 * @return The newly-created channel. The channel may only be safely accessed from the {@link MessageHandler} callback methods.
	 * 
	 * @throws MqttCommandCancelledException
	 *             The channel manager uses a command pattern to process this request on the IO thread. If the command is cancelled for some reason, like the
	 *             channel closes, this exception is thrown.
	 * @throws MqttTimeoutException
	 *             Thrown when using a synchronous implementation and the timeout specified for a blocked method expires
	 * @throws MqttInterruptedException
	 *             Thrown when the calling thread is interrupted
	 * @throws MqttInvocationException
	 *             The channel manager uses a command pattern to process this request on the IO thread. Any {@link Exception} thrown while the command is being
	 *             processed will be wrapped in an {@link MqttInvocationException}.
	 * @throws MqttInvocationError
	 *             The channel manager uses a command pattern to process this request on the IO thread. Any {@link Error} thrown while the command is being
	 *             processed will be wrapped in an {@link MqttInvocationError}.
	 */
	MqttChannelRef newClientChannel(String host, int port, MessageHandler messageHandler) throws MqttCommandCancelledException, MqttTimeoutException,
			MqttInterruptedException, MqttInvocationException, MqttInvocationError;

	/**
	 * Create a new broker side {@link MqttChannelRef} for use in exchanging data using the MQTT protocol. This is the broker end of the connection. The client
	 * will have the remote end of the connection. If an exception is thrown the socketChannel will be closed. This method only blocks long enough for the
	 * channel to be created.
	 * 
	 * @param socketChannel
	 *            The {@link SocketChannel channel} to communicate over
	 * @param messageHandler
	 *            The {@link MessageHandler message handler} to use for all received messages
	 * 
	 * @return The newly-created channel.The channel may only be safely accessed from the {@link MessageHandler} callback methods.
	 * 
	 * @throws MqttCommandCancelledException
	 *             The channel manager uses a command pattern to process this request on the IO thread. If the command is cancelled for some reason, like the
	 *             channel closes, this exception is thrown.
	 * @throws MqttTimeoutException
	 *             Thrown when using a synchronous implementation and the timeout specified for a blocked method expires
	 * @throws MqttInterruptedException
	 *             Thrown when the calling thread is interrupted
	 * @throws MqttInvocationException
	 *             The channel manager uses a command pattern to process this request on the IO thread. Any {@link Exception} thrown while the command is being
	 *             processed will be wrapped in an {@link MqttInvocationException}.
	 * @throws MqttInvocationError
	 *             The channel manager uses a command pattern to process this request on the IO thread. Any {@link Error} thrown while the command is being
	 *             processed will be wrapped in an {@link MqttInvocationError}.
	 */
	MqttChannelRef newBrokerChannel(SocketChannel socketChannel, MessageHandler messageHandler) throws MqttCommandCancelledException, MqttTimeoutException,
			MqttInterruptedException, MqttInvocationException, MqttInvocationError;

	/**
	 * Send a {@link MqttMessage message} over a specified {@code channel}. This method only blocks until the message is queued to send to the channel.
	 * 
	 * @param channel
	 *            The {@link MqttChannelRef channel} to send the message over. This channel should have been previously created via the
	 *            {@link #newChannel(String, int, MessageHandler)} or {@link #newChannel(SocketChannel, MessageHandler)} methods
	 * @param message
	 *            The {@code message} to send. This can be any type of MQTT message
	 * @return In a synchronous implementation this returns the ack message if the message being sent is a {@link ConnectMessage} or has a
	 *         {@link MqttMessage#getQoSLevel() QoS} > 0.
	 * 
	 * @throws MqttCommandCancelledException
	 *             The channel manager uses a command pattern to process this request on the IO thread. If the command is cancelled for some reason, like the
	 *             channel closes, this exception is thrown.
	 * @throws MqttTimeoutException
	 *             Thrown when using a synchronous implementation and the timeout specified for a blocked method expires
	 * @throws MqttInterruptedException
	 *             Thrown when the calling thread is interrupted
	 * @throws MqttInvocationException
	 *             The channel manager uses a command pattern to process this request on the IO thread. Any {@link Exception} thrown while the command is being
	 *             processed will be wrapped in an {@link MqttInvocationException}.
	 * @throws MqttInvocationError
	 *             The channel manager uses a command pattern to process this request on the IO thread. Any {@link Error} thrown while the command is being
	 *             processed will be wrapped in an {@link MqttInvocationError}.
	 */
	<T extends MqttMessage> T send(MqttChannelRef channel, MqttMessage message) throws MqttCommandCancelledException, MqttTimeoutException,
			MqttInterruptedException, MqttInvocationException, MqttInvocationError;

	/**
	 * Closes the specified channel. This method blocks until the channel is closed.
	 * 
	 * @throws MqttCommandCancelledException
	 *             The channel manager uses a command pattern to process this request on the IO thread. If the command is cancelled for some reason, like the
	 *             channel closes, this exception is thrown.
	 * @throws MqttTimeoutException
	 *             Thrown when using a synchronous implementation and the timeout specified for a blocked method expires
	 * @throws MqttInterruptedException
	 *             Thrown when the calling thread is interrupted
	 * @throws MqttInvocationException
	 *             The channel manager uses a command pattern to process this request on the IO thread. Any {@link Exception} thrown while the command is being
	 *             processed will be wrapped in an {@link MqttInvocationException}.
	 * @throws MqttInvocationError
	 *             The channel manager uses a command pattern to process this request on the IO thread. Any {@link Error} thrown while the command is being
	 *             processed will be wrapped in an {@link MqttInvocationError}.
	 */
	void close(MqttChannelRef channel) throws MqttCommandCancelledException, MqttTimeoutException, MqttInterruptedException, MqttInvocationException,
			MqttInvocationError;

	/**
	 * Closes the specified channel and sends cause to the {@link MessageHandler#channelClosed(MqttChannel, Throwable)} callback. This method blocks until the
	 * channel is closed.
	 * 
	 * @throws MqttCommandCancelledException
	 *             The channel manager uses a command pattern to process this request on the IO thread. If the command is cancelled for some reason, like the
	 *             channel closes, this exception is thrown.
	 * @throws MqttTimeoutException
	 *             Thrown when using a synchronous implementation and the timeout specified for a blocked method expires
	 * @throws MqttInterruptedException
	 *             Thrown when the calling thread is interrupted
	 * @throws MqttInvocationException
	 *             The channel manager uses a command pattern to process this request on the IO thread. Any {@link Exception} thrown while the command is being
	 *             processed will be wrapped in an {@link MqttInvocationException}.
	 * @throws MqttInvocationError
	 *             The channel manager uses a command pattern to process this request on the IO thread. Any {@link Error} thrown while the command is being
	 *             processed will be wrapped in an {@link MqttInvocationError}.
	 */
	void close(MqttChannelRef channel, Throwable cause) throws MqttCommandCancelledException, MqttTimeoutException, MqttInterruptedException,
			MqttInvocationException, MqttInvocationError;

	/**
	 * {@link BlockingCommand#cancel() Cancels} all blocking commands for the specified channel. This is not done when the channel is closed because we may want
	 * to reconnect instead of releasing the commands.
	 * 
	 * @throws MqttCommandCancelledException
	 *             The channel manager uses a command pattern to process this request on the IO thread. If the command is cancelled for some reason, like the
	 *             channel closes, this exception is thrown.
	 * @throws MqttTimeoutException
	 *             Thrown when using a synchronous implementation and the timeout specified for a blocked method expires
	 * @throws MqttInterruptedException
	 *             Thrown when the calling thread is interrupted
	 * @throws MqttInvocationException
	 *             The channel manager uses a command pattern to process this request on the IO thread. Any {@link Exception} thrown while the command is being
	 *             processed will be wrapped in an {@link MqttInvocationException}.
	 * @throws MqttInvocationError
	 *             The channel manager uses a command pattern to process this request on the IO thread. Any {@link Error} thrown while the command is being
	 *             processed will be wrapped in an {@link MqttInvocationError}.
	 */
	void cancelBlockingCommands(MqttChannelRef channel) throws MqttCommandCancelledException, MqttTimeoutException, MqttInterruptedException,
			MqttInvocationException, MqttInvocationError;

	/**
	 * @return All messages that have not been sent. This includes messages queued to be sent, any partially sent message, and all in flight messages.
	 * 
	 * @throws MqttCommandCancelledException
	 *             The channel manager uses a command pattern to process this request on the IO thread. If the command is cancelled for some reason, like the
	 *             channel closes, this exception is thrown.
	 * @throws MqttTimeoutException
	 *             Thrown when using a synchronous implementation and the timeout specified for a blocked method expires
	 * @throws MqttInterruptedException
	 *             Thrown when the calling thread is interrupted
	 * @throws MqttInvocationException
	 *             The channel manager uses a command pattern to process this request on the IO thread. Any {@link Exception} thrown while the command is being
	 *             processed will be wrapped in an {@link MqttInvocationException}.
	 * @throws MqttInvocationError
	 *             The channel manager uses a command pattern to process this request on the IO thread. Any {@link Error} thrown while the command is being
	 *             processed will be wrapped in an {@link MqttInvocationError}.
	 */
	List<MqttMessage> getUnsentMessages(MqttChannelRef channel) throws MqttCommandCancelledException, MqttTimeoutException, MqttInterruptedException,
			MqttInvocationException, MqttInvocationError;

	/**
	 * Transfers unsent messages from oldChannel to newChannel and changes oldChannel such that any messages sent to it will actually go to newChannel. This is
	 * used by reconnection logic to safely use a new connection in place of one that closed.
	 * 
	 * @throws MqttCommandCancelledException
	 *             The channel manager uses a command pattern to process this request on the IO thread. If the command is cancelled for some reason, like the
	 *             channel closes, this exception is thrown.
	 * @throws MqttTimeoutException
	 *             Thrown when using a synchronous implementation and the timeout specified for a blocked method expires
	 * @throws MqttInterruptedException
	 *             Thrown when the calling thread is interrupted
	 * @throws MqttInvocationException
	 *             The channel manager uses a command pattern to process this request on the IO thread. Any {@link Exception} thrown while the command is being
	 *             processed will be wrapped in an {@link MqttInvocationException}.
	 * @throws MqttInvocationError
	 *             The channel manager uses a command pattern to process this request on the IO thread. Any {@link Error} thrown while the command is being
	 *             processed will be wrapped in an {@link MqttInvocationError}.
	 */
	void transfer(MqttChannelRef oldChannel, MqttChannelRef newChannel) throws MqttCommandCancelledException, MqttTimeoutException, MqttInterruptedException,
			MqttInvocationException, MqttInvocationError;

	/**
	 * Detaches the specified channel from this manager's control. This is used in conjunction with {@link #attachChannel(MqttChannelRef)} to move handling of
	 * the channel's messages from one manager to another. This is always a synchronous operation.
	 * 
	 * @param channel
	 *            The channel to detach
	 * 
	 * @throws MqttCommandCancelledException
	 *             The channel manager uses a command pattern to process this request on the IO thread. If the command is cancelled for some reason, like the
	 *             channel closes, this exception is thrown.
	 * @throws MqttTimeoutException
	 *             Thrown when using a synchronous implementation and the timeout specified for a blocked method expires
	 * @throws MqttInterruptedException
	 *             Thrown when the calling thread is interrupted
	 * @throws MqttInvocationException
	 *             The channel manager uses a command pattern to process this request on the IO thread. Any {@link Exception} thrown while the command is being
	 *             processed will be wrapped in an {@link MqttInvocationException}.
	 * @throws MqttInvocationError
	 *             The channel manager uses a command pattern to process this request on the IO thread. Any {@link Error} thrown while the command is being
	 *             processed will be wrapped in an {@link MqttInvocationError}.
	 */
	void detachChannel(MqttChannelRef channel) throws MqttCommandCancelledException, MqttTimeoutException, MqttInterruptedException, MqttInvocationException,
			MqttInvocationError;

	/**
	 * Attaches the specified channel to this manager's control. This is used in conjunction with {@link #detachChannel(MqttChannelRef)} to move handling of the
	 * channel's messages from one manager to another. This is always a synchronous operation.
	 * 
	 * @param channel
	 *            The channel to attach
	 * @param messageHandler
	 *            The {@link MessageHandler message handler} to use for all received messages
	 * 
	 * @throws MqttCommandCancelledException
	 *             The channel manager uses a command pattern to process this request on the IO thread. If the command is cancelled for some reason, like the
	 *             channel closes, this exception is thrown.
	 * @throws MqttTimeoutException
	 *             Thrown when using a synchronous implementation and the timeout specified for a blocked method expires
	 * @throws MqttInterruptedException
	 *             Thrown when the calling thread is interrupted
	 * @throws MqttInvocationException
	 *             The channel manager uses a command pattern to process this request on the IO thread. Any {@link Exception} thrown while the command is being
	 *             processed will be wrapped in an {@link MqttInvocationException}.
	 * @throws MqttInvocationError
	 *             The channel manager uses a command pattern to process this request on the IO thread. Any {@link Error} thrown while the command is being
	 *             processed will be wrapped in an {@link MqttInvocationError}.
	 */
	void attachChannel(MqttChannelRef channel, MessageHandler messageHandler) throws MqttCommandCancelledException, MqttTimeoutException,
			MqttInterruptedException, MqttInvocationException, MqttInvocationError;

	/**
	 * Starts this channel manager. Must be called before any other methods
	 */
	void init();

	/**
	 * Stops this channel manager. Any other methods called after this have unpredictable results. Closes all open channels. Blocks until shutdown is complete.
	 */
	void shutdown();

	/**
	 * @return True if this manager is running and not yet stopped. This really means the manager's IO thread is running.
	 */
	boolean isRunning();
}
