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
package net.xenqtt.message;

import java.util.concurrent.TimeUnit;

import net.xenqtt.MqttCommandCancelledException;
import net.xenqtt.MqttInterruptedException;
import net.xenqtt.MqttInvocationError;
import net.xenqtt.MqttInvocationException;
import net.xenqtt.MqttTimeoutException;
import net.xenqtt.client.SyncMqttClient;

/**
 * Command that allows the calling thread to block until the command is completed.
 * 
 * @param <T>
 *            The type returned by {@link #await()} and {@link #await(long, TimeUnit)}. Use {@link Void} if there is no return value.
 */
public interface BlockingCommand<T> {

	/**
	 * Waits indefinitely for the command to complete.
	 * 
	 * @return The result of the command, if any.
	 * 
	 * @throws MqttCommandCancelledException
	 *             Thrown if the command was {@link #cancel() cancelled}
	 * @throws MqttInterruptedException
	 *             Thrown when the {@link SyncMqttClient} implementation is used and the calling thread is {@link Thread#interrupt() interrupted}.
	 * @throws MqttInvocationException
	 *             Any {@link Exception} thrown by a command will be wrapped in an {@link MqttInvocationException}
	 * @throws MqttInvocationError
	 *             Any {@link Error} thrown by a command will be wrapped in an {@link MqttInvocationError}
	 */
	T await() throws MqttCommandCancelledException, MqttInterruptedException, MqttInvocationException, MqttInvocationError;

	/**
	 * Waits for the specified amount of time for the command to complete
	 * 
	 * @param timeout
	 *            The maximum time to wait. If this is {@link Long#MAX_VALUE} is used then the unit arg is ignored and this is the same as invoking
	 *            {@link #await()}.
	 * @param unit
	 *            The time unit of the timeout argument
	 * 
	 * @return The result of the command, if any.
	 * 
	 * @throws MqttCommandCancelledException
	 *             Thrown if the command was {@link #cancel() cancelled}
	 * @throws MqttInterruptedException
	 *             Thrown when the {@link SyncMqttClient} implementation is used and the calling thread is {@link Thread#interrupt() interrupted}.
	 * @throws MqttTimeoutException
	 *             Thrown when the {@link SyncMqttClient} implementation is used and this method has blocked for approximately the configured timeout.
	 * @throws MqttInvocationException
	 *             Any {@link Exception} thrown by a command will be wrapped in an {@link MqttInvocationException}
	 * @throws MqttInvocationError
	 *             Any {@link Error} thrown by a command will be wrapped in an {@link MqttInvocationError}
	 */
	T await(long timeout, TimeUnit unit) throws MqttCommandCancelledException, MqttInterruptedException, MqttTimeoutException, MqttInvocationException,
			MqttInvocationError;

	/**
	 * <p>
	 * Executes the command. This is called by the thread responsible for processing the command, not the thread creating or waiting on the command. If an
	 * exception occurs {@link #setFailureCause(Throwable)} is called with the thrown exception and {@link #complete()} is invoked to complete the command. If
	 * the command returns a result use {@link #setResult(Object)} to set it.
	 * </p>
	 * 
	 * @param now
	 *            The time the command was executed
	 */
	void execute(long now);

	/**
	 * Sets the result of this command. This is the value returned by {@link #await()} and {@link #await(long, TimeUnit)}. This must be set before
	 * {@link #complete()} is called. The last value set before {@link #complete()} is called is returned from {@link #await()} or
	 * {@link #await(long, TimeUnit)}. This should only be called by the same thread that calls {@link #complete()}.
	 */
	void setResult(T result);

	/**
	 * Sets the cause if the command fails. Null to clear any existing cause. If this is non-null then it is wrapped and re-thrown by {@link #await()} or
	 * {@link #await(long, TimeUnit)} after {@link #complete()} is called. This should only be called by the same thread that calls {@link #execute()}.
	 */
	void setFailureCause(Throwable cause);

	/**
	 * Called when the command is complete. Causes {@link #await()} or {@link #await(long, TimeUnit)} to return.This should only be called by the same thread
	 * that calls {@link #execute()}.
	 */
	void complete();

	/**
	 * Cancels the command causes {@link #await()} or {@link #await(long, TimeUnit)} to throw an {@link MqttCommandCancelledException}.This should only be
	 * called by the same thread that calls {@link #execute()}.
	 */
	void cancel();
}
