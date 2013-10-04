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
package net.sf.xenqtt.client;

import java.util.List;

import net.sf.xenqtt.MqttCommandCancelledException;
import net.sf.xenqtt.MqttInterruptedException;
import net.sf.xenqtt.MqttInvocationError;
import net.sf.xenqtt.MqttInvocationException;
import net.sf.xenqtt.MqttQosNotGrantedException;
import net.sf.xenqtt.MqttTimeoutException;
import net.sf.xenqtt.message.ConnectReturnCode;
import net.sf.xenqtt.message.QoS;

/**
 * <p>
 * A client to an MQTT broker. This interface is implemented by both synchronous and asynchronous clients.
 * </p>
 * <p>
 * To use the synchronous client create a {@link SynchronousMqttClient} and {@link MqttClientListener}. The synchronous client blocks until it receives an
 * acknowledgment from the broker for each operation that requires such an acknowledgment. Each method's javadoc details what has to happen before the calling
 * thread is allowed to return. If the timeout configured in the {@link SynchronousMqttClient} expires an {@link MqttTimeoutException} is thrown and any
 * messages it is trying to send will be cancelled. To abort any of the blocking client calls {@link Thread#interrupt() interrupt} the calling thread which will
 * cause an {@link MqttInterruptedException} to be thrown.
 * </p>
 * <p>
 * To use the asynchronous client create an {@link AsyncMqttClient} and {@link AsyncClientListener}. None of the methods in the asynchronous client block.
 * Asynchronous client methods will never throw an {@link MqttTimeoutException} or {@link MqttInterruptedException} and those that have a return type other than
 * <code>void</code> will return <code>null</code>. You implement {@link AsyncClientListener} to be informed of the results of an asynchronous client method.
 * </p>
 * <p>
 * Received publish messages are handled the same way by both the synchronous and asynchronous clients. When a publish message is received
 * {@link MqttClientListener#publishReceived(MqttClient, PublishMessage) publish} is invoked with the client that received the message and the message that was
 * received. If the message's {@link PublishMessage#getQoS() QoS} level is anything other than {@link QoS#AT_MOST_ONCE AT_MOST_ONCE} you must call
 * {@link PublishMessage#ack() ack()} when you are finished processing the message. It is recommended that you always call {@link PublishMessage#ack() ack()}
 * regardless of the message's {@link PublishMessage#getQoS() QoS}. If you do not call ack or you wait too long to call ack the message will be resent by the
 * broker and {@link MqttClientListener#publishReceived(MqttClient, PublishMessage) publish} will be called with the resent message.
 * {@link PublishMessage#isDuplicate() isDuplicate()} will be <code>true</code> for messages that are resent by the broker.
 * </p>
 * <p>
 * If the connection is lost it will be restored using the {@link ReconnectionStrategy}, if any, provided to the client.
 * </p>
 * <p>
 * This class is thread safe.
 * </p>
 */
public interface MqttClient {

	/**
	 * @return True if this client has been closed. {@link MqttClientListener#disconnected(MqttClient, Throwable, boolean) Disconnected} will have been called
	 *         and all internal thread pools will be shut down or will be in the process of shutting down.
	 */
	boolean isClosed();

