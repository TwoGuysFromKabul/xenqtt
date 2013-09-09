/**
 * 
 */
package net.sf.xenqtt;

/**
 * This is a wrapper for {@link InterruptedException}. {@link InterruptedException} is checked and we don't want to throw checked exceptions from the public
 * API.
 */
public class MqttInterruptedException extends MqttException {

	private static final long serialVersionUID = 1L;

	public MqttInterruptedException(InterruptedException cause) {
		super(cause);
	}
}
