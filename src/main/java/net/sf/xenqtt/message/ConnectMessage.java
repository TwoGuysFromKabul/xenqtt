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

import java.nio.ByteBuffer;

/**
 * The MQTT connect message. When a TCP/IP socket connection is established from a client to a server, a protocol level session must be created using a CONNECT
 * flow.
 */
public final class ConnectMessage extends MqttMessage {

	private final byte flags;
	private final String protocolName;
	private final int protocolVersion;
	private final String clientId;
	private final String userName;
	private final String password;
	private final String willTopic;
	private final String willMessage;
	private final int keepAliveSeconds;

	/**
	 * Create a new instance of this class.
	 * 
	 * @param buffer
	 *            The buffer that contains the contents of the message
	 * @param remainingLength
	 *            The remaining length of the message
	 * @param receivedTimestamp
	 *            The time at which the message was received
	 */
	public ConnectMessage(ByteBuffer buffer, int remainingLength, long receivedTimestamp) {
		super(buffer, remainingLength, receivedTimestamp);

		this.protocolName = getString();
		this.protocolVersion = buffer.get() & 0xff;
		this.flags = buffer.get();
		this.keepAliveSeconds = buffer.getShort() & 0xffff;
		this.clientId = getString();

		boolean willFlag = isWillMessageFlag();
		this.willTopic = willFlag ? getString() : null;
		this.willMessage = willFlag ? getString() : null;
		this.userName = isUserNameFlag() && buffer.hasRemaining() ? getString() : null;
		this.password = isPasswordFlag() && buffer.hasRemaining() ? getString() : null;
	}

	/**
	 * Create an instance with no credentials and no will message.
	 */
	public ConnectMessage(String clientId, boolean cleanSession, int keepAliveSeconds) {
		this(clientId, cleanSession, keepAliveSeconds, null, null, null, null, null, false);
	}

	/**
	 * Create an instance with credentials and no will message.
	 */
	public ConnectMessage(String clientId, boolean cleanSession, int keepAliveSeconds, String userName, String password) {
		this(clientId, cleanSession, keepAliveSeconds, userName, password, null, null, null, false);
	}

	/**
	 * Create an instance with no credentials and a will message.
	 */
	public ConnectMessage(String clientId, boolean cleanSession, int keepAliveSeconds, String willTopic, String willMessage, QoS willQos, boolean willRetain) {
		this(clientId, cleanSession, keepAliveSeconds, null, null, willTopic, willMessage, willQos, willRetain);
	}

	/**
	 * Create an instance with credentials and a will message.
	 */
	public ConnectMessage(String clientId, boolean cleanSession, int keepAliveSeconds, String userName, String password, String willTopic, String willMessage,
			QoS willQos, boolean willRetain) {
		this(clientId, cleanSession, keepAliveSeconds, stringToUtf8(clientId), userName, stringToUtf8(userName), password, stringToUtf8(password), willTopic,
				stringToUtf8(willTopic), willMessage, stringToUtf8(willMessage), willQos, willRetain);

		if (willTopic == null) {
			if (willMessage != null) {
				throw new IllegalArgumentException("If willTopic is null then willMessage must be null");
			}
			if ((willQos != null)) {
				throw new IllegalArgumentException("If willTopic is null then willQos must be null");
			}
			if (willRetain) {
				throw new IllegalArgumentException("If willTopic is null then willRetain must be false");
			}
		} else if (willMessage == null) {
			throw new IllegalArgumentException("If willTopic is not null then willMessage must not be null");
		} else if (willQos == null) {
			throw new IllegalArgumentException("If willTopic is not null then willQos must not be null");
		} else if (willTopic.isEmpty()) {
			throw new IllegalArgumentException("willTopic may not be an empty string");
		}
		if (userName == null && password != null) {
			throw new IllegalArgumentException("If userName is null then password must be null");
		}
	}

	/**
	 * String that represents the protocol name MQIsdp, capitalized as shown.
	 */
	public String getProtocolName() {
		return protocolName;
	}

