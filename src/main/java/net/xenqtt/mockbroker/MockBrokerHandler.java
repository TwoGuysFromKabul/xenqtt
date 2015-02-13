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
package net.xenqtt.mockbroker;

import net.xenqtt.message.ConnectMessage;
import net.xenqtt.message.DisconnectMessage;
import net.xenqtt.message.MqttMessage;
import net.xenqtt.message.PubAckMessage;
import net.xenqtt.message.PubCompMessage;
import net.xenqtt.message.PubMessage;
import net.xenqtt.message.PubRecMessage;
import net.xenqtt.message.PubRelMessage;
import net.xenqtt.message.SubscribeMessage;
import net.xenqtt.message.UnsubAckMessage;
import net.xenqtt.message.UnsubscribeMessage;

/**
 * This class does nothing by default. You can extend it to add functionality to the {@link MockMqttBroker}, mock it with Mockito or another mocking framework,
 * etc. If the method returns false then the default behavior for that event is executed. If the method returns true it is not.
 */
public class MockBrokerHandler {

	/**
	 * @see net.xenqtt.message.MessageHandler#connect(net.xenqtt.message.MqttChannel, net.xenqtt.message.ConnectMessage)
	 */
	public boolean connect(Client client, ConnectMessage message) throws Exception {
		return false;
	}

	/**
	 * @see net.xenqtt.message.MessageHandler#publish(net.xenqtt.message.MqttChannel, net.xenqtt.message.PubMessage)
	 */
	public boolean publish(Client client, PubMessage message) throws Exception {
		return false;
	}

	/**
	 * @see net.xenqtt.message.MessageHandler#pubAck(net.xenqtt.message.MqttChannel, net.xenqtt.message.PubAckMessage)
	 */
	public boolean pubAck(Client client, PubAckMessage message) throws Exception {
		return false;
	}

	/**
	 * @see net.xenqtt.message.MessageHandler#pubRec(net.xenqtt.message.MqttChannel, net.xenqtt.message.PubRecMessage)
	 */
	public boolean pubRec(Client client, PubRecMessage message) throws Exception {
		return false;
	}

	/**
	 * @see net.xenqtt.message.MessageHandler#pubRel(net.xenqtt.message.MqttChannel, net.xenqtt.message.PubRelMessage)
	 */
	public boolean pubRel(Client client, PubRelMessage message) throws Exception {
		return false;
	}

	/**
	 * @see net.xenqtt.message.MessageHandler#pubComp(net.xenqtt.message.MqttChannel, net.xenqtt.message.PubCompMessage)
	 */
	public boolean pubComp(Client client, PubCompMessage message) throws Exception {
		return false;
	}

	/**
	 * @see net.xenqtt.message.MessageHandler#subscribe(net.xenqtt.message.MqttChannel, net.xenqtt.message.SubscribeMessage)
	 */
	public boolean subscribe(Client client, SubscribeMessage message) throws Exception {
		return false;
	}

	/**
	 * @see net.xenqtt.message.MessageHandler#unsubscribe(net.xenqtt.message.MqttChannel, net.xenqtt.message.UnsubscribeMessage)
	 */
	public boolean unsubscribe(Client client, UnsubscribeMessage message) throws Exception {
		return false;
	}

	/**
	 * @see net.xenqtt.message.MessageHandler#disconnect(net.xenqtt.message.MqttChannel, net.xenqtt.message.DisconnectMessage)
	 */
	public void disconnect(Client client, DisconnectMessage message) throws Exception {
	}

	/**
	 * @see net.xenqtt.message.MessageHandler#channelOpened(net.xenqtt.message.MqttChannel)
	 */
	public void channelOpened(Client client) {
	}

	/**
	 * @see net.xenqtt.message.MessageHandler#channelClosed(net.xenqtt.message.MqttChannel, java.lang.Throwable)
	 */
	public void channelClosed(Client client, Throwable cause) {
	}

	/**
	 * Called when an unexpected message is received. This includes things like receiving an {@link UnsubAckMessage} which should never be received by a broker
	 * or receiving any message other than {@link ConnectMessage} as the first message from a client.
	 */
	public void unexpectedMessage(Client client, MqttMessage message) {

	}
}
