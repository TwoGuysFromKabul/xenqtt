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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import net.sf.xenqtt.client.PublishMessage;
import net.sf.xenqtt.test.XenqttTestClient.ClientType;

/**
 * TODO [jeremy] - Document this type.
 */
final class XenqttTestClientStats {

	private final ClientType clientType;

	private volatile long testStart;
	private volatile long testEnd;

	private final Lock publishLock = new ReentrantLock();

	// Fields synchronized by publishLock.
	private long messagesPublished;
	private double publishDuration;

	private final Lock subscribeLock = new ReentrantLock();

	// Fields synchronized by subscribeLock
	private long messagesReceived;
	private long duplicateMessagesReceived;
	private double messageLatency;

	private final DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

	XenqttTestClientStats(ClientType clientType) {
		this.clientType = clientType;
	}

	void testStarted() {
		testStart = System.currentTimeMillis();
	}

	void testEnded() {
		testEnd = System.currentTimeMillis();
	}

	void publishComplete(PublishMessage message) {
		Lock lock = publishLock;
		lock.lock();
		try {
			long then = getOriginalPublishTime(message);
			messagesPublished++;
			publishDuration += (System.currentTimeMillis() - then);
		} finally {
			lock.unlock();
		}
	}

	void messageReceived(PublishMessage message) {
		Lock lock = subscribeLock;
		lock.lock();
		try {
			messagesReceived++;
			if (message.isDuplicate()) {
				duplicateMessagesReceived++;
				return;
			}

			long originalPublishTime = getOriginalPublishTime(message);
			long latency = System.currentTimeMillis() - originalPublishTime;
			messageLatency += latency;
		} finally {
			lock.unlock();
		}
	}

	private long getOriginalPublishTime(PublishMessage message) {
		byte[] payload = message.getPayload();
		long originalPublishTime = 0L;
		if (payload.length == 8) {
			originalPublishTime |= (((long) payload[7] & 0xff) << 56);
			originalPublishTime |= (((long) payload[6] & 0xff) << 48);
			originalPublishTime |= (((long) payload[5] & 0xff) << 40);
			originalPublishTime |= (((long) payload[4] & 0xff) << 32);
			originalPublishTime |= (((long) payload[3] & 0xff) << 24);
			originalPublishTime |= (((long) payload[2] & 0xff) << 16);
			originalPublishTime |= (((long) payload[1] & 0xff) << 8);
			originalPublishTime |= ((long) payload[0] & 0xff);
		}

		return originalPublishTime;
	}

	String getClientType() {
		return clientType.getType();
	}

	String getTestStart() {
		return format.format(new Date(testStart));
	}

	String getTestEnd() {
		return format.format(new Date(testEnd));
	}

	double getTestDurationSeconds() {
		double durationMillis = testEnd - testStart;

		return durationMillis / 1000.0D;
	}

	long getNumMessagesPublished() {
		Lock lock = publishLock;
		lock.lock();
		try {
			return messagesPublished;
		} finally {
			lock.unlock();
		}
	}

	double getAveragePublishDuration() {
		Lock lock = publishLock;
		lock.lock();
		try {
			return publishDuration / messagesPublished;
		} finally {
			lock.unlock();
		}
	}

	long getMessagesReceived() {
		Lock lock = subscribeLock;
		lock.lock();
		try {
			return messagesReceived;
		} finally {
			lock.unlock();
		}
	}

	double getAverageMessageLatency() {
		Lock lock = subscribeLock;
		lock.lock();
		try {
			return messageLatency / messagesReceived;
		} finally {
			lock.unlock();
		}
	}

	long getDuplicates() {
		Lock lock = subscribeLock;
		lock.lock();
		try {
			return duplicateMessagesReceived;
		} finally {
			lock.unlock();
		}
	}

	double getPublishThroughput() {
		double now = System.currentTimeMillis();
		Lock lock = publishLock;
		lock.lock();
		try {
			return messagesPublished / (now - testStart) * 1000.0D;
		} finally {
			lock.unlock();
		}
	}

	double getMessagesReceivedThroughput() {
		double now = System.currentTimeMillis();
		Lock lock = subscribeLock;
		lock.lock();
		try {
			return messagesReceived / (now - testStart) * 1000.0D;
		} finally {
			lock.unlock();
		}
	}

}
