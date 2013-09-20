package net.sf.xenqtt;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class XenqttTest {

	PrintStream standardOut;

	@Before
	public void setup() {
		standardOut = System.out;
	}

	@After
	public void teardown() {
		System.setOut(standardOut);
	}

	@Test
	public void testMain_InvalidMode() throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(baos);
		System.setOut(out);

		Xenqtt.main(new String[] { "-vv", "proxe" });
		assertTrue(new String(baos.toByteArray(), "US-ASCII").startsWith("usage: "));
	}

	@Test
	public void testMain_NoMode() throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(baos);
		System.setOut(out);

		Xenqtt.main(new String[] { "-vv" });
		assertTrue(new String(baos.toByteArray(), "US-ASCII").startsWith("usage: "));
	}

	@Test
	public void testMain_NoArgs() throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(baos);
		System.setOut(out);

		Xenqtt.main(new String[0]);
		assertTrue(new String(baos.toByteArray(), "US-ASCII").startsWith("usage: "));
	}

}
