package net.sf.xenqtt;

import static org.junit.Assert.*;

import org.junit.Test;

public class XenqttUtilTest {

	@Test
	public void testQuickSplit_OneDelimiter() {
		String value = "ab/cd";
		String[] values = XenqttUtil.quickSplit(value, '/');
		assertEquals(2, values.length);
		assertEquals("ab", values[0]);
		assertEquals("cd", values[1]);
	}

	@Test
	public void testQuickSplit_NoDelimiter() {
		String value = "abcd";
		String[] values = XenqttUtil.quickSplit(value, '/');
		assertEquals(1, values.length);
		assertEquals("abcd", values[0]);
	}

	@Test
	public void testQuickSplit_MultipleDelimiters() {
		String value = "a/b/c/d";
		String[] values = XenqttUtil.quickSplit(value, '/');
		assertEquals(4, values.length);
		assertEquals("a", values[0]);
		assertEquals("b", values[1]);
		assertEquals("c", values[2]);
		assertEquals("d", values[3]);
	}

	@Test
	public void testQuickSplit_DelimiterAtBeginning() {
		String value = "/ab/cd";
		String[] values = XenqttUtil.quickSplit(value, '/');
		assertEquals(2, values.length);
		assertEquals("ab", values[0]);
		assertEquals("cd", values[1]);
	}

	@Test
	public void testQuickSplit_DelimiterAtEnd() {
		String value = "ab/cd/";
		String[] values = XenqttUtil.quickSplit(value, '/');
		assertEquals(2, values.length);
		assertEquals("ab", values[0]);
		assertEquals("cd", values[1]);
	}

	@Test
	public void testQuickSplit_DelimiterAtBeginningAndEnd() {
		String value = "/ab/cd/";
		String[] values = XenqttUtil.quickSplit(value, '/');
		assertEquals(2, values.length);
		assertEquals("ab", values[0]);
		assertEquals("cd", values[1]);
	}

	@Test
	public void testQuickSplit_LotsOfDelimiters() {
		String value = "//ab////cd///";
		String[] values = XenqttUtil.quickSplit(value, '/');
		assertEquals(2, values.length);
		assertEquals("ab", values[0]);
		assertEquals("cd", values[1]);
	}

	@Test
	public void testQuickSplit_OnlyDelimiters() {
		String value = "/////";
		String[] values = XenqttUtil.quickSplit(value, '/');
		assertEquals(0, values.length);
	}

	@Test
	public void testQuickSplit_NullString() {
		String[] values = XenqttUtil.quickSplit(null, '/');
		assertEquals(0, values.length);
	}

	@Test
	public void testQuickSplit_EmptyString() {
		String[] values = XenqttUtil.quickSplit("", '/');
		assertEquals(0, values.length);
	}

}
