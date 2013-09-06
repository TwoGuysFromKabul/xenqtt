package net.sf.xenqtt;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.Test;

public class XenqttTest {

	@Test
	public void testMain_InvalidMode() throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(baos);
		System.setOut(out);

		Xenqtt.main(new String[] { "proxe", "-vv" });
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
	public void testMain_InvalidSwitch() throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(baos);
		System.setOut(out);

		Xenqtt.main(new String[] { "proxy", "-verbose" });
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