	/**
	 * Connects this client to the broker with credentials and a WillMessage. This includes these actions:
	 * <ol>
	 * <li>Complete the TCP connection to the broker if it hasn't completed already</li>
	 * <li>Send a connect message to the broker</li>
	 * <li>Receive a connect acknowledgment from the broker</li>
	 * </ol>
	 * If the synchronous client is used this method blocks until these actions are completed. If the asynchronous client is used the
	 * {@link AsyncClientListener#connected(MqttClient, ConnectReturnCode) connected} method is called after these actions are completed.
	 * 
	 * @param clientId
	 *            The Client Identifier (Client ID) is between 1 and 23 characters long, and uniquely identifies the client to the broker. It must be unique
	 *            across all clients connecting to a single broker. If the Client ID contains more than 23 characters, the broker responds with
	 *            {@link ConnectReturnCode#IDENTIFIER_REJECTED}.
	 * @param cleanSession
	 *            If not set, then the broker must store the subscriptions of the client after it disconnects. This includes continuing to store QoS 1 and QoS 2
	 *            messages for the subscribed topics so that they can be delivered when the client reconnects. The broker must also maintain the state of
	 *            in-flight messages being delivered at the point the connection is lost. This information must be kept until the client reconnects.
	 *            <p>
	 *            If set, then the broker must discard any previously maintained information about the client and treat the connection as "clean". The broker
	 *            must also discard any state when the client disconnects.
	 *            <p>
	 *            Typically, a client will operate in one mode or the other and not change. The choice will depend on the application. A clean session client
	 *            will not receive stale information and it must re-subscribe each time it connects. A non-clean session client will not miss any QoS 1 or QoS 2
	 *            messages that were published whilst it was disconnected. QoS 0 messages are never stored, since they are delivered on a best efforts basis.
	 * @param userName
	 *            The user name identifies the name of the user who is connecting, which can be used for authentication. It is recommended that user names are
	 *            kept to 12 characters or fewer, but it is not required.
	 *            <p>
	 *            Null if there is no user name.
	 * @param password
	 *            The password corresponding to the user who is connecting, which can be used for authentication. It is recommended that passwords are kept to
	 *            12 characters or fewer, but it is not required.
	 *            <p>
	 *            Null if there is no password. If there is no username there can be no password.
	 * @param willTopic
	 *            The Will Message is published to the Will Topic. If there is not a Will Message then this is not applicable.
	 *            <p>
	 *            Null if there is no Will Message.
	 * @param willMessage
	 *            The Will Message defines the content of the message that is published to the Will Topic if the client is unexpectedly disconnected. This may
	 *            be a zero-length message.
	 *            <p>
	 *            Although the Will Message is UTF-8 encoded in the CONNECT message, when it is published to the Will Topic only the bytes of the message are
	 *            sent, not the first two length bytes. The message must therefore only consist of 7-bit ASCII characters.
	 *            <p>
	 *            Null if there is no Will Message. Zero length string if there is an empty Will Message.
	 * @param willQos
	 *            The QoS of the {@link #willMessage}. If there is not a Will Message then this is not applicable.
	 * @param willRetain
	 *            The retain value of the Will message. False if either retain is false or there is no Will Message.
	 * 
	 * @return The {@link ConnectReturnCode return code} from the broker if the {@link SynchronousMqttClient} is used. Anything other than
	 *         {@link ConnectReturnCode#ACCEPTED} (or null) will result in the client being immediately disconnected. Null if the {@link AsyncMqttClient}
	 *         implementation is used.
	 * 
	 * @throws MqttCommandCancelledException
	 *             Thrown when the internal command used to implement this feature is cancelled.
	 * @throws MqttTimeoutException
	 *             Thrown when this method has blocked for approximately the configured timeout. Only applicable when the {@link SynchronousMqttClient
	 *             synchronous} implementation is used.
	 * @throws MqttInterruptedException
	 *             Thrown when the calling thread is {@link Thread#interrupt() interrupted}.
	 * @throws MqttInvocationException
	 *             Thrown when the internal command used to implement this feature throws an {@link Exception}.
	 * @throws MqttInvocationError
	 *             Thrown when the internal command used to implement this feature throws an {@link Error}.
	 */
	ConnectReturnCode connect(String clientId, boolean cleanSession, String userName, String password, String willTopic, String willMessage, QoS willQos,
			boolean willRetain) throws MqttCommandCancelledException, MqttTimeoutException, MqttInterruptedException, MqttInvocationException,
			MqttInvocationError;

	/**
	 * Connects this client to the broker with no credentials and no Will Message. Delegates to
	 * {@link #connect(String, boolean, int, String, String, String, String, QoS, boolean)}.
	 * 
	 * @see net.sf.xenqtt.client.MqttClient#connect(String, boolean, int, String, String, String, String, QoS, boolean)
	 */
	ConnectReturnCode connect(String clientId, boolean cleanSession) throws MqttCommandCancelledException, MqttTimeoutException, MqttInterruptedException,
			MqttInvocationException, MqttInvocationError;

	/**
	 * Connects this client to the broker with credentials but no Will Message. Delegates to
	 * {@link #connect(String, boolean, int, String, String, String, String, QoS, boolean)}.
	 * 
	 * @see net.sf.xenqtt.client.MqttClient#connect(String, boolean, int, String, String, String, String, QoS, boolean)
	 */
	ConnectReturnCode connect(String clientId, boolean cleanSession, String userName, String password) throws MqttCommandCancelledException,
			MqttTimeoutException, MqttInterruptedException, MqttInvocationException, MqttInvocationError;

