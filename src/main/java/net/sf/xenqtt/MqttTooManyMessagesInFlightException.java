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

import net.sf.xenqtt.client.AsyncMqttClient;
import net.sf.xenqtt.client.MqttClientConfig;

/**
 * Thrown when an {@link AsyncMqttClient} tries to send a message when {@link MqttClientConfig#getMaxInFlightMessages()} messages are already in-flight.
 */
public class MqttTooManyMessagesInFlightException extends MqttException {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a new mqtt exception with <code>null</code> as its detail message.
	 */
	public MqttTooManyMessagesInFlightException() {
	}

	/**
	 * Constructs a new mqtt exception with the specified detail message.
	 * 
	 * @param message
	 *            the detail message. The detail message is saved for later retrieval by the {@link #getMessage()} method.
	 */
	public MqttTooManyMessagesInFlightException(String message) {
		super(message);
	}
}
