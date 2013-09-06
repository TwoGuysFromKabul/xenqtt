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

	/**
	 * Constructs a new mqtt exception with the specified detail message and cause.
	 * <p>
	 * Note that the detail message associated with <code>cause</code> is <i>not</i> automatically incorporated in this mqtt exception's detail message.
	 * 
	 * @param message
	 *            the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
	 * @param cause
	 *            the cause (which is saved for later retrieval by the {@link #getCause()} method).
	 */
	public MqttInterruptedException(String message, InterruptedException cause) {
		super(message, cause);
	}

	/**
	 * Constructs a new mqtt exception with the specified cause and a detail message of <tt>(cause==null ? null : cause.toString())</tt> (which typically
	 * contains the class and detail message of <tt>cause</tt>).
	 * 
	 * @param cause
	 *            the cause (which is saved for later retrieval by the {@link #getCause()} method).
	 */
	public MqttInterruptedException(InterruptedException cause) {
		super(cause);
	}
}
