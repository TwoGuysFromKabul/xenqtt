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
package net.xenqtt.client;

/**
 * Reconnect strategy that does 0 reconnect attempts.
 */
public final class NullReconnectStrategy implements ReconnectionStrategy {

	/**
	 * @see net.xenqtt.client.ReconnectionStrategy#connectionLost(net.xenqtt.client.MqttClient, java.lang.Throwable)
	 */
	@Override
	public long connectionLost(MqttClient client, Throwable cause) {

		return -1;
	}

	/**
	 * @see net.xenqtt.client.ReconnectionStrategy#connectionEstablished()
	 */
	@Override
	public void connectionEstablished() {
	}

	/**
	 * @see java.lang.Object#clone()
	 * @see ReconnectionStrategy#clone()
	 */
	@Override
	public ReconnectionStrategy clone() {
		return this;
	}
}
