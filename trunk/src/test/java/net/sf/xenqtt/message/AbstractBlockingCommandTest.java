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

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import net.sf.xenqtt.MqttCommandCancelledException;
import net.sf.xenqtt.MqttInterruptedException;
import net.sf.xenqtt.MqttInvocationError;
import net.sf.xenqtt.MqttInvocationException;
import net.sf.xenqtt.MqttTimeoutException;

import org.junit.Test;

public class AbstractBlockingCommandTest {

	TestBlockingCommand cmd = new TestBlockingCommand();
	long now = System.currentTimeMillis();

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

	@Test(expected = MqttInvocationException.class)
	public void testAwait_Cancelled_WithException() throws Exception {

		Exception exception = new IllegalStateException();

		cmd = new TestBlockingCommand();
		cmd.setFailureCause(exception);
		cmd.cancel();
		cmd.await(0, TimeUnit.MILLISECONDS);
	}

	@Test(expected = MqttCommandCancelledException.class)
	public void testAwaitLongTimeUnit_Cancelled() throws Exception {

		cmd = new TestBlockingCommand();
		cmd.cancel();
		cmd.await(10, TimeUnit.MILLISECONDS);
	}

	@Test
	public void testAwait_Success() {

		cmd.execute(now);
		cmd.complete();
		assertSame(cmd.returnValue, cmd.await());
	}

	@Test(expected = MqttInvocationException.class)
	public void testAwait_ThrowsRuntimeException() {
		cmd.execute(now);
		cmd.setFailureCause(new IllegalStateException());
		cmd.complete();
		cmd.await();
	}

	@Test(expected = Error.class)
	public void testAwait_ThrowsError() {
		cmd.execute(now);
		cmd.setFailureCause(new Error());
		cmd.complete();
		cmd.await();
	}

	@Test(expected = MqttInterruptedException.class)
	public void testAwait_Interrupted() {
		cmd.execute(now);
		Thread.currentThread().interrupt();
		cmd.await();
	}

	@Test
	public void testAwaitLongTimeUnit_Success() {
		cmd.execute(now);
		cmd.complete();
		assertSame(cmd.returnValue, cmd.await(10, TimeUnit.MILLISECONDS));
	}

	@Test(expected = MqttInvocationException.class)
	public void testAwaitLongTimeUnit_ThrowsRuntimeException() {
		cmd.execute(now);
		cmd.setFailureCause(new IllegalStateException());
		cmd.complete();
		cmd.await(10, TimeUnit.MILLISECONDS);
	}

	@Test(expected = Error.class)
	public void testAwaitLongTimeUnit_ThrowsError() {
		cmd.execute(now);
		cmd.setFailureCause(new Error());
		cmd.complete();
		cmd.await(10, TimeUnit.MILLISECONDS);
	}

	@Test(expected = MqttInterruptedException.class)
	public void testAwaitLongTimeUnit_Interrupted() {
		cmd.execute(now);
		Thread.currentThread().interrupt();
		cmd.await(10, TimeUnit.MILLISECONDS);
	}

	@Test(expected = MqttTimeoutException.class)
	public void testAwaitLongTimeUnit_Timeout() {
		cmd.await(10, TimeUnit.MILLISECONDS);
	}

	@Test
	public void testExecute_Success() {
		cmd.execute(now);
		cmd.complete();
		assertSame(cmd.returnValue, cmd.await());
	}

	@Test(expected = MqttInvocationException.class)
	public void testExecute_RuntimeException() {
		cmd.exceptionToThrow = new IllegalStateException();
		cmd.execute(now);
		cmd.complete();
		cmd.await();
	}

	@Test
	public void testExecute_CheckedException() {

		cmd.exceptionToThrow = new IOException();
		cmd.execute(now);
		cmd.complete();

		try {
			cmd.await();
			fail("expected exception");
		} catch (MqttInvocationException e) {
			assertSame(cmd.exceptionToThrow, e.getRootCause());
		}
	}

	@Test
	public void testExecute_Error() {

		cmd.errorToThrow = new Error();
		cmd.execute(now);
		cmd.complete();

		try {
			cmd.await();
			fail("expected error");
		} catch (MqttInvocationError e) {
			assertSame(cmd.errorToThrow, e.getRootCause());
		}
	}

	@Test
	public void testComplete_NoException() {
		cmd.execute(now);
		cmd.complete();
		assertSame(cmd.returnValue, cmd.await());
	}

	@Test(expected = MqttInvocationException.class)
	public void testComplete_WithException() {
		cmd.execute(now);
		cmd.setFailureCause(new IllegalStateException());
		cmd.complete();
		cmd.await();
	}

	private static class TestBlockingCommand extends AbstractBlockingCommand<Object> {

		final Object returnValue = new Object();
		Exception exceptionToThrow;
		Error errorToThrow;

		@Override
		protected void doExecute(long now) throws Exception {

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
