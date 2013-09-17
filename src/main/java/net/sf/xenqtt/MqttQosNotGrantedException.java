package net.sf.xenqtt;

import net.sf.xenqtt.client.MqttClient;
import net.sf.xenqtt.client.Subscription;
import net.sf.xenqtt.message.QoS;

/**
 * Thrown by synchronous {@link MqttClient#subscribe(java.util.List)} and {@link MqttClient#subscribe(Subscription[])} methods when the {@link QoS} granted for
 * a subscription request does not match the QoS requested.
 */
public final class MqttQosNotGrantedException extends MqttException {

	private static final long serialVersionUID = 1L;

	private final Subscription[] grantedSubscriptions;

	public MqttQosNotGrantedException(Subscription[] grantedSubscriptions) {
		this.grantedSubscriptions = grantedSubscriptions;
	}

	/**
	 * @return The topics subscribed to and the QoS granted for each
	 */
	public Subscription[] getGrantedSubscriptions() {
		return grantedSubscriptions;
	}
}
