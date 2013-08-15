package net.sf.xenqtt.message;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * This is a generic MQTT message. Extending classes support more specific message types
 */
public class MqttMessage {

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
	 * The type of message
	 */
	public final MessageType getMessageType() {
		return MessageType.lookup((buffer.get(0) >> 4) & 0xff);
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
	 * The level of assurance for delivery of a PUBLISH message.
	 */
	public final QoS getQoS() {
		return QoS.lookup((buffer.get(0) >> 1) & 0xff);
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
	 * @return The string value as UTF-8 bytes. UTF-8 is the character set used for all MQTT strings. Null if the value is null.
	 */
	final static byte[] stringToUtf8(String value) {

		return value == null ? null : value.getBytes(UTF8);
	}

	/**
	 * @return The size of the MQTT string that will be created from the specified UTF-8 bytes. 0 if utf8 is null. 2 if utf8 is empty.
	 */
	final static int mqttStringSize(byte[] utf8) {
		return utf8 == null ? 0 : utf8.length;
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
	 * @return A string from the current buffer position
	 */
	final String getString() {

		int len = buffer.getShort() & 0xffff;
		byte[] bytes = new byte[len];
		buffer.get(bytes);
		return new String(bytes, UTF8);
	}

	// FIXME - will need this code or similar later when building the received byte buffer
	private int parseRemainingLengthBytes() {

		int value = 0;
		byte b;
		int multiplier = 1;
		do {
			b = buffer.get();
			value += (b & 0x7f) * multiplier;
			multiplier *= 0x80;
		} while ((b & 0x80) != 0);

		return value;
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
