package net.sf.xenqtt.gw.message;

/**
 * QoS is the quality of service which determines the assurance of message delivery. The order in this enum is important
 * as the ordinal is used as the numeric value of the QoS in the message.
 */
public enum QoS {

	AT_MOST_ONCE, // Fire and Forget
	AT_LEAST_ONCE, // Acknowledged delivery
	EXACTLY_ONCE // Assured delivery
}