	/**
	 * The revision level of the protocol used by the client. The current version of the protocol is 3
	 */
	public int getProtocolVersion() {
		return protocolVersion;
	}

	/**
	 * If not set, then the server must store the subscriptions of the client after it disconnects. This includes continuing to store QoS 1 and QoS 2 messages
	 * for the subscribed topics so that they can be delivered when the client reconnects. The server must also maintain the state of in-flight messages being
	 * delivered at the point the connection is lost. This information must be kept until the client reconnects.
	 * <p>
	 * If set, then the server must discard any previously maintained information about the client and treat the connection as "clean". The server must also
	 * discard any state when the client disconnects.
	 * <p>
	 * Typically, a client will operate in one mode or the other and not change. The choice will depend on the application. A clean session client will not
	 * receive stale information and it must resubscribe each time it connects. A non-clean session client will not miss any QoS 1 or QoS 2 messages that were
	 * published whilst it was disconnected. QoS 0 messages are never stored, since they are delivered on a best efforts basis.
	 * <p>
	 * This flag was formerly known as "Clean start". It has been renamed to clarify the fact it applies to the whole session and not just to the initial
	 * connect.
	 * <p>
	 * A server may provide an administrative mechanism for clearing stored information about a client which can be used when it is known that a client will
	 * never reconnect.
	 */
	public boolean isCleanSession() {
		return (flags & 0x02) == 0x02;
	}

	/**
	 * The Client Identifier (Client ID) is between 1 and 23 characters long, and uniquely identifies the client to the server. It must be unique across all
	 * clients connecting to a single server, and is the key in handling messages with QoS levels 1 and 2. If the Client ID contains more than 23 characters,
	 * the server responds to the CONNECT message with a CONNACK return code 2: Identifier Rejected.
	 */
	public String getClientId() {
		return clientId;
	}

	/**
	 * The Will Message is published to the Will Topic. The QoS level is defined by {@link #getWillQoS()} and the RETAIN status is defined by
	 * {@link #isWillRetain()}.
	 * <p>
	 * Null if there is no Will Message.
	 */
	public String getWillTopic() {
		return willTopic;
	}

	/**
	 * @return True if the will message flag is set
	 */
	public boolean isWillMessageFlag() {
		return (flags & 0x04) == 0x04;
	}

	/**
	 * The Will Message defines the content of the message that is published to the Will Topic if the client is unexpectedly disconnected. This may be a
	 * zero-length message.
	 * <p>
	 * Although the Will Message is UTF-8 encoded in the CONNECT message, when it is published to the Will Topic only the bytes of the message are sent, not the
	 * first two length bytes. The message must therefore only consist of 7-bit ASCII characters.
	 * <p>
	 * Null if there is no Will Message. Zero length string if there is an empty Will Message.
	 */
	public String getWillMessage() {
		return willMessage;
	}

	/**
	 * @return The QoS of the {@link #willMessage}. If there is not a Will Message then this is not applicable.
	 */
	public QoS getWillQoS() {
		return QoS.values()[getWillQoSLevel()];
	}

	/**
	 * @return The integer value of the QoS of the {@link #willMessage}. If there is not a Will Message then this is not applicable.
	 */
	public int getWillQoSLevel() {
		return (flags & 0x18) >> 3;
	}

	/**
	 * The retain value of the Will message. False if either retain is false or there is no Will Message.
	 */
	public boolean isWillRetain() {
		return (flags & 0x20) == 0x20;
	}

	/**
	 * @return True if the user name flag is set
	 */
	public boolean isUserNameFlag() {
		return (flags & 0x80) == 0x80;
	}

	/**
	 * The user name identifies the name of the user who is connecting, which can be used for authentication. It is recommended that user names are kept to 12
	 * characters or fewer, but it is not required.
	 * <p>
	 * Note that, for compatibility with the original MQTT V3 specification, the Remaining Length field from the fixed header takes precedence over the User
	 * Name flag. Server implementations must allow for the possibility that the User Name flag is set, but the User Name string is missing. This is valid, and
	 * connections should be allowed to continue.
	 * <p>
	 * Null if there is no user name.
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * @return True if the password flag is set
	 */
	public boolean isPasswordFlag() {
		return (flags & 0x40) == 0x40;
	}

