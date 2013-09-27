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

import static org.junit.Assert.*;
import net.sf.xenqtt.message.QoS;

import org.junit.Test;

public class SubscriptionTest {

	Subscription subscription = new Subscription("grand/foo/bar", QoS.AT_MOST_ONCE);

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_NullTopic() {
		new Subscription(null, QoS.AT_MOST_ONCE);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCtor_NullQoS() {
		new Subscription("grand/foo/bar", null);
	}

	@Test
	public void testGetQos() {
		assertSame(QoS.AT_MOST_ONCE, subscription.getQos());
	}

	@Test
	public void testGetTopic() {
		assertEquals("grand/foo/bar", subscription.getTopic());
	}

	@Test
	public void testEquals() {
		Subscription sub = new Subscription("grand/foo/bar", QoS.AT_MOST_ONCE);
		assertEquals(sub, subscription);

		sub = new Subscription("grand/foo/bar", QoS.AT_LEAST_ONCE);
		assertFalse(subscription.equals(sub));

		sub = new Subscription("grand/bar/foo", QoS.AT_MOST_ONCE);
		assertFalse(subscription.equals(sub));
	}

	@Test
	public void testHashCode() {
		Subscription sub = new Subscription("grand/foo/bar", QoS.AT_MOST_ONCE);
		assertEquals(sub.hashCode(), subscription.hashCode());

		sub = new Subscription("grand/foo/bar", QoS.AT_LEAST_ONCE);
		assertFalse(subscription.hashCode() == sub.hashCode());

		sub = new Subscription("grand/bar/foo", QoS.AT_MOST_ONCE);
		assertFalse(subscription.hashCode() == sub.hashCode());
	}

	@Test
	public void testToString() {
		assertEquals("Subscription [topic=grand/foo/bar, qos=AT_MOST_ONCE]", subscription.toString());
	}

}
