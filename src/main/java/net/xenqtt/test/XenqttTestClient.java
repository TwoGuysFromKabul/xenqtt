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
package net.xenqtt.test;

import net.xenqtt.AppContext;
import net.xenqtt.Log;
import net.xenqtt.client.AsyncMqttClient;
import net.xenqtt.client.SyncMqttClient;

/**
 * A test client that facilitates load and validation testing of the Xenqtt MQTT client.
 */
public final class XenqttTestClient {

	private final TestClientRunner runner;

	/**
	 * Create a new instance of this class.
	 * 
	 * @param context
	 *            The {@link AppContext context} that were passed in on the command-line
	 */
	public XenqttTestClient(AppContext context) {
		runner = new TestClientRunner(context);
	}

	/**
	 * Start the test client.
	 */
	public void start() {
		runner.start();
	}

	/**
	 * Stop the test client.
	 */
	public void stop() {
		runner.interrupt();
		try {
			runner.join(3000);
		} catch (Exception ex) {
			Log.error(ex, "Unable to wait for the test client runner to shutdown.");
		}

		if (runner.isAlive()) {
			Log.warn("Unable to cleanly shutdown the test client runner.");
		}
	}

	/**
	 * An enumeration that identifies the disparate MQTT client types available during the test. The desired client type is chosen via the configuration file
	 * and is a required property.
	 */
	static enum ClientType {

		/**
		 * Specifies usage of the {@link SyncMqttClient synchronous} MQTT client.
		 */
		SYNC("sync"),

		/**
		 * Specifies usage of the {@link AsyncMqttClient asynchronous} MQTT client.
		 */
		ASYNC("async");

		private final String type;

		private ClientType(String type) {
			this.type = type;
		}

		/**
		 * @return A textual representation of this {@link ClientType}
		 */
		String getType() {
			return type;
		}

		/**
		 * Get a {@link ClientType} instance based on a given textual representation.
		 * 
		 * @param type
		 *            The desired type
		 * 
		 * @return The {@link ClientType} that corresponds to the specified {@code type}
		 * 
		 * @throws IllegalArgumentException
		 *             If the specified {@code type} does not correspond to a known client type
		 */
		static ClientType getClientType(String type) {
			if (type == null) {
				throw new IllegalArgumentException("The client type cannot be null.");
			}

			for (ClientType clientType : values()) {
				if (clientType.type.equalsIgnoreCase(type)) {
					return clientType;
				}
			}

			throw new IllegalArgumentException(String.format("Unrecognized client type: %s", type));
		}

	}

}
