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
 * The code returned in the {@link ConnAckMessage}. The order is important as the ordinal is the numeric value used in messages.
 */
public enum ConnectReturnCode {
	/**
	 * Connection Accepted (success)
	 */
	ACCEPTED,
	/**
	 * Connection Refused: unacceptable protocol version
	 */
	UNACCEPTABLE_PROTOCOL_VERSION,
	/**
	 * Connection Refused: identifier rejected. Sent if the unique client identifier is not between 1 and 23 characters in length.
	 */
	IDENTIFIER_REJECTED,
	/**
	 * Connection Refused: server unavailable
	 */
	SERVER_UNAVAILABLE,
	/**
	 * Connection Refused: bad user name or password
	 */
	BAD_CREDENTIALS,
	/**
	 * Connection Refused: not authorized
	 */
	NOT_AUTHORIZED,
	/**
	 * a return code unknown at the time of this writing
	 */
	OTHER;

	/**
	 * @return The {@link ConnectReturnCode} for the specified value. {@link #OTHER} if the value does not match any other explicit enum.
	 */
	public static ConnectReturnCode lookup(int value) {
		return value > values().length ? OTHER : values()[value];
	}

	/**
	 * @return The numeric value for this {@link ConnectReturnCode}
	 */
	public int value() {
		return ordinal();
	}
}
