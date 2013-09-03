package net.sf.xenqtt.proxy;


public class ProxyServerTest {
	// FIXME [jim] - implement proxy

	// volatile boolean failCreateSession;
	// volatile MqttChannel newSessionChannel;
	// volatile ConnectMessage newSessionMsg;
	//
	// volatile MqttChannel addedSessionChannel;
	// volatile ConnectMessage addedSessionMsg;
	//
	// volatile @Mock ProxySession session;
	//
	// volatile Selector selector;
	//
	// Thread serverThread;
	// volatile TestServer server;
	// volatile Exception ex;
	// volatile boolean addClientResult = true;
	//
	// @Before
	// public void setup() throws IOException {
	//
	// MockitoAnnotations.initMocks(this);
	// selector = Selector.open();
	// server = new TestServer();
	//
	// serverThread = new Thread() {
	// @Override
	// public void run() {
	//
	// try {
	// server.run(23416);
	// } catch (IOException e) {
	// ex = e;
	// }
	// }
	// };
	//
	// serverThread.start();
	//
	// doAnswer(new Answer<Object>() {
	// @Override
	// public Object answer(InvocationOnMock invocation) throws Throwable {
	//
	// if (!addClientResult) {
	// return false;
	// }
	//
	// addedSessionChannel = (MqttChannel) invocation.getArguments()[0];
	// addedSessionMsg = (ConnectMessage) invocation.getArguments()[1];
	// selector.wakeup();
	//
	// return true;
	// }
	// }).when(session).addClient(any(MqttChannel.class), any(ConnectMessage.class));
	//
	// when(session.isOpen()).thenReturn(true);
	// }
	//
	// @After
	// public void after() throws Exception {
	//
	// server.stop();
	// serverThread.join();
	// }
	//
	// @Test
	// public void testStop() throws Exception {
	//
	// server.stop();
	// serverThread.join();
	// }
	//
	// @Test
	// public void testSessionCleanup() throws Exception {
	//
	// when(session.isOpen()).thenReturn(false).thenReturn(true);
	//
	// MqttChannel clientChannel1 = new MqttChannelImpl("localhost", 23416, null, selector);
	// ConnectMessage connectMessage1 = new ConnectMessage("123", false, 0, "abc", "123");
	//
	// waitForConnectToSession(clientChannel1, true, connectMessage1);
	//
	// assertNotSame(connectMessage1, newSessionMsg);
	// assertEquals(connectMessage1, newSessionMsg);
	//
	// MqttChannel firstChannel = newSessionChannel;
	//
	// Thread.sleep(200);
	//
	// newSessionChannel = null;
	// newSessionMsg = null;
	// // establish another session with client id 123
	// MqttChannel clientChannel2 = new MqttChannelImpl("localhost", 23416, null, selector);
	// ConnectMessage connectMessage2 = new ConnectMessage("123", false, 0, "abc", "123");
	//
	// waitForConnectToSession(clientChannel2, true, connectMessage2);
	//
	// assertNotEquals(firstChannel, newSessionChannel);
	// assertNotSame(connectMessage1, connectMessage2);
	// assertEquals(connectMessage1, connectMessage2);
	//
	// assertNotSame(connectMessage2, newSessionMsg);
	// assertEquals(connectMessage2, newSessionMsg);
	//
	// // clean up
	// clientChannel1.close();
	// clientChannel2.close();
	//
	// server.stop();
	//
	// // this only happens once because the first session is cleaned up
	// verify(session).close();
	// }
	//
	// @Test
	// public void testSessionAbort() throws Exception {
	//
	// MqttChannel clientChannel = new MqttChannelImpl("localhost", 23416, null, selector);
	// ConnectMessage connectMessage = new ConnectMessage("123", false, 0, "abc", "123");
	//
	// waitForConnectToSession(clientChannel, true, connectMessage);
	//
	// // we sleep here just to be sure that the session does not get disconnected
	// Thread.sleep(200);
	//
	// clientChannel.close();
	// server.stop();
	//
	// verify(session).close();
	// }
	//
	// @Test
	// public void testNewClient_SessionCreationFails() throws Exception {
	//
	// failCreateSession = true;
	//
	// MqttChannel clientChannel = new MqttChannelImpl("localhost", 23416, null, selector);
	// ConnectMessage connectMessage = new ConnectMessage("123", false, 0, "abc", "123");
	//
	// waitForConnectToSession(clientChannel, true, connectMessage);
	//
	// assertFalse(clientChannel.isOpen());
	//
	// clientChannel.close();
	// server.stop();
	// }
	//
	// @Test
	// public void testNewClient_SessionAdditionFails() throws Exception {
	//
	// doThrow(new RuntimeException()).when(session).addClient(any(MqttChannel.class), any(ConnectMessage.class));
	//
	// MqttChannel clientChannel1 = new MqttChannelImpl("localhost", 23416, null, selector);
	// ConnectMessage connectMessage1 = new ConnectMessage("123", false, 0, "abc", "123");
	//
	// waitForConnectToSession(clientChannel1, true, connectMessage1);
	//
	// MqttChannel clientChannel2 = new MqttChannelImpl("localhost", 23416, null, selector);
	// ConnectMessage connectMessage2 = new ConnectMessage("123", false, 0, "abc", "123");
	//
	// waitForConnectToSession(clientChannel2, false, connectMessage2);
	//
	// assertTrue(clientChannel1.isOpen());
	// assertFalse(clientChannel2.isOpen());
	//
	// clientChannel1.close();
	// clientChannel2.close();
	// server.stop();
	// }
	//
	// @Test
	// public void testNewClient_NewSession_NoOtherSessions() throws Exception {
	//
	// MqttChannel clientChannel = new MqttChannelImpl("localhost", 23416, null, selector);
	// ConnectMessage connectMessage = new ConnectMessage("123", false, 0, "abc", "123");
	//
	// waitForConnectToSession(clientChannel, true, connectMessage);
	//
	// clientChannel.close();
	// server.stop();
	//
	// verify(session).close();
	// assertNotSame(connectMessage, newSessionMsg);
	// assertEquals(connectMessage, newSessionMsg);
	// assertNull(addedSessionChannel);
	// assertNull(addedSessionMsg);
	// }
	//
	// @Test
	// public void testNewClient_NewSession_OtherSessions() throws Exception {
	//
	// // establish first session with client id 123
	// MqttChannel clientChannel1 = new MqttChannelImpl("localhost", 23416, null, selector);
	// ConnectMessage connectMessage1 = new ConnectMessage("123", false, 0, "abc", "123");
	//
	// waitForConnectToSession(clientChannel1, true, connectMessage1);
	//
	// assertNotSame(connectMessage1, newSessionMsg);
	// assertEquals(connectMessage1, newSessionMsg);
	//
	// MqttChannel firstChannel = newSessionChannel;
	//
	// // establish first session with client id 456
	// newSessionChannel = null;
	// newSessionMsg = null;
	//
	// MqttChannel clientChannel2 = new MqttChannelImpl("localhost", 23416, null, selector);
	// ConnectMessage connectMessage2 = new ConnectMessage("456", false, 0, "abc", "123");
	//
	// waitForConnectToSession(clientChannel2, true, connectMessage2);
	//
	// assertNotEquals(firstChannel, newSessionChannel);
	// assertNotEquals(connectMessage1, connectMessage2);
	//
	// assertNotSame(connectMessage2, newSessionMsg);
	// assertEquals(connectMessage2, newSessionMsg);
	//
	// // clean up
	// clientChannel1.close();
	// clientChannel2.close();
	//
	// server.stop();
	// verify(session, times(2)).close();
	// assertNull(addedSessionChannel);
	// assertNull(addedSessionMsg);
	// }
	//
	// @Test
	// public void testNewClient_ExistingSessionIsOpen() throws Exception {
	//
	// // establish first session with client id 123
	// MqttChannel clientChannel1 = new MqttChannelImpl("localhost", 23416, null, selector);
	// ConnectMessage connectMessage1 = new ConnectMessage("123", false, 0, "abc", "123");
	//
	// waitForConnectToSession(clientChannel1, true, connectMessage1);
	//
	// assertNotSame(connectMessage1, newSessionMsg);
	// assertEquals(connectMessage1, newSessionMsg);
	//
	// MqttChannel firstChannel = newSessionChannel;
	//
	// // establish another session with client id 123
	// newSessionChannel = null;
	// newSessionMsg = null;
	//
	// MqttChannel clientChannel2 = new MqttChannelImpl("localhost", 23416, null, selector);
	// ConnectMessage connectMessage2 = new ConnectMessage("123", false, 0, "abc", "123");
	//
	// waitForConnectToSession(clientChannel2, false, connectMessage2);
	//
	// assertNotEquals(firstChannel, addedSessionChannel);
	// assertNotSame(connectMessage1, connectMessage2);
	// assertEquals(connectMessage1, connectMessage2);
	//
	// assertNotSame(connectMessage2, addedSessionMsg);
	// assertEquals(connectMessage2, addedSessionMsg);
	//
	// // clean up
	// clientChannel1.close();
	// clientChannel2.close();
	//
	// server.stop();
	// verify(session).close();
	// assertNull(newSessionChannel);
	// assertNull(newSessionMsg);
	// }
	//
	// @Test
	// public void testNewClient_ExistingSessionIsClosed() throws Exception {
	//
	// addClientResult = false;
	//
	// // establish first session with client id 123
	// MqttChannel clientChannel1 = new MqttChannelImpl("localhost", 23416, null, selector);
	// ConnectMessage connectMessage1 = new ConnectMessage("123", false, 0, "abc", "123");
	//
	// waitForConnectToSession(clientChannel1, true, connectMessage1);
	//
	// assertNotSame(connectMessage1, newSessionMsg);
	// assertEquals(connectMessage1, newSessionMsg);
	//
	// MqttChannel firstChannel = newSessionChannel;
	//
	// // establish another session with client id 123
	// newSessionChannel = null;
	// newSessionMsg = null;
	//
	// MqttChannel clientChannel2 = new MqttChannelImpl("localhost", 23416, null, selector);
	// ConnectMessage connectMessage2 = new ConnectMessage("123", false, 0, "abc", "123");
	//
	// waitForConnectToSession(clientChannel2, true, connectMessage2);
	//
	// assertNotEquals(firstChannel, newSessionChannel);
	// assertNotSame(connectMessage1, connectMessage2);
	// assertEquals(connectMessage1, connectMessage2);
	//
	// assertNotSame(connectMessage2, newSessionMsg);
	// assertEquals(connectMessage2, newSessionMsg);
	//
	// // clean up
	// clientChannel1.close();
	// clientChannel2.close();
	//
	// server.stop();
	// // this only happens once because the first channel reported itself closed already
	// verify(session).close();
	// assertNull(addedSessionChannel);
	// assertNull(addedSessionMsg);
	// }
	//
	// private void waitForConnectToSession(MqttChannel clientChannel, boolean createdSession, ConnectMessage connectMessage) throws Exception {
	//
	// clientChannel.send(connectMessage);
	//
	// while ((createdSession && newSessionChannel == null) || (!createdSession && addedSessionChannel == null)) {
	// selector.select();
	// Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
	// while (iter.hasNext()) {
	//
	// SelectionKey key = iter.next();
	// MqttChannel channel = (MqttChannel) key.attachment();
	//
	// if (key.isConnectable()) {
	// channel.finishConnect();
	// }
	// if (key.isWritable()) {
	// channel.write();
	// }
	// if (key.isReadable()) {
	// if (!channel.read()) {
	// channel.close();
	// return;
	// }
	// }
	// iter.remove();
	// }
	// }
	// }
	//
	// private class TestServer extends ProxyServer {
	//
	// public TestServer() throws IOException {
	// super(null, 0, 100);
	// }
	//
	// @Override
	// ProxySession createSession(MqttChannel channel, ConnectMessage message) throws IOException {
	//
	// if (failCreateSession) {
	// throw new RuntimeException();
	// }
	//
	// newSessionChannel = channel;
	// newSessionMsg = message;
	// selector.wakeup();
	//
	// return session;
	// }
	// }
}
