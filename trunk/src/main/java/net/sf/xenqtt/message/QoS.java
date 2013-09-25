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
package net.sf.xenqtt.message;

/**
 * QoS is the quality of service which determines the assurance of message delivery. The order in this enum is important as the ordinal is used as the numeric
 * value of the QoS in the message. QOS 2, exactly once delivery, is not supported.
 */
public enum QoS {

	AT_MOST_ONCE, // Fire and Forget
	AT_LEAST_ONCE, // Acknowledged delivery
	EXACTLY_ONCE // Assured delivery
	;

	/**
	 * @return The {@link QoS} for the specified numeric value.
	 */
	public static QoS lookup(int value) {
		return values()[value];
	}

	/**
	 * @return The numeric value for this {@link Qos}
	 */
	public int value() {
		return ordinal();
	}
}
