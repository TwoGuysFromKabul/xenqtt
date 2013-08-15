package net.sf.xenqtt.message;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * Receives messages from a {@link ReadableByteChannel}.
 */
public final class ByteChannelMessageReceiver implements MessageReceiver {

	private final ReadableByteChannel channel;
	private final MessageHandler handler;

	// reads the first byte of the fixed header
	private final ByteBuffer header1 = ByteBuffer.allocate(2);

	// reads the next 3 bytes if the remaining length is > 127
	private final ByteBuffer header2 = ByteBuffer.allocate(3);

	// created on the fly to read any remaining data.
	private ByteBuffer remaining;

	/**
	 * @param channel
	 *            The channel to read from
	 * @param handler
	 *            The handler for received messages
	 */
	public ByteChannelMessageReceiver(ReadableByteChannel channel, MessageHandler handler) {
		this.channel = channel;
		this.handler = handler;
	}

	/**
	 * @see net.sf.xenqtt.message.MessageReceiver#receive()
	 */
	@Override
	public boolean receive() {

		// FIXME [jim] - read and handle messages

		return false;
	}
}
