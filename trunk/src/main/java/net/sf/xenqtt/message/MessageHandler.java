/**
    Copyright 2013 James McClure

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package net.sf.xenqtt.message;

/**
 * Handles received {@link MqttMessage}s. There is a method for each message type. This handler will always be called by the IO thread that owns the channel. It
 * is safe to invoke any method on the channel in these methods. Implementations must not block this thread.
 */
public interface MessageHandler {

	/**
	 * Called when a {@link ConnectMessage} is received through the specified channel
	 */
	void connect(MqttChannel channel, ConnectMessage message) throws Exception;

	/**
	 * Called when a {@link ConnAckMessage} is received through the specified channel
	 */
	void connAck(MqttChannel channel, ConnAckMessage message) throws Exception;

	/**
	 * Called when a {@link PubMessage} is received through the specified channel
	 */
	void publish(MqttChannel channel, PubMessage message) throws Exception;

	/**
	 * Called when a {@link PubAckMessage} is received through the specified channel
	 */
	void pubAck(MqttChannel channel, PubAckMessage message) throws Exception;

	/**
	 * Called when a {@link PubRecMessage} is received through the specified channel
	 */
	void pubRec(MqttChannel channel, PubRecMessage message) throws Exception;

	/**
	 * Called when a {@link PubRelMessage} is received through the specified channel
	 */
	void pubRel(MqttChannel channel, PubRelMessage message) throws Exception;

	/**
	 * Called when a {@link PubCompMessage} is received through the specified channel
	 */
	void pubComp(MqttChannel channel, PubCompMessage message) throws Exception;

	/**
	 * Called when a {@link SubscribeMessage} is received through the specified channel
	 */
	void subscribe(MqttChannel channel, SubscribeMessage message) throws Exception;

	/**
	 * Called when a {@link SubAckMessage} is received through the specified channel
	 */
	void subAck(MqttChannel channel, SubAckMessage message) throws Exception;

	/**
	 * Called when a {@link UnsubscribeMessage} is received through the specified channel
	 */
	void unsubscribe(MqttChannel channel, UnsubscribeMessage message) throws Exception;

	/**
	 * Called when a {@link UnsubAckMessage} is received through the specified channel
	 */
	void unsubAck(MqttChannel channel, UnsubAckMessage message) throws Exception;

	/**
	 * Called when a {@link DisconnectMessage} is received through the specified channel
	 */
	void disconnect(MqttChannel channel, DisconnectMessage message) throws Exception;

	/**
	 * Called when a {@link MqttChannel MQTT channel} is opened. Opened is when the connection to the broker or from the client has been established but before
	 * any messages have been sent.
	 * 
	 * @param channel
	 *            The channel that was opened.
	 */
	void channelOpened(MqttChannel channel);

	/**
	 * Called when an {@link MqttChannel MQTT channel} is formally closed.
	 * 
	 * @param channel
	 *            The channel that was closed
	 * @param cause
	 *            The reason the channel was closed. Null if the channel was not closed because of an exception.
	 */
	void channelClosed(MqttChannel channel, Throwable cause);

	/**
	 * Called when an {@link MqttChannel MQTT channel} is {@link ChannelManager#detachChannel(MqttChannelRef) attached} to a {@link ChannelManager channel
	 * manager}.
	 * 
	 * @param channel
	 *            The channel that was attached
	 */
	void channelAttached(MqttChannel channel);

	/**
	 * Called when an {@link MqttChannel MQTT channel} is {@link ChannelManager#detachChannel(MqttChannelRef) detached} from a {@link ChannelManager channel
	 * manager}.
	 * 
	 * @param channel
	 *            The channel that was detached
	 */
	void channelDetached(MqttChannel channel);
}
