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
package net.xenqtt;

import java.io.PrintStream;

/**
 * Thrown when an {@link Exception} occurs during the processing of a command.
 */
public final class MqttInvocationException extends MqttException {

	private static final long serialVersionUID = 1L;

	private final Exception rootCause;

	/**
	 * Create a new instance of this class.
	 * 
	 * @param message
	 *            A textual message describing the situation that led to the throwing of this {@link MqttInvocationException exception}
	 * @param rootCause
	 *            The {@link Exception exception} that was originally thrown
	 */
	public MqttInvocationException(String message, Exception rootCause) {
		super(message);
		this.rootCause = rootCause;
	}

	/**
	 * @return The exception that occurred while the command was executing. This is not the {@link #getCause()} of this exception because it happened on a
	 *         different thread. The stack trace for the {@link #getRootCause() root cause} shows where the problem lies. The stack trace for this
	 *         {@link MqttInvocationException invocation exception} shows where the command was called from.
	 */
	public Exception getRootCause() {
		return rootCause;
	}

	/**
	 * @see java.lang.Throwable#printStackTrace(java.io.PrintStream)
	 */
	@Override
	public void printStackTrace(PrintStream s) {
		synchronized (s) {
			super.printStackTrace(s);
			s.print("ROOT CAUSE: ");
			if (rootCause == null) {
				s.println("Unknown");
			} else {
				rootCause.printStackTrace(s);
			}
		}
	}

	/**
	 * @see java.lang.Throwable#getMessage()
	 */
	@Override
	public String getMessage() {
		if (rootCause == null) {
			return String.format("%s; ROOT CAUSE: Unknown", super.getMessage());
		} else {
			return String.format("%s; ROOT CAUSE: %s", super.getMessage(), rootCause.getMessage());
		}
	}
}
