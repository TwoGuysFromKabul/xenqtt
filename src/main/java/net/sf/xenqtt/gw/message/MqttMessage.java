package net.sf.xenqtt.gw.message;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * This is a generic MQTT message. Extending classes support more specific message types
 */
public class MqttMessage {

	private static final Charset UTF8 = Charset.forName("UTF-8");

	private final int remainingLength;

	protected final ByteBuffer buffer;

	/**
	 * Creates the message from a {@link ByteBuffer}. This is typically used for received messages. The buffer will be
	 * read to the end of the fixed header. Extending classes can assume the position of the buffer is at the end of the
	 * fixed header when this constructor returns.
	 * 
	 * @param buffer Byte buffer the contains this message. The buffer should already be populated and flipped. This
	 *            buffer is used directly by this class; it is not copied.
	 */
	public MqttMessage(ByteBuffer buffer, int remainingLength) {
		this.buffer = buffer;

		this.remainingLength = remainingLength;
		buffer.position(getRemainingLengthSize() + 1);
	}

	/**
	 * Creates a message with no variable header or payload (remaining length == 0). This is typically used to construct
	 * messages for sending.
	 */
//	public MqttMessage(MessageType messageType, boolean duplicate, QoS qos, boolean retain) {
//
//		this(messageType, duplicate, qos, retain, 0);
//
//		buffer.flip();
//	}

	/**
	 * Creates a message with an optional variable header and/or payload (remaining length >= 0). This is typically used
	 * to construct messages for sending. This should only be used by constructors in extending classes. The extending
	 * class should add any variable header and payload information to the buffer then call {@link ByteBuffer#flip()}.
	 */
	protected MqttMessage(MessageType messageType, boolean duplicate, QoS qos, boolean retain, int remainingLength) {

		int byte1 = (messageType.ordinal() << 4) + (qos.ordinal() << 1);

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
	}

	/**
	 * The type of message
	 */
	public final MessageType getMessageType() {
		return MessageType.values()[(buffer.get(0) >> 4) & 0xff];
	}

	/**
	 * This flag is set when the client or server attempts to re-deliver a PUBLISH, PUBREL, SUBSCRIBE or UNSUBSCRIBE
	 * message. This applies to messages where the value of QoS is greater than zero (0), and an acknowledgment is
	 * required. When the DUP bit is set, the variable header includes a Message ID.
	 * <p>
	 * The recipient should treat this flag as a hint as to whether the message may have been previously received. It
	 * should not be relied on to detect duplicates.
	 */
	public final boolean isDuplicate() {
		return (buffer.get(0) & 0x08) == 0x08;
	}

	/**
	 * The level of assurance for delivery of a PUBLISH message.
	 */
	public final QoS getQoS() {
		return QoS.values()[(buffer.get(0) >> 1) & 0xff];
	}

	/**
	 * This flag is only used on PUBLISH messages. When a client sends a PUBLISH to a server, if the Retain flag is set
	 * (1), the server should hold on to the message after it has been delivered to the current subscribers.
	 * <p>
	 * When a new subscription is established on a topic, the last retained message on that topic should be sent to the
	 * subscriber with the Retain flag set. If there is no retained message, nothing is sent
	 * <p>
	 * This is useful where publishers send messages on a "report by exception" basis, where it might be some time
	 * between messages. This allows new subscribers to instantly receive data with the retained, or Last Known Good,
	 * value.
	 * <p>
	 * When a server sends a PUBLISH to a client as a result of a subscription that already existed when the original
	 * PUBLISH arrived, the Retain flag should not be set, regardless of the Retain flag of the original PUBLISH. This
	 * allows a client to distinguish messages that are being received because they were retained and those that are
	 * being received "live".
	 * <p>
	 * Retained messages should be kept over restarts of the server.
	 * <p>
	 * A server may delete a retained message if it receives a message with a zero-length payload and the Retain flag
	 * set on the same topic.
	 */
	public final boolean isRetain() {
		return (buffer.get(0) & 0x01) == 1;
	}

	/**
	 * Represents the number of bytes remaining within the current message, including data in the variable header and
	 * the payload. The maximum value is 268,435,455 (256MB).
	 */
	public final int getRemainingLength() {

		return remainingLength;
	}

	/**
	 * Adds the string in the format required by MQTT to the end of the buffer. If the value is null nothing will be
	 * added to the buffer. If the value is an empty string a zero length string will be added to the buffer.
	 */
	protected final void putString(String value) {

		if (value != null) {
			byte[] bytes = value.getBytes(UTF8);
			buffer.putShort((short) bytes.length);
			buffer.put(bytes);
		}
	}

	/**
	 * @return A string from the current buffer position
	 */
	protected final String getString() {

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
			throw new IllegalArgumentException(
					"Remaining length is negative or exceeds supported maximum (268,435,455): " + remainingLength);
		}
	}
}
