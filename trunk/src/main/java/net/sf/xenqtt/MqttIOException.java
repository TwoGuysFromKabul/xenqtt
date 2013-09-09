/**
 * 
 */
package net.sf.xenqtt;

import java.io.IOException;

/**
 * This is a wrapper for {@link IOException}. {@link IOException} is checked and we don't want to throw checked exceptions from the public API.
 */
public class MqttIOException extends MqttException {

	private static final long serialVersionUID = 1L;

	public MqttIOException(IOException cause) {
		super(cause);
	}
}