	/**
	 * Connects this client to the broker with a Will Message but no credentials. Delegates to
	 * {@link #connect(String, boolean, int, String, String, String, String, QoS, boolean)}.
	 * 
	 * @see net.sf.xenqtt.client.MqttClient#connect(String, boolean, int, String, String, String, String, QoS, boolean)
	 */
	ConnectReturnCode connect(String clientId, boolean cleanSession, String willTopic, String willMessage, QoS willQos, boolean willRetain)
			throws MqttTimeoutException, MqttInterruptedException, MqttInvocationException, MqttInvocationError;

	/**
	 * Disconnects this client from the broker. This includes these actions:
	 * <ol>
	 * <li>Send a disconnect message to the broker</li>
	 * <li>Close the TCP connection to the broker</li>
	 * </ol>
	 * If the synchronous client is used this method blocks until these actions are completed. If the asynchronous client is used the
	 * {@link AsyncClientListener#disconnected(MqttClient, Throwable, boolean) disconnected} method is called after these actions are completed.
	 * 
	 * @throws MqttCommandCancelledException
	 *             Thrown when the internal command used to implement this feature is cancelled.
	 * @throws MqttTimeoutException
	 *             Thrown when this method has blocked for approximately the configured timeout. Only applicable when the {@link SynchronousMqttClient
	 *             synchronous} implementation is used.
	 * @throws MqttInterruptedException
	 *             Thrown when the calling thread is {@link Thread#interrupt() interrupted}.
	 * @throws MqttInvocationException
	 *             Thrown when the internal command used to implement this feature throws an {@link Exception}.
	 * @throws MqttInvocationError
	 *             Thrown when the internal command used to implement this feature throws an {@link Error}.
	 */
	void disconnect() throws MqttCommandCancelledException, MqttTimeoutException, MqttInterruptedException, MqttInvocationException, MqttInvocationError;

	/**
	 * Subscribes to topics. This includes these actions:
	 * <ol>
	 * <li>Send a subscribe message to the broker</li>
	 * <li>Receive a subscribe acknowledgment from the broker</li>
	 * <li></li>
	 * </ol>
	 * If the synchronous client is used this method blocks until these actions are completed. If the asynchronous client is used the
	 * {@link AsyncClientListener#subscribed(MqttClient, Subscription[]) subscribed} method is called after these actions are completed.
	 * 
	 * @param subscriptions
	 *            The topics to subscribe to and the requested QoS for each. The topics can include wildcards:
	 *            <ul>
	 *            <li>'+': Matches a single level in the topic. foo/+ would match foo/bar but not foo/a/b or foo/a/b/c. foo/+/+/c would match foo/a/b/c and
	 *            foo/d/g/c but not foo/a/c</li>
	 *            <li>'#': Matches the rest of the topic. Must be the last character in the topic. foo/# would match foo/bar, foo/a/b/c, etc</li>
	 *            </ul>
	 * 
	 * @return The topics subscribed to and the QoS granted for each if the {@link SynchronousMqttClient} is used. Null if the {@link AsyncMqttClient}
	 *         implementation is used.
	 * 
	 * @throws MqttQosNotGrantedException
	 *             Thrown when the QoS granted for any topic does not match the QoS requested.
	 * @throws MqttCommandCancelledException
	 *             Thrown when the internal command used to implement this feature is cancelled.
	 * @throws MqttTimeoutException
	 *             Thrown when this method has blocked for approximately the configured timeout. Only applicable when the {@link SynchronousMqttClient
	 *             synchronous} implementation is used.
	 * @throws MqttInterruptedException
	 *             Thrown when the calling thread is {@link Thread#interrupt() interrupted}.
	 * @throws MqttInvocationException
	 *             Thrown when the internal command used to implement this feature throws an {@link Exception}.
	 * @throws MqttInvocationError
	 *             Thrown when the internal command used to implement this feature throws an {@link Error}.
	 */
	Subscription[] subscribe(Subscription[] subscriptions) throws MqttQosNotGrantedException, MqttCommandCancelledException, MqttTimeoutException,
			MqttInterruptedException, MqttInvocationException, MqttInvocationError;

	/**
	 * Subscribes to topics. This is the same as {@link #subscribe(Subscription[])} except it uses {@link List lists} instead of arrays.
	 * 
	 * @see MqttClient#subscribe(Subscription[])
	 */
	List<Subscription> subscribe(List<Subscription> subscriptions) throws MqttQosNotGrantedException, MqttCommandCancelledException, MqttTimeoutException,
			MqttInterruptedException, MqttInvocationException, MqttInvocationError;

