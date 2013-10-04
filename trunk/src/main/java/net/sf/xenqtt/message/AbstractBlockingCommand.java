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
package net.sf.xenqtt.message;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import net.sf.xenqtt.MqttCommandCancelledException;
import net.sf.xenqtt.MqttInterruptedException;
import net.sf.xenqtt.MqttInvocationError;
import net.sf.xenqtt.MqttInvocationException;
import net.sf.xenqtt.MqttTimeoutException;

/**
 * Implementation of {@link BlockingCommand} that provides a simple extension point for command implementations.
 */
public abstract class AbstractBlockingCommand<T> implements BlockingCommand<T> {

	private final CountDownLatch done = new CountDownLatch(1);

	private boolean cancelled;
	private T result;
	private Throwable failCause;

	/**
	 * @see net.sf.xenqtt.message.BlockingCommand#await()
	 */
	@Override
	public final T await() throws MqttInterruptedException {
		return await(Long.MAX_VALUE, TimeUnit.DAYS);
	}

	/**
	 * @see net.sf.xenqtt.message.BlockingCommand#await(long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public final T await(long timeout, TimeUnit unit) throws MqttInterruptedException, MqttTimeoutException {
		try {
			if (timeout == Long.MAX_VALUE) {
				done.await();
			} else {
				if (!done.await(timeout, unit)) {
					throw new MqttTimeoutException(String.format("Timed out waiting %d %s for command %s", timeout, unit, getClass().getSimpleName()));
				}
			}
		} catch (InterruptedException e) {
			throw new MqttInterruptedException(e);
		}

		if (failCause != null) {
			if (failCause instanceof Exception) {
				throw new MqttInvocationException("Command failed: " + getClass().getSimpleName(), (Exception) failCause);
			}
			if (failCause instanceof Error) {
				throw new MqttInvocationError("Command failed: " + getClass().getSimpleName(), (Error) failCause);
			}

			throw new RuntimeException("Unexpected exception type. This is a bug (and should be impossible)!", failCause);
		}

		if (cancelled) {
			throw new MqttCommandCancelledException("Command cancelled: " + getClass().getSimpleName(), this.failCause);
		}

		return result;
	}

	/**
	 * @see net.sf.xenqtt.message.BlockingCommand#execute()
	 */
	@Override
	public final void execute() {
		try {
			doExecute();
		} catch (Throwable t) {
			setFailureCause(t);
			complete();
		}
	}

	/**
	 * @see net.sf.xenqtt.message.BlockingCommand#setResult(java.lang.Object)
	 */
	@Override
	public void setResult(T result) {

		this.result = result;
	}

	/**
	 * @see net.sf.xenqtt.message.BlockingCommand#setFailureCause(java.lang.Throwable)
	 */
	@Override
	public void setFailureCause(Throwable cause) {

		this.failCause = cause;
	}

	/**
	 * @see net.sf.xenqtt.message.BlockingCommand#complete()
	 */
	@Override
	public void complete() {

		done.countDown();
	}

	/**
	 * @see net.sf.xenqtt.message.BlockingCommand#cancel()
	 */
	@Override
	public void cancel() {

		this.cancelled = true;
		done.countDown();
	}

	/**
	 * Extensions implement this method to execute the command
	 * 
	 * @throws Exception
	 *             Any exception thrown by the command. This will be set as the failure cause using {@link #setFailureCause(Throwable)}.
	 */
	protected abstract void doExecute() throws Throwable;
}
