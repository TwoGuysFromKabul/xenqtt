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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import net.sf.xenqtt.client.PublishMessage;
import net.sf.xenqtt.test.XenqttTestClient.ClientType;

/**
 * Aggregates and computes statistical information related to an execution of the Xenqtt {@link XenqttTestClient test client}.
 */
final class XenqttTestClientStats {

	private final ClientType clientType;

	private volatile long testStart;
	private volatile long testEnd;

	private final Lock publishLock = new ReentrantLock();

	// Fields synchronized by publishLock.
	private long messagesPublished;
	private long intervalMessagesPublished;
	private double publishDuration;
	private List<Integer> publishedMessageIds;
	private long lastPublishThroughputSnapshotTime;

	private final Lock subscribeLock = new ReentrantLock();

	// Fields synchronized by subscribeLock
	private long messagesReceived;
	private long intervalMessagesReceived;
	private long duplicateMessagesReceived;
	private double messageLatency;
	private List<Integer> receivedMessageIds;
	private long lastReceivedThroughputSnapshotTime;

	private final DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

	XenqttTestClientStats(ClientType clientType) {
		this.clientType = clientType;

		Lock lock = publishLock;
		lock.lock();
		try {
			publishedMessageIds = new ArrayList<Integer>();
		} finally {
			lock.unlock();
		}

		lock = subscribeLock;
		lock.lock();
		try {
			receivedMessageIds = new ArrayList<Integer>();
		} finally {
			lock.unlock();
		}
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
			intervalMessagesPublished++;
			publishDuration += (System.currentTimeMillis() - then);
			publishedMessageIds.add(getMessageId(message.getPayload()));
		} finally {
			lock.unlock();
		}
	}

	void messageReceived(PublishMessage message) {
		Lock lock = subscribeLock;
		lock.lock();
		try {
			messagesReceived++;
			intervalMessagesReceived++;
			if (message.isDuplicate()) {
				duplicateMessagesReceived++;
				return;
			}

			long originalPublishTime = getOriginalPublishTime(message);
			long latency = System.currentTimeMillis() - originalPublishTime;
			messageLatency += latency;
			receivedMessageIds.add(getMessageId(message.getPayload()));
		} finally {
			lock.unlock();
		}
	}

	private long getOriginalPublishTime(PublishMessage message) {
		byte[] payload = message.getPayload();
		long originalPublishTime = 0L;
		if (payload.length >= 12) {
			originalPublishTime |= (((long) payload[0] & 0xff) << 56);
			originalPublishTime |= (((long) payload[1] & 0xff) << 48);
			originalPublishTime |= (((long) payload[2] & 0xff) << 40);
			originalPublishTime |= (((long) payload[3] & 0xff) << 32);
			originalPublishTime |= (((long) payload[4] & 0xff) << 24);
			originalPublishTime |= (((long) payload[5] & 0xff) << 16);
			originalPublishTime |= (((long) payload[6] & 0xff) << 8);
			originalPublishTime |= ((long) payload[7] & 0xff);
		}

		return originalPublishTime;
	}

	private Integer getMessageId(byte[] payload) {
		if (payload.length < 12) {
			return Integer.valueOf(-1);
		}

		int messageId = 0;
		messageId |= (payload[8] & 0xff) << 24;
		messageId |= (payload[9] & 0xff) << 16;
		messageId |= (payload[10] & 0xff) << 8;
		messageId |= payload[11] & 0xff;

		return Integer.valueOf(messageId);
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
		long now = System.currentTimeMillis();
		Lock lock = publishLock;
		lock.lock();
		try {
			if (lastPublishThroughputSnapshotTime == 0) {
				lastPublishThroughputSnapshotTime = testStart;
			}

			double throughput = ((double) intervalMessagesPublished) / (now - lastPublishThroughputSnapshotTime) * 1000.0D;
			lastPublishThroughputSnapshotTime = now;
			intervalMessagesPublished = 0;

			return throughput;
		} finally {
			lock.unlock();
		}
	}

	double getMessagesReceivedThroughput() {
		long now = System.currentTimeMillis();
		Lock lock = subscribeLock;
		lock.lock();
		try {
			if (lastReceivedThroughputSnapshotTime == 0) {
				lastReceivedThroughputSnapshotTime = testStart;
			}

			double throughput = ((double) intervalMessagesReceived) / (now - lastReceivedThroughputSnapshotTime) * 1000.0D;
			lastReceivedThroughputSnapshotTime = now;
			intervalMessagesReceived = 0;

			return throughput;
		} finally {
			lock.unlock();
		}
	}

	List<Gap> getPublishMessageGaps() {
		Lock lock = publishLock;
		lock.lock();
		try {
			return findGaps(publishedMessageIds);
		} finally {
			lock.unlock();
		}
	}

	List<Gap> getReceivedMessageGaps() {
		Lock lock = subscribeLock;
		lock.lock();
		try {
			return findGaps(receivedMessageIds);
		} finally {
			lock.unlock();
		}
	}

	private List<Gap> findGaps(List<Integer> ids) {
		Collections.sort(ids);
		List<Gap> gaps = new ArrayList<Gap>();
		int previous = -1;
		for (Integer id : ids) {
			int value = id.intValue();
			if (previous != -1 && previous + 1 != value) {
				gaps.add(new Gap(previous + 1, value - 1));
			}

			previous = value;
		}

		return gaps;
	}

	static final class Gap {

		final int start;
		final int end;

		Gap(int start, int end) {
			this.start = start;
			this.end = end;
		}

		@Override
		public String toString() {
			if (start == end) {
				return String.valueOf(start);
			}

			return String.format("%d - %d", start, end);
		}

	}

}
