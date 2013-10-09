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
package net.sf.xenqtt.test;

import java.util.concurrent.CountDownLatch;

import net.sf.xenqtt.Log;
import net.sf.xenqtt.message.ConnectReturnCode;

/**
 * Controls when a load test orchestrated via the {@link XenqttTestClient Xenqtt test client} is permitted to advance through its disparate stages.
 */
final class StageControl {

	private final CountDownLatch connectedLatch;
	private final CountDownLatch publishLatch;
	private final CountDownLatch receiveLatch;
	private final CountDownLatch durationLatch;

	StageControl(boolean awaitConnect, int messagesToPublish, int messagesToReceive) {
		connectedLatch = new CountDownLatch(awaitConnect ? 1 : 0);
		publishLatch = new CountDownLatch(messagesToPublish);
		receiveLatch = new CountDownLatch(messagesToReceive);
		durationLatch = new CountDownLatch(0);
	}

	StageControl(boolean awaitConnect, final long testDuration) {
		connectedLatch = new CountDownLatch(awaitConnect ? 1 : 0);
		publishLatch = new CountDownLatch(0);
		receiveLatch = new CountDownLatch(0);
		durationLatch = new CountDownLatch(1);

		new Thread() {

			@Override
			public void run() {
				try {
					Thread.sleep(testDuration);
				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
					Log.error(ex, "Waiting for the test to run to completion was interrupted. Exiting the test now.");
				} finally {
					durationLatch.countDown();
				}
			}

		}.start();
	}

	void connected(ConnectReturnCode result) {
		if (result != ConnectReturnCode.ACCEPTED) {
			Log.error("The connection to the broker was rejected. Reason: %s", result);
			System.err.println("The broker refused the connection. This error is not recoverable.");
			System.exit(-1);
		}

		connectedLatch.countDown();
	}

	void awaitConnect() {
		try {
			connectedLatch.await();
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			Log.error(ex, "Unable to complete the connect cadence.");
			System.exit(-1);
		}
	}

	void messagePublished() {
		publishLatch.countDown();
	}

	void messageReceived() {
		receiveLatch.countDown();
	}

	void awaitTestCompletion() {
		try {
			durationLatch.await();
			publishLatch.await();
			receiveLatch.await();
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			Log.error(ex, "Unable to complete the shutdown await cadence.");
		}
	}

}
