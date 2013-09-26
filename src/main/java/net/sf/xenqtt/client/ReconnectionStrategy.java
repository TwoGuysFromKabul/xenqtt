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

/**
 * Implementations are strategies used by {@link MqttClient} implementation to reconnect to the broker if the connection is lost. Implementations must be thread
 * safe.
 */
public interface ReconnectionStrategy extends Cloneable {

	/**
	 * Called by an {@link MqttClient} each time the connection to the broker is lost other than by an intentional disconnect.
	 * 
	 * @param cause
	 *            The exception that cause the connection to close or resulted from the connection closing. May be {@code null}.
	 * 
	 * @return Milliseconds the client should wait before trying to connect to the broker again. If <= 0 the client will stop trying to connect to the broker.
	 */
	long connectionLost(MqttClient client, Throwable cause);

	/**
	 * Called by an {@link MqttClient} instance when a connection to the broker is established.
	 */
	void connectionEstablished();

	/**
	 * @return A new instance of the same type as this object. This is used to create a new strategy for each client created by the {@link MqttClientFactory}.
	 */
	ReconnectionStrategy clone();
}
