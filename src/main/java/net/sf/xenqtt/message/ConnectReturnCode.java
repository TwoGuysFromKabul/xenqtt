package net.sf.xenqtt.message;

/**
 * The code returned in the {@link ConnAckMessage}. The order is important as the ordinal is the numeric value used in messages.
 */
public enum ConnectReturnCode {
	ACCEPTED, // Connection Accepted (success)
	UNACCEPTABLE_PROTOCOL_VERSION, // Connection Refused: unacceptable protocol version
	IDENTIFIER_REJECTED, // Connection Refused: identifier rejected. Sent if the unique client identifier is not between 1 and 23 characters in length.
	SERVER_UNAVAILABLE, // Connection Refused: server unavailable
	BAD_CREDENTIALS, // Connection Refused: bad user name or password
	NOT_AUTHORIZED, // Connection Refused: not authorized
	OTHER // a return code unknown at the time of this writing
	;

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
