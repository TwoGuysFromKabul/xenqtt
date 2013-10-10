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
 * Base interface for {@link SyncClientFactory} and {@link AsyncClientFactory}.
 */
public interface ClientFactory {

	/**
	 * Stops this factory. Closes all open connections to the broker. Blocks until shutdown is complete. Any other methods called after this have unpredictable
	 * results.
	 */
	void shutdown();

	/**
	 * @param reset
	 *            If true the stats will be reset to 0 (where applicable) so the next time they are retrieved they will be for the time between the 2 calls to
	 *            this method.
	 * 
	 * @return A snapshot of the statistics for all the clients created by this factory.
	 */
	MqttClientStats getStats(boolean reset);
}
