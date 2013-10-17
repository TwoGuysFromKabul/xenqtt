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
package net.sf.xenqtt;

import net.sf.xenqtt.client.MqttClient;
import net.sf.xenqtt.client.Subscription;
import net.sf.xenqtt.message.QoS;

/**
 * Thrown by synchronous {@link MqttClient#subscribe(java.util.List)} and {@link MqttClient#subscribe(Subscription[])} methods when the {@link QoS} granted for
 * a subscription request does not match the QoS requested.
 */
public final class MqttQosNotGrantedException extends MqttException {

	private static final long serialVersionUID = 1L;

	private final Subscription[] grantedSubscriptions;

	/**
	 * Create a new instance of this class.
	 * 
	 * @param grantedSubscriptions
	 *            The subscriptions for which the {@link QoS QOS} was not granted
	 */
	public MqttQosNotGrantedException(Subscription[] grantedSubscriptions) {
		this.grantedSubscriptions = grantedSubscriptions;
	}

	/**
	 * @return The topics subscribed to and the QoS granted for each
	 */
	public Subscription[] getGrantedSubscriptions() {
		return grantedSubscriptions;
	}
}
