package net.sf.xenqtt.message;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import net.sf.xenqtt.MqttCommandCancelledException;
import net.sf.xenqtt.MqttException;
import net.sf.xenqtt.MqttInterruptedException;
import net.sf.xenqtt.MqttTimeoutException;

import org.junit.Test;

public class AbstractBlockingCommandTest {

	TestBlockingCommand cmd = new TestBlockingCommand();

	@Test
	public void testDefaultCtor() throws Exception {

		cmd.complete();
		cmd.await(0, TimeUnit.MILLISECONDS);
	}

	@Test(expected = MqttCommandCancelledException.class)
	public void testAwait_Cancelled_NoException() throws Exception {

		cmd = new TestBlockingCommand();
		cmd.cancel();
		cmd.await(0, TimeUnit.MILLISECONDS);
	}

	@Test
	public void testAwait_Cancelled_WithException() throws Exception {

		Exception exception = new IllegalStateException();

		cmd = new TestBlockingCommand();
		cmd.setFailureCause(exception);
		cmd.cancel();

		try {
			cmd.await(0, TimeUnit.MILLISECONDS);
			fail("expected exception");
		} catch (MqttCommandCancelledException e) {
			assertSame(exception, e.getCause());
		}
	}

	@Test(expected = MqttCommandCancelledException.class)
	public void testAwaitLongTimeUnit_Cancelled() throws Exception {

		cmd = new TestBlockingCommand();
		cmd.cancel();
		cmd.await(10, TimeUnit.MILLISECONDS);
	}

	@Test
	public void testAwait_Success() {

		cmd.execute();
		cmd.complete();
		assertSame(cmd.returnValue, cmd.await());
	}

	@Test(expected = IllegalStateException.class)
	public void testAwait_ThrowsRuntimeException() {
		cmd.execute();
		cmd.setFailureCause(new IllegalStateException());
		cmd.complete();
		cmd.await();
	}

	@Test(expected = Error.class)
	public void testAwait_ThrowsError() {
		cmd.execute();
		cmd.setFailureCause(new Error());
		cmd.complete();
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
		cmd.complete();
		assertSame(cmd.returnValue, cmd.await(10, TimeUnit.MILLISECONDS));
	}

	@Test(expected = IllegalStateException.class)
	public void testAwaitLongTimeUnit_ThrowsRuntimeException() {
		cmd.execute();
		cmd.setFailureCause(new IllegalStateException());
		cmd.complete();
		cmd.await(10, TimeUnit.MILLISECONDS);
	}

	@Test(expected = Error.class)
	public void testAwaitLongTimeUnit_ThrowsError() {
		cmd.execute();
		cmd.setFailureCause(new Error());
		cmd.complete();
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
		cmd.complete();
		assertSame(cmd.returnValue, cmd.await());
	}

	@Test(expected = IllegalStateException.class)
	public void testExecute_RuntimeException() {
		cmd.exceptionToThrow = new IllegalStateException();
		cmd.execute();
		cmd.complete();
		cmd.await();
	}

	@Test
	public void testExecute_CheckedException() {

		cmd.exceptionToThrow = new IOException();
		cmd.execute();
		cmd.complete();

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
		cmd.complete();
		cmd.await();
	}

	@Test
	public void testComplete_NoException() {
		cmd.execute();
		cmd.complete();
		assertSame(cmd.returnValue, cmd.await());
	}

	@Test(expected = IllegalStateException.class)
	public void testComplete_WithException() {
		cmd.execute();
		cmd.setFailureCause(new IllegalStateException());
		cmd.complete();
		cmd.await();
	}

	private static class TestBlockingCommand extends AbstractBlockingCommand<Object> {

		final Object returnValue = new Object();
		Exception exceptionToThrow;
		Error errorToThrow;

		@Override
		protected void doExecute() throws Exception {

			if (errorToThrow != null) {
				throw errorToThrow;
			}
			if (exceptionToThrow != null) {
				throw exceptionToThrow;
			}

			setResult(returnValue);
		}
	}
}
