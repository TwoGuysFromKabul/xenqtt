package net.sf.xenqtt.message;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import net.sf.xenqtt.MqttException;
import net.sf.xenqtt.MqttInterruptedException;
import net.sf.xenqtt.MqttTimeoutException;

/**
 * Implementation of {@link BlockingCommand} that provides a simple extension point for command implementations.
 */
public abstract class AbstractBlockingCommand<T> implements BlockingCommand<T> {

	private final CountDownLatch done = new CountDownLatch(1);

	private T returnValue;
	private Throwable failCause;

	/**
	 * @see net.sf.xenqtt.message.BlockingCommand#await()
	 */
	@Override
	public final T await() throws MqttInterruptedException {
		return await(0, TimeUnit.DAYS);
	}

	/**
	 * @see net.sf.xenqtt.message.BlockingCommand#await(long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public final T await(long timeout, TimeUnit unit) throws MqttInterruptedException, MqttTimeoutException {
		try {
			if (timeout == Long.MAX_VALUE && unit == TimeUnit.DAYS) {
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

		return returnValue;
	}

	/**
	 * @see net.sf.xenqtt.message.BlockingCommand#execute()
	 */
	@Override
	public final void execute() {
		try {
			returnValue = doExecute();
		} catch (RuntimeException e) {
			failCause = e;
		} catch (Exception e) {
			failCause = new MqttException(e);
		} catch (Error e) {
			failCause = e;
		}
	}

	/**
	 * @see net.sf.xenqtt.message.BlockingCommand#complete(java.lang.Throwable)
	 */
	@Override
	public final void complete(Throwable failCause) {

		if (this.failCause == null && failCause != null) {
			if (failCause instanceof RuntimeException) {
				this.failCause = failCause;
			} else if (failCause instanceof Exception) {
				this.failCause = new MqttException(failCause);
			} else {
				this.failCause = failCause;
			}
		}

		done.countDown();
	}

	/**
	 * Extensions implement this method to execute the command
	 * 
	 * @return The value returned by the command
	 * @throws Exception
	 *             Any exception thrown by the command
	 */
	protected abstract T doExecute() throws Exception;
}