	/**
	 * If the Password flag is set, this is the next UTF-encoded string. The password corresponding to the user who is connecting, which can be used for
	 * authentication. It is recommended that passwords are kept to 12 characters or fewer, but it is not required.
	 * <p>
	 * Note that, for compatibility with the original MQTT V3 specification, the Remaining Length field from the fixed header takes precedence over the Password
	 * flag. Server implementations must allow for the possibility that the Password flag is set, but the Password string is missing. This is valid, and
	 * connections should be allowed to continue.
	 * <p>
	 * Null if there is no password. If there is no username there can be no password.
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * The Keep Alive timer, measured in seconds, defines the maximum time interval between messages received from a client. It enables the server to detect
	 * that the network connection to a client has dropped, without having to wait for the long TCP/IP timeout. The client has a responsibility to send a
	 * message within each Keep Alive time period. In the absence of a data-related message during the time period, the client sends a PINGREQ message, which
	 * the server acknowledges with a PINGRESP message.
	 * <p>
	 * If the server does not receive a message from the client within one and a half times the Keep Alive time period (the client is allowed "grace" of half a
	 * time period), it disconnects the client as if the client had sent a DISCONNECT message. This action does not impact any of the client's subscriptions.
	 * See DISCONNECT for more details.
	 * <p>
	 * If a client does not receive a PINGRESP message within a Keep Alive time period after sending a PINGREQ, it should close the TCP/IP socket connection.
	 * <p>
	 * The Keep Alive timer is a 16-bit value that represents the number of seconds for the time period. The actual value is application-specific, but a typical
	 * value is a few minutes. The maximum value is approximately 18 hours. A value of zero (0) means the client is not disconnected.
	 */
	public int getKeepAliveSeconds() {
		return keepAliveSeconds;
	}

	private byte buildFlags(boolean cleanSession, QoS willQos, boolean willRetain) {

		int flags = 0;
		if (userName != null) {
			flags |= 0x80; // bit 7
		}
		if (password != null) {
			flags |= 0x40; // bit 6
		}
		if (willTopic != null) {
			flags |= 0x04;
			if (willQos != null) {
				flags |= willQos.value() << 3;
			}
			if (willRetain) {
				flags |= 0x20;
			}
		}
		if (cleanSession) {
			flags |= 0x02;
		}
		// bit 0 is unused

		return (byte) flags;
	}

	private ConnectMessage(String clientId, boolean cleanSession, int keepAliveSeconds, byte[] clientIdUtf8, String userName, byte[] userNameUtf8,
			String password, byte[] passwordUtf8, String willTopic, byte[] willTopicUtf8, String willMessage, byte[] willMessageUtf8, QoS willQos,
			boolean willRetain) {
		super(MessageType.CONNECT, 12 + mqttStringSize(willTopicUtf8) + mqttStringSize(willMessageUtf8) + mqttStringSize(clientIdUtf8)
				+ mqttStringSize(userNameUtf8) + mqttStringSize(passwordUtf8));

		this.protocolName = "MQIsdp";
		this.protocolVersion = 3;
		this.keepAliveSeconds = keepAliveSeconds;
		this.clientId = clientId;
		this.willTopic = willTopic;
		this.willMessage = willMessage;
		this.userName = userName;
		this.password = password;
		this.flags = buildFlags(cleanSession, willQos, willRetain);

		putString(protocolName);
		buffer.put((byte) protocolVersion);
		buffer.put(flags);
		buffer.putShort((short) keepAliveSeconds);
		putString(clientIdUtf8);
		putString(willTopicUtf8);
		putString(willMessageUtf8);
		putString(userNameUtf8);
		putString(passwordUtf8);

		buffer.flip();
	}
}
