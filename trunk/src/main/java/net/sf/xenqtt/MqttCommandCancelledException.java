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
}
