/**
 * 
 */
/**
    Copyright 2013 James McClure

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
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
