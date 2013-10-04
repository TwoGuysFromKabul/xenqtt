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
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.xenqtt.Log;
import net.sf.xenqtt.client.AsyncMqttClient;
import net.sf.xenqtt.client.SynchronousMqttClient;
import net.sf.xenqtt.message.QoS;

/**
 * A {@link Runnable} implementation that handles the work of publishing to an MQTT broker.
 */
final class PublishWorker implements Runnable {

	private final String name;
	private final boolean async;
	private final String publishTopic;
	private final int messageSize;
	private final AtomicInteger messageIds;
	private final QoS qos;
	private final CountDownLatch latch;
	private final Publisher publisher;
	private final Semaphore inFlight;

	/**
	 * Create a new instance of this class.
	 * 
	 * @param name
	 *            The name to assign to this {@link PublishWorker worker}
	 * @param async
	 *            Whether or not the {@link AsyncMqttClient asynchronous} or the {@link SynchronousMqttClient synchronous} MQTT client is being used
	 * @param publishTopic
	 *            The topic to publish to
	 * @param messageSize
	 *            The size of the message that will follow the message ID and timestamp in all messages published by this test client
	 * @param messageIds
	 *            Produces IDs for each message sent by this and every other {@link PublishWorker worker}
	 * @param qos
	 *            The QoS level at which data is being published
	 * @param latch
	 *            A {@link CountDownLatch latch} that is triggered following the publish of each message, regardless of whether or not the publish succeeded
	 * @param stats
	 *            The {@link XenqttTestClientStats stats} instance being used in this test
	 * @param publisher
	 *            The {@link Publisher publisher} to use in actually publishing messages to the broker
	 * @param inFlight
	 *            The maximum number of in-flight messages allowed across all publish workers
	 */
	PublishWorker(String name, boolean async, String publishTopic, int messageSize, AtomicInteger messageIds, QoS qos, CountDownLatch latch,
			XenqttTestClientStats stats, Publisher publisher, Semaphore inFlight) {
		this.name = name;
		this.async = async;
		this.publishTopic = publishTopic;
		this.messageSize = messageSize;
		this.messageIds = messageIds;
		this.qos = qos;
		this.latch = latch;
		this.publisher = publisher;
		this.inFlight = inFlight;
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		for (;;) {
			try {
				inFlight.acquire();
				long messagesRemaining = publisher.publish(publishTopic, qos, createPayload());
				if (!async || qos == QoS.AT_MOST_ONCE) {
					inFlight.release();
				}
				if (messagesRemaining == 0) {
					break;
				}

				if (messagesRemaining % 1000 == 0) {
					Log.debug("Publisher %s has %d messages remaining.", name, messagesRemaining);
				}
			} catch (Exception ex) {
				Log.error(ex, "Unable to publish a message");
			} finally {
				if (!async || qos == QoS.AT_MOST_ONCE) {
					latch.countDown();
				}
			}
		}
	}

	private byte[] createPayload() {
		int size = 12 + messageSize;
		byte[] payload = new byte[size];

		long now = System.currentTimeMillis();
		payload[0] = (byte) ((now & 0xff00000000000000L) >> 56);
		payload[1] = (byte) ((now & 0x00ff000000000000L) >> 48);
		payload[2] = (byte) ((now & 0x0000ff0000000000L) >> 40);
		payload[3] = (byte) ((now & 0x000000ff00000000L) >> 32);
		payload[4] = (byte) ((now & 0x00000000ff000000L) >> 24);
		payload[5] = (byte) ((now & 0x0000000000ff0000L) >> 16);
		payload[6] = (byte) ((now & 0x000000000000ff00L) >> 8);
		payload[7] = (byte) (now & 0x00000000000000ffL);

		int id = messageIds.getAndIncrement();
		payload[8] = (byte) ((id & 0xff000000) >> 24);
		payload[9] = (byte) ((id & 0x00ff0000) >> 16);
		payload[10] = (byte) ((id & 0x0000ff00) >> 8);
		payload[11] = (byte) (id & 0x000000ff);

		for (int i = 12; i < size; i++) {
			payload[i] = (byte) ((i - 12) & 0xff);
		}

		return payload;
	}
}
