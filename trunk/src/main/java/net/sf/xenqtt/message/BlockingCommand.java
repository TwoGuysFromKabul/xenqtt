package net.sf.xenqtt.message;

import java.util.concurrent.TimeUnit;

import net.sf.xenqtt.MqttCommandCancelledException;
import net.sf.xenqtt.MqttException;
import net.sf.xenqtt.MqttInterruptedException;
import net.sf.xenqtt.MqttTimeoutException;
import net.sf.xenqtt.client.SynchronousMqttClient;

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
	 *             Thrown when the {@link SynchronousMqttClient} implementation is used and the calling thread is {@link Thread#interrupt() interrupted}.
	 * @throws MqttException
	 *             Any checked exception thrown by the command will be wrapped in an {@link MqttException}
	 * @throws RuntimeException
	 *             Of course, any {@link RuntimeException} may be thrown
	 */
	T await() throws MqttCommandCancelledException, MqttInterruptedException, MqttException, RuntimeException;

	/**
	 * Waits for the specified amount of time for the command to complete
	 * 
	 * @param timeout
	 *            the maximum time to wait
	 * @param unit
	 *            the time unit of the timeout argument
	 * 
	 * @return The result of the command, if any.
	 * 
	 * @throws MqttCommandCancelledException
	 *             Thrown if the command was {@link #cancel() cancelled}
	 * @throws MqttInterruptedException
	 *             Thrown when the {@link SynchronousMqttClient} implementation is used and the calling thread is {@link Thread#interrupt() interrupted}.
	 * @throws MqttTimeoutException
	 *             Thrown when the {@link SynchronousMqttClient} implementation is used and this method has blocked for approximately the configured timeout.
	 * @throws MqttException
	 *             Any checked exception thrown by the command will be wrapped in an {@link MqttException}
	 * @throws RuntimeException
	 *             Of course, any {@link RuntimeException} may be thrown
	 */
	T await(long timeout, TimeUnit unit) throws MqttCommandCancelledException, MqttInterruptedException, MqttTimeoutException, MqttException, RuntimeException;

	/**
	 * Executes the command. This is called by the thread responsible for processing the command, not the thread creating or waiting on the command.
	 */
	void execute();

	/**
	 * Called when the command is complete. Causes {@link #await()} or {@link #await(long, TimeUnit)} to return. This must be called as many times as the count
	 * specified in the constructor before the methods return.
	 * 
	 * @param failCause
	 *            Thrown by the command execution but not in the execution path of the original thread. Null if there was no exception.
	 */
	void complete(Throwable failCause);

	/**
	 * Cancels the command causes {@link #await()} or {@link #await(long, TimeUnit)} to throw an {@link MqttCommandCancelledException}.
	 */
	void cancel();
}
