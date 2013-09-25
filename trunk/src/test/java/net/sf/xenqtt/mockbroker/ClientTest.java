package net.sf.xenqtt.mockbroker;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.List;

import net.sf.xenqtt.message.MqttChannel;
import net.sf.xenqtt.message.MqttMessage;
import net.sf.xenqtt.message.PubAckMessage;

import org.junit.Before;
import org.junit.Test;

public class ClientTest {

	MqttChannel channel = mock(MqttChannel.class);
	BrokerEvents events = new BrokerEvents();
	Client client = new Client(channel, events);

	@Before
	public void setup() {
		client.clientId = "clientId";
		client.cleanSession = false;
	}

	@Test
	public void testGetClientId() {
		assertEquals("clientId", client.getClientId());
	}

	@Test
	public void testSend() {
		MqttMessage message = new PubAckMessage(7);
		client.send(message);

		verify(channel).send(message, null);
		List<BrokerEvent> events = this.events.getEvents("clientId");
		assertEquals(1, events.size());
		assertSame(BrokerEventType.MESSAGE_SENT, events.get(0).getEventType());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSend_NullMessage() {
		client.send(null);
	}

}
