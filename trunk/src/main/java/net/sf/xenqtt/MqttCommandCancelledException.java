/**
 * 
 */
package net.sf.xenqtt;

import net.sf.xenqtt.message.BlockingCommand;

/**
 * Thrown when a {@link BlockingCommand} is {@link BlockingCommand#cancel() cancelled}.
 */
public class MqttCommandCancelledException extends MqttException {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructs a new instance with <code>null</code> as its detail message. The cause is not initialized, and may subsequently be initialized by a call to
	 * {@link #initCause}.
	 */
	public MqttCommandCancelledException() {
	}

	/**
	 * Constructs a new instance with the specified detail message. The cause is not initialized, and may subsequently be initialized by a call to
	 * {@link #initCause}.
	 * 
	 * @param message
	 *            the detail message. The detail message is saved for later retrieval by the {@link #getMessage()} method.
	 */
	public MqttCommandCancelledException(String message) {
		super(message);
	}

	/**
	 * Constructs a new instance with the specified detail message and cause.
	 * <p>
	 * Note that the detail message associated with <code>cause</code> is <i>not</i> automatically incorporated in thisinstance's detail message.
	 * 
	 * @param message
	 *            the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
	 * @param cause
	 *            the cause (which is saved for later retrieval by the {@link #getCause()} method). (A <tt>null</tt> value is permitted, and indicates that the
	 *            cause is nonexistent or unknown.)
	 */
	public MqttCommandCancelledException(String message, Throwable cause) {
		super(message, cause);
	}
}
