package net.sf.xenqtt;

/**
 * Thrown when an invalid topic name is used
 */
public class MqttInvalidTopicNameException extends MqttException {

	private static final long serialVersionUID = 1L;

	public MqttInvalidTopicNameException(String message) {
		super(message);
	}
}
