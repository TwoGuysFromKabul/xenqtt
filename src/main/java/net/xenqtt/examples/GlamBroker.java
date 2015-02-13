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
package net.xenqtt.examples;

import net.xenqtt.mockbroker.MockBroker;
import net.xenqtt.mockbroker.MockBrokerHandler;

/**
 * Fires up a mock broker that specializes in routing data of the 'Glam' variety.
 */
public class GlamBroker {

	public static void main(String... args) throws InterruptedException {
		MockBrokerHandler handler = new MockBrokerHandler();
		MockBroker broker = new MockBroker(handler);

		broker.init(); // Blocks until startup is complete.

		// At this point the broker is online. Clients can connect to it, publish messages, subscribe, etc.
		Thread.sleep(60000);

		// We are done. Shutdown the broker. Wait forever (> 0 means wait that many milliseconds).
		broker.shutdown(0);
	}

}
