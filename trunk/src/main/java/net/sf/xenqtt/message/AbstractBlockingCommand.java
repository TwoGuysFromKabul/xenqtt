package net.sf.xenqtt.message;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import net.sf.xenqtt.MqttCommandCancelledException;
import net.sf.xenqtt.MqttException;
import net.sf.xenqtt.MqttInterruptedException;
import net.sf.xenqtt.MqttTimeoutException;

/**
 * Implementation of {@link BlockingCommand} that provides a simple extension point for command implementations.
 */
public abstract class AbstractBlockingCommand<T> implements BlockingCommand<T> {

	private final CountDownLatch done = new CountDownLatch(1);

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
			if (failCause instanceof RuntimeException) {
				throw (RuntimeException) failCause;
			}
			if (failCause instanceof Error) {
				throw (Error) failCause;
			}

			throw new RuntimeException("Unexpected exception. This is a bug!", failCause);
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

		if (cause instanceof RuntimeException) {
			this.failCause = cause;
		} else if (cause instanceof Exception) {
			this.failCause = new MqttException(cause);
		} else {
			this.failCause = cause;
		}
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

		this.failCause = new MqttCommandCancelledException("Command cancelled: " + getClass().getSimpleName(), this.failCause);
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
