/**
 * 
 */
package net.sf.xenqtt;

/**
 * Thrown when a timeout occurs in xenqtt.
 */
public class MqttTimeoutException extends MqttException {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a new mqtt exception with <code>null</code> as its detail message. The cause is not initialized, and may subsequently be initialized by a call
	 * to {@link #initCause}.
	 */
	public MqttTimeoutException() {
	}

	/**
	 * Constructs a new mqtt exception with the specified detail message. The cause is not initialized, and may subsequently be initialized by a call to
	 * {@link #initCause}.
	 * 
	 * @param message
	 *            the detail message. The detail message is saved for later retrieval by the {@link #getMessage()} method.
	 */
	public MqttTimeoutException(String message) {
		super(message);
	}
}
