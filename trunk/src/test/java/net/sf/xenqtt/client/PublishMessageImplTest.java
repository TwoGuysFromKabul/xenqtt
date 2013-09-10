package net.sf.xenqtt.client;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import net.sf.xenqtt.message.ChannelManager;
import net.sf.xenqtt.message.MqttChannelRef;
import net.sf.xenqtt.message.PubAckMessage;
import net.sf.xenqtt.message.PubMessage;
import net.sf.xenqtt.message.QoS;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PublishMessageImplTest {

	@Mock ChannelManager channelManager;
	@Mock MqttChannelRef channel;
	PubMessage pubMessage = new PubMessage(QoS.AT_LEAST_ONCE, false, "my topic", 123, new byte[] { 97, 98, 99 });
	PublishMessage message;

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		message = new PublishMessageImpl(channelManager, channel, pubMessage);
	}

	@Test
	public void testGetTopic() throws Exception {
		assertEquals("my topic", message.getTopic());
	}

	@Test
	public void testGetPayload() throws Exception {

		assertArrayEquals(new byte[] { 97, 98, 99 }, message.getPayload());
	}

	@Test
	public void testGetPayloadString() throws Exception {

		assertEquals("abc", message.getPayloadString());
	}

	@Test
	public void testIsRetain() throws Exception {

		assertFalse(message.isRetain());

		pubMessage = new PubMessage(QoS.AT_LEAST_ONCE, true, "my topic", 123, new byte[] { 97, 98, 99 });
		message = new PublishMessageImpl(channelManager, channel, pubMessage);

		assertTrue(message.isRetain());
	}

	@Test
	public void testIsDuplicate() throws Exception {

		assertFalse(message.isDuplicate());

		pubMessage.setDuplicateFlag();

		assertTrue(message.isDuplicate());
	}

	@Test
	public void testGetQos() throws Exception {

		assertEquals(QoS.AT_LEAST_ONCE, message.getQoS());

		pubMessage = new PubMessage(QoS.AT_MOST_ONCE, false, "my topic", 123, new byte[] { 97, 98, 99 });
		message = new PublishMessageImpl(channelManager, channel, pubMessage);

		assertEquals(QoS.AT_MOST_ONCE, message.getQoS());
	}

	@Test
	public void testAck_Qos0() throws Exception {

		pubMessage = new PubMessage(QoS.AT_MOST_ONCE, false, "my topic", 123, new byte[] { 97, 98, 99 });
		message = new PublishMessageImpl(channelManager, channel, pubMessage);

		message.ack();
		verifyZeroInteractions(channelManager);
		assertFalse(Thread.interrupted());
	}

	@Test
	public void testAck_Qos1() throws Exception {

		message.ack();
		verify(channelManager).send(channel, new PubAckMessage(123));
		assertFalse(Thread.interrupted());
	}
}
