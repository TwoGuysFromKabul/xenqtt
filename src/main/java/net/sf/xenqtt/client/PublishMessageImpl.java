package net.sf.xenqtt.client;

import java.nio.charset.Charset;

import net.sf.xenqtt.ChannelManager;
import net.sf.xenqtt.message.MqttChannelRef;
import net.sf.xenqtt.message.PubAckMessage;
import net.sf.xenqtt.message.PubMessage;
import net.sf.xenqtt.message.QoS;

/**
 * Implementation of {@link PublishMessage}. This is intentionally package visible as API users should not be creating it.
 */
final class PublishMessageImpl implements PublishMessage {

	private static final Charset UTF8 = Charset.forName("UTF-8");

	private final ChannelManager manager;
	private final MqttChannelRef channel;
	private final PubMessage pubMessage;

	public PublishMessageImpl(ChannelManager manager, MqttChannelRef channel, PubMessage pubMessage) {
		this.manager = manager;
		this.channel = channel;
		this.pubMessage = pubMessage;
	}

	/**
	 * @see net.sf.xenqtt.client.PublishMessage#getTopic()
	 */
	@Override
	public String getTopic() {
		return pubMessage.getTopicName();
	}

	/**
	 * @see net.sf.xenqtt.client.PublishMessage#getPayload()
	 */
	@Override
	public byte[] getPayload() {
		return pubMessage.getPayload();
	}

	/**
	 * @see net.sf.xenqtt.client.PublishMessage#getPayloadString()
	 */
	@Override
	public String getPayloadString() {
		return new String(pubMessage.getPayload(), UTF8);
	}

	/**
	 * @see net.sf.xenqtt.client.PublishMessage#isRetain()
	 */
	@Override
	public boolean isRetain() {
		return pubMessage.isRetain();
	}

	/**
	 * @see net.sf.xenqtt.client.PublishMessage#isDuplicate()
	 */
	@Override
	public boolean isDuplicate() {
		return pubMessage.isDuplicate();
	}

	/**
	 * @see net.sf.xenqtt.client.PublishMessage#getQoS()
	 */
	@Override
	public QoS getQoS() {
		return pubMessage.getQoS();
	}

	/**
	 * @see net.sf.xenqtt.client.PublishMessage#ack()
	 */
	@Override
	public void ack() {

		if (pubMessage.getQoSLevel() > 0) {
			try {
				manager.send(channel, new PubAckMessage(pubMessage.getMessageId()));
			} catch (InterruptedException e) {
				// reset the thread's interrupted flag
				Thread.currentThread().interrupt();
			}
		}
	}
}