	/**
	 * Unsubscribes from topics. This includes these actions:
	 * <ol>
	 * <li>Send an unsubscribe message to the broker</li>
	 * <li>Receive the broker's unsubscribe acknowledgment</li>
	 * </ol>
	 * If the synchronous client is used this method blocks until these actions are completed. If the asynchronous client is used the
	 * {@link AsyncClientListener#unsubscribed(MqttClient, String[]) unsubscribed} method is called after these actions are completed.
	 * 
	 * @param topics
	 *            The topics to unsubscribe from. This can include wildcards:
	 *            <ul>
	 *            <li>'+': Matches a single level in the topic. foo/+ would match foo/bar but not foo/a/b or foo/a/b/c. foo/+/+/c would match foo/a/b/c and
	 *            foo/d/g/c but not foo/a/c</li>
	 *            <li>'#': Matches the rest of the topic. Must be the last character in the topic. foo/# would match foo/bar, foo/a/b/c, etc</li>
	 *            </ul>
	 * @throws MqttCommandCancelledException
	 *             Thrown when the internal command used to implement this feature is cancelled.
	 * @throws MqttTimeoutException
	 *             Thrown when this method has blocked for approximately the configured timeout. Only applicable when the {@link SynchronousMqttClient
	 *             synchronous} implementation is used.
	 * @throws MqttInterruptedException
	 *             Thrown when the calling thread is {@link Thread#interrupt() interrupted}.
	 * @throws MqttInvocationException
	 *             Thrown when the internal command used to implement this feature throws an {@link Exception}.
	 * @throws MqttInvocationError
	 *             Thrown when the internal command used to implement this feature throws an {@link Error}.
	 */
	void unsubscribe(String[] topics) throws MqttCommandCancelledException, MqttTimeoutException, MqttInterruptedException, MqttInvocationException,
			MqttInvocationError;

	/**
	 * Unsubscribes from topics. This is the same as {@link #unsubscribe(String[])} except it uses {@link List lists} instead of arrays.
	 */
	void unsubscribe(List<String> topics) throws MqttCommandCancelledException, MqttTimeoutException, MqttInterruptedException, MqttInvocationException,
			MqttInvocationError;

	/**
	 * Publishes a {@link PublishMessage message}. This includes these actions:
	 * <ol>
	 * <li>Send a publish message to the broker</li>
	 * <li>If the QoS is not {@link QoS#AT_MOST_ONCE} then wait for the broker's acknowledgment</li>
	 * </ol>
	 * If the synchronous client is used this method blocks until these actions are completed. If the asynchronous client is used the
	 * {@link AsyncClientListener#published(MqttClient, PublishMessage) published} method is called after these actions are completed.
	 * 
	 * @param message
	 *            The message to publish to the broker
	 * 
	 * @throws MqttCommandCancelledException
	 *             Thrown when the internal command used to implement this feature is cancelled.
	 * @throws MqttTimeoutException
	 *             Thrown when this method has blocked for approximately the configured timeout. Only applicable when the {@link SynchronousMqttClient
	 *             synchronous} implementation is used.
	 * @throws MqttInterruptedException
	 *             Thrown when the calling thread is {@link Thread#interrupt() interrupted}.
	 * @throws MqttInvocationException
	 *             Thrown when the internal command used to implement this feature throws an {@link Exception}.
	 * @throws MqttInvocationError
	 *             Thrown when the internal command used to implement this feature throws an {@link Error}.
	 */
	void publish(PublishMessage message) throws MqttCommandCancelledException, MqttTimeoutException, MqttInterruptedException, MqttInvocationException,
			MqttInvocationError;

	/**
	 * Closes this client without doing a clean disconnect. This includes these actions:
	 * <ol>
	 * <li>Close the TCP connection to the broker</li>
	 * <li></li>
	 * </ol>
	 * If the synchronous client is used this method blocks until these actions are completed. If the asynchronous client is used the
	 * {@link AsyncClientListener#disconnected(MqttClient, Throwable, boolean) disconnected} method is called after these actions are completed.
	 * 
	 * @throws MqttTimeoutException
	 *             Thrown when this method has blocked for approximately the configured timeout. Only applicable when the {@link SynchronousMqttClient
	 *             synchronous} implementation is used.
	 * @throws MqttInterruptedException
	 *             Thrown when the calling thread is {@link Thread#interrupt() interrupted}.
	 * @throws MqttInvocationException
	 *             Thrown when the internal command used to implement this feature throws an {@link Exception}.
	 * @throws MqttInvocationError
	 *             Thrown when the internal command used to implement this feature throws an {@link Error}.
	 */
	void close() throws MqttTimeoutException, MqttInterruptedException, MqttInvocationException, MqttInvocationError;
}
