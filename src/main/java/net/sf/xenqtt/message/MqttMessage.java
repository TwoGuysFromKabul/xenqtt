package net.sf.xenqtt.message;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * This is a generic MQTT message. Extending classes support more specific message types
 */
public class MqttMessage {

	private static final byte[] HEX_CHAR_TABLE = { (byte) '0', (byte) '1', (byte) '2', (byte) '3', (byte) '4', (byte) '5', (byte) '6', (byte) '7', (byte) '8',
			(byte) '9', (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f' };

	private static final Charset UTF8 = Charset.forName("UTF-8");

	private final int remainingLength;

	/**
	 * The buffer that contains the entire message
	 */
	final ByteBuffer buffer;

	/**
	 * The offset of the first byte after the fixed header. This should be used for any explicit byte offset in an extending class that may have a remaining
	 * length > 127.
	 */
	final int fixedHeaderEndOffset;

	/**
	 * If not null then {@link BlockingCommand#complete(Throwable)} is invoked when this message is "complete". The definition of "complete" varies by message
	 * type. If the message is ackable then it is complete when the ack message is received. Otherwise, it is complete when it is written to the socket or the
	 * channel is closed. Used by {@link AbstractMqttChannel} to enable blocking.
	 */
	BlockingCommand<?> blockingCommand;

	private MessageType messageType;

	/**
	 * Creates a copy of the copyFrom message.
	 */
	public MqttMessage(MqttMessage copyFrom) {
		this.buffer = copyFrom.buffer.asReadOnlyBuffer();
		this.fixedHeaderEndOffset = copyFrom.fixedHeaderEndOffset;
		this.messageType = copyFrom.messageType;
		this.remainingLength = copyFrom.remainingLength;
	}

	/**
	 * Creates the message from a {@link ByteBuffer}. This is typically used for received messages. The buffer will be read to the end of the fixed header. This
	 * should only be used by constructors in extending classes. Extending classes can assume the position of the buffer is at the end of the fixed header when
	 * this constructor returns.
	 * 
	 * @param buffer
	 *            Byte buffer the contains this message. The buffer should already be populated and flipped. This buffer is used directly by this class; it is
	 *            not copied.
	 */
	MqttMessage(ByteBuffer buffer, int remainingLength) {
		this.buffer = buffer;

		this.remainingLength = remainingLength;
		fixedHeaderEndOffset = getRemainingLengthSize() + 1;
		buffer.position(fixedHeaderEndOffset);
	}

	/**
	 * This is typically used to construct messages for sending when there is no duplicate, qos, or retain. This should only be used by constructors in
	 * extending classes. The extending class should add any variable header and payload information to the buffer then call {@link ByteBuffer#flip()}.
	 */
	MqttMessage(MessageType messageType, int remainingLength) {
		this(messageType, false, null, false, remainingLength);
	}

	/**
	 * This is typically used to construct messages for sending. This should only be used by constructors in extending classes. The extending class should add
	 * any variable header and payload information to the buffer then call {@link ByteBuffer#flip()}.
	 */
	MqttMessage(MessageType messageType, boolean duplicate, QoS qos, boolean retain, int remainingLength) {

		int byte1 = messageType.value() << 4;
		if (qos != null) {
			byte1 |= qos.value() << 1;
		}

		if (duplicate) {
			byte1 |= 0x08;
		}

		if (retain) {
			byte1 |= 0x01;
		}

		this.remainingLength = remainingLength;

		byte[] remainingLengthBytes = buildRemainingLengthBytes();

		this.buffer = ByteBuffer.allocate(1 + remainingLengthBytes.length + remainingLength);
		buffer.put((byte) byte1);
		for (byte b : remainingLengthBytes) {
			buffer.put(b);
		}

		this.fixedHeaderEndOffset = buffer.position();
	}

	/**
	 * @return Space delimited hexadecimal string representation of the specified byte buffer
	 */
	public static String byteBufferToHex(ByteBuffer buffer) {

		return bytesToHex(getBytes(buffer));
	}

	/**
	 * @return Space delimited hexadecimal string representation of the specified byte array
	 */
	public static String bytesToHex(byte[] bytes) {

		final byte[] hex = new byte[(bytes.length - 1) * 3 + 2];

		for (int i = 0, j = 0; i < bytes.length; i++) {
			final int b = bytes[i] & 0xff;

			if (i != 0) {
				hex[j++] = ' ';
			}
			hex[j++] = HEX_CHAR_TABLE[b >>> 4];
			hex[j++] = HEX_CHAR_TABLE[b & 0xf];
		}

		return new String(hex, UTF8);
	}

	/**
	 * @return True if {@link #getQoSLevel()} > 0
	 */
	public final boolean isAckable() {

		return getQoSLevel() > 0;
	}

	/**
	 * @return True if this is an ack to an ackable message (does not include {@link ConnAckMessage}.
	 */
	public final boolean isAck() {

		MessageType type = getMessageType();
		return type == MessageType.PUBACK || type == MessageType.PUBREC || type == MessageType.PUBCOMP || type == MessageType.SUBACK
				|| type == MessageType.UNSUBACK;
	}

	/**
	 * The type of message
	 */
	public final MessageType getMessageType() {
		if (messageType == null) {
			messageType = MessageType.lookup((buffer.get(0) & 0xf0) >> 4);
		}

		return messageType;
	}

	/**
	 * This flag is set when the client or server attempts to re-deliver a PUBLISH, PUBREL, SUBSCRIBE or UNSUBSCRIBE message. This applies to messages where the
	 * value of QoS is greater than zero (0), and an acknowledgment is required. When the DUP bit is set, the variable header includes a Message ID.
	 * <p>
	 * The recipient should treat this flag as a hint as to whether the message may have been previously received. It should not be relied on to detect
	 * duplicates.
	 */
	public final boolean isDuplicate() {
		return (buffer.get(0) & 0x08) == 0x08;
	}

	/**
	 * The level of assurance for delivery of an {@link IdentifiableMqttMessage}.
	 */
	public final QoS getQoS() {
		return QoS.lookup(getQoSLevel());
	}

	/**
	 * The raw integer value for the level of assurance for delivery of a PUBLISH message.
	 */
	public final int getQoSLevel() {
		return (buffer.get(0) & 0x06) >> 1;
	}

	/**
	 * This flag is only used on PUBLISH messages. When a client sends a PUBLISH to a server, if the Retain flag is set (1), the server should hold on to the
	 * message after it has been delivered to the current subscribers.
	 * <p>
	 * When a new subscription is established on a topic, the last retained message on that topic should be sent to the subscriber with the Retain flag set. If
	 * there is no retained message, nothing is sent
	 * <p>
	 * This is useful where publishers send messages on a "report by exception" basis, where it might be some time between messages. This allows new subscribers
	 * to instantly receive data with the retained, or Last Known Good, value.
	 * <p>
	 * When a server sends a PUBLISH to a client as a result of a subscription that already existed when the original PUBLISH arrived, the Retain flag should
	 * not be set, regardless of the Retain flag of the original PUBLISH. This allows a client to distinguish messages that are being received because they were
	 * retained and those that are being received "live".
	 * <p>
	 * Retained messages should be kept over restarts of the server.
	 * <p>
	 * A server may delete a retained message if it receives a message with a zero-length payload and the Retain flag set on the same topic.
	 */
	public final boolean isRetain() {
		return (buffer.get(0) & 0x01) == 1;
	}

	/**
	 * Represents the number of bytes remaining within the current message, including data in the variable header and the payload. The maximum value is
	 * 268,435,455 (256MB).
	 */
	public final int getRemainingLength() {

		return remainingLength;
	}

	/**
	 * Sets the duplicate flag on this message. This is called before resending this message.
	 */
	public final void setDuplicateFlag() {
		buffer.put(0, (byte) (buffer.get(0) | 0x08));
	}

	/**
	 * @return The string value as UTF-8 bytes. UTF-8 is the character set used for all MQTT strings. Null if the value is null.
	 */
	final static byte[] stringToUtf8(String value) {

		return value == null ? null : value.getBytes(UTF8);
	}

	/**
	 * @return The string values as UTF-8 bytes. UTF-8 is the character set used for all MQTT strings. Null if the values is null. Null array entry if the
	 *         values array entry is null.
	 */
	final static byte[][] stringsToUtf8(String[] values) {

		byte[][] utf8 = new byte[values.length][];
		for (int i = 0; i < values.length; i++) {
			utf8[i] = stringToUtf8(values[i]);
		}
		return utf8;
	}

	/**
	 * @return The size of the MQTT string that will be created from the specified UTF-8 bytes. 0 if utf8 is null. 2 if utf8 is empty.
	 */
	final static int mqttStringSize(byte[] utf8) {
		return utf8 == null ? 0 : utf8.length + 2;
	}

	/**
	 * @return The size of the MQTT strings that will be created from the specified UTF-8 bytes. 0 if utf8 is null.
	 */
	final static int mqttStringSize(byte[][] utf8) {

		if (utf8 == null) {
			return 0;
		}

		int size = 0;

		for (byte[] b : utf8) {
			size += mqttStringSize(b);
		}

		return size;
	}

	/**
	 * Adds the UTF-8 string in the format required by MQTT to the end of the buffer. If utf8 is null nothing will be added to the buffer. If utf8 is a zero
	 * length array then a zero length string will be added to the buffer.
	 */
	final void putString(byte[] utf8) {

		if (utf8 != null) {
			buffer.putShort((short) utf8.length);
			buffer.put(utf8);
		}
	}

	/**
	 * Adds the string in the format required by MQTT to the end of the buffer. If the value is null nothing will be added to the buffer. If the value is an
	 * empty string a zero length string will be added to the buffer.
	 */
	final void putString(String value) {

		putString(stringToUtf8(value));
	}

	/**
	 * @return A string at the specified index. This will change the buffer's current position.
	 */
	final String getString(int index) {

		int pos = buffer.position();
		buffer.position(index);
		String value = getString();
		buffer.position(pos);
		return value;
	}

	/**
	 * @return A string from the current buffer position
	 */
	final String getString() {

		int len = buffer.getShort() & 0xffff;
		byte[] bytes = new byte[len];
		buffer.get(bytes);
		return new String(bytes, UTF8);
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {

		return Arrays.hashCode(getBytes());
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {

		if (obj == null || !(getClass().isAssignableFrom(obj.getClass()))) {
			return false;
		}

		MqttMessage that = (MqttMessage) obj;

		return Arrays.equals(getBytes(), that.getBytes());
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		return getClass().getSimpleName() + ": [" + byteBufferToHex(buffer) + "]";
	}

	private byte[] getBytes() {
		return getBytes(buffer);
	}

	private static byte[] getBytes(ByteBuffer buffer) {

		int pos = buffer.position();
		buffer.rewind();

		byte[] buf = new byte[buffer.limit()];
		buffer.get(buf);

		buffer.position(pos);

		return buf;
	}

	private byte[] buildRemainingLengthBytes() {

		byte[] remainingLengthBytes = new byte[getRemainingLengthSize()];

		int index = 0;
		int value = remainingLength;
		do {
			remainingLengthBytes[index] = (byte) (value % 0x80);
			value /= 0x80;
			if (value > 0) {
				remainingLengthBytes[index] |= 0x80;
			}
			index++;
		} while (value > 0);

		return remainingLengthBytes;
	}

	private int getRemainingLengthSize() {

		if (remainingLength >= 0 && remainingLength < 128) {
			return 1;
		} else if (remainingLength < 16384) {
			return 2;
		} else if (remainingLength < 2097152) {
			return 3;
		} else if (remainingLength < 268435456) {
			return 4;
		} else {
			throw new IllegalArgumentException("Remaining length is negative or exceeds supported maximum (268,435,455): " + remainingLength);
		}
	}
}
