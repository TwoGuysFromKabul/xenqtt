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
package net.sf.xenqtt.client;

import net.sf.xenqtt.message.MqttChannel;
import net.sf.xenqtt.message.MqttMessage;

/**
 * Extends {@link MqttClientStats} to add methods used to update the stats
 */
interface MutableMqttClientStats extends MqttClientStats, Cloneable {

	/**
	 * Registers the channel as one of the channels used to create these stats
	 */
	void registerChannel(MqttChannel channel);

	/**
	 * Deregisters the channel as one of the channels used to create these stats
	 */
	void deregisterChannel(MqttChannel channel);

	/**
	 * Called when the message is queued for sending.
	 */
	void messageQueued(boolean duplicate);

	/**
	 * Called when a message is successfully sent.
	 */
	void messageSent(boolean resent, long millisInQueue);

	/**
	 * Called when a message has been received. The {@link MqttMessage#isDuplicate()} must be correctly set when this is called.
	 */
	void messageReceived(boolean duplicate);

	/**
	 * Called immediately before the {@link AsyncClientListener} or {@link MqttClientListener} callback is invoked.
	 */
	void messageProcessing(long millisInQueue);

	/**
	 * Resets all applicable stats
	 */
	void reset();

	/**
	 * Clones this object for a snapshot
	 */
	MqttClientStats clone();
}
