package net.sf.xenqtt.test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Hanldes IO for a single TCP connection for {@link BlockingTcpEchoClient} and {@link BlockingTcpEchoServer}.
 */
abstract class AbstractBlockingConnection {

	private static final int MAX_QUEUED_MSGS_PER_CONNECTION = 1000;
	private static final AtomicInteger NEXT_NUM = new AtomicInteger();

	private final int connectionNumber = NEXT_NUM.incrementAndGet();

	private final WriteThread writer = new WriteThread();
	private final ReadThread reader = new ReadThread();

	private final SocketChannel channel;

	public AbstractBlockingConnection(SocketChannel channel) {
		this.channel = channel;
	}

	public final void start() {

		writer.start();
		reader.start();
	}

	public final void send(ByteBuffer buffer) {
		try {
			writer.toSend.put(buffer);
		} catch (InterruptedException ignore) {
		}
	}

	void messageReceived(ByteBuffer buffer) {
	}

	void messageSent(ByteBuffer buffer) {
	}

	public final void close() {

		try {
			channel.close();
			writer.interrupt();
		} catch (IOException ignore) {
		}
	}

	private final class WriteThread extends Thread {

		private final BlockingQueue<ByteBuffer> toSend = new ArrayBlockingQueue<ByteBuffer>(MAX_QUEUED_MSGS_PER_CONNECTION);

		public WriteThread() {
			super("WriteThread-" + connectionNumber);
			setDaemon(true);
		}

		/**
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {

			try {
				for (;;) {
					ByteBuffer buffer = toSend.take();
					while (buffer != null && buffer.hasRemaining()) {
						channel.write(buffer);
					}

					messageSent(buffer);
				}
			} catch (InterruptedException ignore) {
			} catch (Exception e) {
				if (channel.isOpen()) {
					System.err.println(Thread.currentThread().getName());
					e.printStackTrace();
				}
			} finally {
				close();
			}
		}
	}

	private final class ReadThread extends Thread {

		private final ByteBuffer header = ByteBuffer.allocate(2);

		public ReadThread() {
			super("ReadThread-" + connectionNumber);
			setDaemon(true);
		}

		/**
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			try {
				read();
			} catch (Exception e) {
				if (channel.isOpen()) {
					System.err.println(Thread.currentThread().getName());
					e.printStackTrace();
				}
			} finally {
				close();
			}
		}

		private void read() throws IOException {
			for (;;) {
				while (header.hasRemaining()) {
					if (channel.read(header) < 0) {
						return;
					}
				}

				header.flip();

				ByteBuffer buffer = ByteBuffer.allocate((header.getShort(0) & 0xffff) + 2);
				buffer.put(header);
				while (buffer.hasRemaining()) {
					if (channel.read(buffer) < 0) {
						return;
					}
				}

				buffer.flip();
				messageReceived(buffer);

				header.clear();
			}
		}
	}
}