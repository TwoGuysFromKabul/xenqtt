package net.sf.xenqtt.message;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class MqttChannelImplTest {

	@Mock MessageHandler handler;

	int port;
	volatile Exception serverException;
	ByteBuffer serverBuffer = ByteBuffer.allocate(1024);
	Selector selector;

	TestServer server;
	MqttChannel channel;

	@Before
	public void setup() throws Exception {

		MockitoAnnotations.initMocks(this);

		selector = Selector.open();
		server = new TestServer();
		server.start();
	}

	// FIXME [jim] - only got as far as setting up the basic framework for getting a connection going
	@Test
	public void test() throws Exception {

		channel = new MqttChannelImpl("localhost", port, handler, selector);

		assertEquals(1, selector.select());
		SelectionKey key = selector.selectedKeys().iterator().next();
		assertEquals(SelectionKey.OP_CONNECT, key.readyOps());
		channel.finishConnect();

		channel.close();
		server.join();
		fail("Not yet implemented");

	}

	private class TestServer extends Thread {

		ServerSocketChannel ssc;

		public TestServer() throws Exception {
			ssc = ServerSocketChannel.open();
			ServerSocket serverSocket = ssc.socket();
			serverSocket.bind(new InetSocketAddress(0));
			port = serverSocket.getLocalPort();
		}

		@Override
		public void run() {

			SocketChannel socketChannel = null;
			try {

				socketChannel = ssc.accept();
				while (socketChannel.read(serverBuffer) > 0) {
					;
				}

			} catch (Exception e) {
				serverException = e;
			} finally {
				if (ssc != null) {
					try {
						ssc.close();
						socketChannel.close();
					} catch (IOException ignore) {
					}
				}
			}

		}
	}
}
