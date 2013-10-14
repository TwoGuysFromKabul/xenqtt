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

import net.sf.xenqtt.client.MessageStats;

/**
 * Extends {@link MessageStats} to add methods used to update the stats
 */
interface MutableMessageStats extends MessageStats, Cloneable {

	/**
	 * Called when a message is successfully sent.
	 */
	void messageSent(boolean resent);

	/**
	 * Called when a message has been ACKed by the broker.
	 */
	void messageAcked(long ackLatency);

	/**
	 * Called when a message has been received. The {@link MqttMessage#isDuplicate()} must be correctly set when this is called.
	 */
	void messageReceived(boolean duplicate);

	/**
	 * Resets all applicable stats
	 */
	void reset();

}
