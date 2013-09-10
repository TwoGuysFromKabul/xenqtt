package net.sf.xenqtt.message;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import net.sf.xenqtt.MqttException;
import net.sf.xenqtt.MqttInterruptedException;
import net.sf.xenqtt.MqttTimeoutException;

import org.junit.Test;

public class AbstractBlockingCommandTest {

	TestBlockingCommand cmd = new TestBlockingCommand();

	@Test
	public void testAwait_Success() {

		cmd.execute();
		cmd.complete(null);
		assertSame(cmd.returnValue, cmd.await());
	}

	@Test(expected = IllegalStateException.class)
	public void testAwait_ThrowsRuntimeException() {
		cmd.execute();
		cmd.complete(new IllegalStateException());
		cmd.await();
	}

	@Test(expected = Error.class)
	public void testAwait_ThrowsError() {
		cmd.execute();
		cmd.complete(new Error());
		cmd.await();
	}

	@Test(expected = MqttInterruptedException.class)
	public void testAwait_Interrupted() {
		cmd.execute();
		Thread.currentThread().interrupt();
		cmd.await();
	}

	@Test
	public void testAwaitLongTimeUnit_Success() {
		cmd.execute();
		cmd.complete(null);
		assertSame(cmd.returnValue, cmd.await(10, TimeUnit.MILLISECONDS));
	}

	@Test(expected = IllegalStateException.class)
	public void testAwaitLongTimeUnit_ThrowsRuntimeException() {
		cmd.execute();
		cmd.complete(new IllegalStateException());
		cmd.await(10, TimeUnit.MILLISECONDS);
	}

	@Test(expected = Error.class)
	public void testAwaitLongTimeUnit_ThrowsError() {
		cmd.execute();
		cmd.complete(new Error());
		cmd.await(10, TimeUnit.MILLISECONDS);
	}

	@Test(expected = MqttInterruptedException.class)
	public void testAwaitLongTimeUnit_Interrupted() {
		cmd.execute();
		Thread.currentThread().interrupt();
		cmd.await(10, TimeUnit.MILLISECONDS);
	}

	@Test(expected = MqttTimeoutException.class)
	public void testAwaitLongTimeUnit_Timeout() {
		cmd.await(10, TimeUnit.MILLISECONDS);
	}

	@Test
	public void testExecute_Success() {
		cmd.execute();
		cmd.complete(null);
		assertSame(cmd.returnValue, cmd.await());
	}

	@Test(expected = IllegalStateException.class)
	public void testExecute_RuntimeException() {
		cmd.exceptionToThrow = new IllegalStateException();
		cmd.execute();
		cmd.complete(null);
		cmd.await();
	}

	@Test
	public void testExecute_CheckedException() {

		cmd.exceptionToThrow = new IOException();
		cmd.execute();
		cmd.complete(null);

		try {
			cmd.await();
			fail("expected exception");
		} catch (MqttException e) {
			assertSame(cmd.exceptionToThrow, e.getCause());
		}
	}

	@Test(expected = Error.class)
	public void testExecute_Error() {

		cmd.errorToThrow = new Error();
		cmd.execute();
		cmd.complete(null);
		cmd.await();
	}

	@Test
	public void testComplete_Success_ExecuteSucceeded() {
		cmd.execute();
		cmd.complete(null);
		assertSame(cmd.returnValue, cmd.await());
	}

	@Test(expected = IllegalStateException.class)
	public void testComplete_RuntimeException_ExecuteSucceeded() {
		cmd.execute();
		cmd.complete(new IllegalStateException());
		cmd.await();
	}

	@Test
	public void testComplete_CheckedException_ExecuteSucceeded() {

		Exception exception = new IOException();
		cmd.execute();
		cmd.complete(exception);

		try {
			cmd.await();
			fail("expected exception");
		} catch (MqttException e) {
			assertSame(exception, e.getCause());
		}
	}

	@Test(expected = Error.class)
	public void testComplete_Error_ExecuteSucceeded() {
		cmd.execute();
		cmd.complete(new Error());
		cmd.await();
	}

	@Test(expected = IllegalStateException.class)
	public void testComplete_Success_ExecuteFailed() {
		cmd.exceptionToThrow = new IllegalStateException();
		cmd.execute();
		cmd.complete(null);
		cmd.await();
	}

	@Test(expected = Error.class)
	public void testComplete_RuntimeException_ExecuteFailed() {
		cmd.errorToThrow = new Error();
		cmd.execute();
		cmd.complete(new IllegalStateException());
		cmd.await();
	}

	@Test(expected = Error.class)
	public void testComplete_CheckedException_ExecuteFailed() {
		cmd.errorToThrow = new Error();
		cmd.execute();
		cmd.complete(new IOException());
		cmd.await();
	}

	@Test(expected = Exception.class)
	public void testComplete_Error_ExecuteFailed() {
		cmd.exceptionToThrow = new IllegalStateException();
		cmd.execute();
		cmd.complete(new Error());
		cmd.await();
	}

	private static class TestBlockingCommand extends AbstractBlockingCommand<Object> {

		final Object returnValue = new Object();
		Exception exceptionToThrow;
		Error errorToThrow;

		@Override
		protected Object doExecute() throws Exception {

			if (errorToThrow != null) {
				throw errorToThrow;
			}
			if (exceptionToThrow != null) {
				throw exceptionToThrow;
			}
			return returnValue;
		}

	}
}
