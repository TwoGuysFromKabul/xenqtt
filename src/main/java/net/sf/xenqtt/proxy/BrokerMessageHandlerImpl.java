package net.sf.xenqtt.proxy;

/**
 * Handles messages from the proxy's broker connection
 */
final class BrokerMessageHandlerImpl {
	// FIXME [jim] - implement proxy
	// final class BrokerMessageHandlerImpl implements BrokerMessageHandler {
	//
	// private final List<MqttChannel> clientChannels;
	// private final Map<Integer, ChannelAndId> clientChannelsByMessageId;
	//
	// private int nextClientIndex;
	//
	// public BrokerMessageHandlerImpl(List<MqttChannel> clientChannels, Map<Integer, ChannelAndId> clientChannelsByMessageId) {
	// this.clientChannels = clientChannels;
	// this.clientChannelsByMessageId = clientChannelsByMessageId;
	// }
	//
	// /**
	// * @see net.sf.xenqtt.proxy.BrokerMessageHandler#newClientChannel(net.sf.xenqtt.message.MqttChannel)
	// */
	// @Override
	// public void newClientChannel(MqttChannel clientChannel) {
	// // TODO Auto-generated method stub
	//
	// }
	//
	// /**
	// * @see net.sf.xenqtt.proxy.BrokerMessageHandler#closeClientChannel(net.sf.xenqtt.message.MqttChannel)
	// */
	// @Override
	// public void closeClientChannel(MqttChannel channel) {
	//
	// channel.close();
	//
	// Iterator<MqttChannel> iter1 = clientChannels.iterator();
	// while (iter1.hasNext()) {
	// if (iter1.next() == channel) {
	// iter1.remove();
	// break;
	// }
	// }
	//
	// Iterator<ChannelAndId> iter2 = clientChannelsByMessageId.values().iterator();
	// while (iter2.hasNext()) {
	// if (iter2.next().channel == channel) {
	// iter2.remove();
	// }
	// }
	// }
	//
	// /**
	// * @see net.sf.xenqtt.message.MessageHandler#handle(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.ConnectMessage)
	// */
	// @Override
	// public void handle(MqttChannel channel, ConnectMessage message) throws Exception {
	// // should never be received from the broker
	// }
	//
	// /**
	// * @see net.sf.xenqtt.message.MessageHandler#handle(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.ConnAckMessage)
	// */
	// @Override
	// public void handle(MqttChannel channel, ConnAckMessage message) throws Exception {
	// // FIXME [jim] - need to deal with first conn ack, clean up if rejected, and not respond to new ones until first is acked.
	// }
	//
	// /**
	// * @see net.sf.xenqtt.message.MessageHandler#handle(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.PublishMessage)
	// */
	// @Override
	// public void handle(MqttChannel channel, PublishMessage message) throws Exception {
	// distributeToClient(message);
	// }
	//
	// /**
	// * @see net.sf.xenqtt.message.MessageHandler#handle(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.PubAckMessage)
	// */
	// @Override
	// public void handle(MqttChannel channel, PubAckMessage message) throws Exception {
	// forwardClientAck(message);
	// }
	//
	// /**
	// * @see net.sf.xenqtt.message.MessageHandler#handle(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.PubRecMessage)
	// */
	// @Override
	// public void handle(MqttChannel channel, PubRecMessage message) throws Exception {
	// forwardClientAck(message);
	// }
	//
	// /**
	// * @see net.sf.xenqtt.message.MessageHandler#handle(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.PubRelMessage)
	// */
	// @Override
	// public void handle(MqttChannel channel, PubRelMessage message) throws Exception {
	// distributeToClient(message);
	// }
	//
	// /**
	// * @see net.sf.xenqtt.message.MessageHandler#handle(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.PubCompMessage)
	// */
	// @Override
	// public void handle(MqttChannel channel, PubCompMessage message) throws Exception {
	// distributeToClient(message);
	// }
	//
	// /**
	// * @see net.sf.xenqtt.message.MessageHandler#handle(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.SubscribeMessage)
	// */
	// @Override
	// public void handle(MqttChannel channel, SubscribeMessage message) throws Exception {
	// // should never be received from broker
	// }
	//
	// /**
	// * @see net.sf.xenqtt.message.MessageHandler#handle(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.SubAckMessage)
	// */
	// @Override
	// public void handle(MqttChannel channel, SubAckMessage message) throws Exception {
	// forwardClientAck(message);
	// }
	//
	// /**
	// * @see net.sf.xenqtt.message.MessageHandler#handle(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.UnsubscribeMessage)
	// */
	// @Override
	// public void handle(MqttChannel channel, UnsubscribeMessage message) throws Exception {
	// // should never be received from broker
	// }
	//
	// /**
	// * @see net.sf.xenqtt.message.MessageHandler#handle(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.UnsubAckMessage)
	// */
	// @Override
	// public void handle(MqttChannel channel, UnsubAckMessage message) throws Exception {
	// forwardClientAck(message);
	// }
	//
	// /**
	// * @see net.sf.xenqtt.message.MessageHandler#handle(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.PingReqMessage)
	// */
	// @Override
	// public void handle(MqttChannel channel, PingReqMessage message) throws Exception {
	// // should never be received from broker
	// }
	//
	// /**
	// * @see net.sf.xenqtt.message.MessageHandler#handle(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.PingRespMessage)
	// */
	// @Override
	// public void handle(MqttChannel channel, PingRespMessage message) throws Exception {
	// // FIXME [jim] - need to deal with pings
	// }
	//
	// /**
	// * @see net.sf.xenqtt.message.MessageHandler#channelClosed(net.sf.xenqtt.message.MqttChannel)
	// */
	// @Override
	// public void channelClosed(MqttChannel channel) {
	// // TODO [jeremy] - Implement this method.
	// }
	//
	// /**
	// * @see net.sf.xenqtt.message.MessageHandler#handle(net.sf.xenqtt.message.MqttChannel, net.sf.xenqtt.message.DisconnectMessage)
	// */
	// @Override
	// public void handle(MqttChannel channel, DisconnectMessage message) throws Exception {
	// // should never be received from broker
	// }
	//
	// private void distributeToClient(MqttMessage message) throws IOException {
	//
	// if (clientChannels.isEmpty()) {
	// // FIXME [jim] - log something here??
	// return;
	// }
	//
	// if (nextClientIndex >= clientChannels.size()) {
	// nextClientIndex = 0;
	// }
	// MqttChannel channel = clientChannels.get(nextClientIndex++);
	// channel.send(message);
	// }
	//
	// private void forwardClientAck(IdentifiableMqttMessage message) throws IOException {
	//
	// ChannelAndId channelAndId = clientChannelsByMessageId.remove(message.getMessageId());
	// if (channelAndId == null) {
	// // FIXME [jim] - log something??
	// return;
	// }
	//
	// message.setMessageId(channelAndId.clientSideMessageId);
	// channelAndId.channel.send(message);
	// }
}
