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
package net.xenqtt;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

	@Test(expected = IllegalArgumentException.class)
	public void testValidateNotNull() {
		Object o = new Object();
		assertSame(o, XenqttUtil.validateNotNull("object", o));
		XenqttUtil.validateNotNull("object", null);
	}

	@Test
	public void testValidateNotEmpty() {
		String str = "string";
		assertSame(str, XenqttUtil.validateNotEmpty("string", str));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateNotEmpty_Blank() {
		XenqttUtil.validateNotEmpty("string", "");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateNotEmpty_Null() {
		String str = null;
		XenqttUtil.validateNotEmpty("string", str);
	}

	@Test
	public void testValidateNotEmpty_Array() {
		Object[] os = new Object[1];
		os[0] = new Object();
		assertSame(os, XenqttUtil.validateNotEmpty("os", os));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateNotEmpty_ArrayOfSizeZero() {
		Object[] os = new Object[0];
		XenqttUtil.validateNotEmpty("os", os);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateNotEmpty_NullArray() {
		Object[] os = null;
		XenqttUtil.validateNotEmpty("os", os);
	}

	@Test
	public void testValidateNotEmpty_Collection() {
		Collection<Object> os = new ArrayList<Object>();
		os.add(new Object());
		assertSame(os, XenqttUtil.validateNotEmpty("os", os));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateNotEmpty_CollectionOfSizeZero() {
		Collection<Object> os = new ArrayList<Object>();
		XenqttUtil.validateNotEmpty("os", os);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateNotEmpty_NullCollection() {
		Collection<Object> os = null;
		XenqttUtil.validateNotEmpty("os", os);
	}

	@Test
	public void testValidateLessThan() {
		Integer value = Integer.valueOf(7);
		Integer max = Integer.valueOf(10);
		assertSame(value, XenqttUtil.validateLessThan("integer", value, max));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateLessThan_Greater() {
		Integer value = Integer.valueOf(7);
		Integer max = Integer.valueOf(5);
		XenqttUtil.validateLessThan("integer", value, max);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateLessThan_Equal() {
		Integer value = Integer.valueOf(7);
		Integer max = Integer.valueOf(7);
		XenqttUtil.validateLessThan("integer", value, max);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateLessThan_Null() {
		Integer max = Integer.valueOf(7);
		XenqttUtil.validateLessThan("integer", null, max);
	}

	@Test
	public void testValidateLessThanOrEqualTo() {
		Integer value = Integer.valueOf(7);
		Integer max = Integer.valueOf(10);
		assertSame(value, XenqttUtil.validateLessThanOrEqualTo("integer", value, max));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateLessThanOrEqualTo_Greater() {
		Integer value = Integer.valueOf(7);
		Integer max = Integer.valueOf(5);
		XenqttUtil.validateLessThanOrEqualTo("integer", value, max);
	}

	@Test
	public void testValidateLessThanOrEqualTo_Equal() {
		Integer value = Integer.valueOf(7);
		Integer max = Integer.valueOf(7);
		assertSame(value, XenqttUtil.validateLessThanOrEqualTo("integer", value, max));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateLessThanOrEqualTo_Null() {
		Integer max = Integer.valueOf(7);
		XenqttUtil.validateLessThanOrEqualTo("integer", null, max);
	}

	@Test
	public void testValidateGreaterThan() {
		Integer value = Integer.valueOf(7);
		Integer max = Integer.valueOf(5);
		assertSame(value, XenqttUtil.validateGreaterThan("integer", value, max));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateGreaterThan_Less() {
		Integer value = Integer.valueOf(7);
		Integer max = Integer.valueOf(10);
		XenqttUtil.validateGreaterThan("integer", value, max);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateGreaterThan_Equal() {
		Integer value = Integer.valueOf(7);
		Integer max = Integer.valueOf(7);
		XenqttUtil.validateGreaterThan("integer", value, max);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateGreaterThan_Null() {
		Integer max = Integer.valueOf(7);
		XenqttUtil.validateGreaterThan("integer", null, max);
	}

	@Test
	public void testValidateGreaterThanOrEqualTo() {
		Integer value = Integer.valueOf(7);
		Integer max = Integer.valueOf(5);
		assertSame(value, XenqttUtil.validateGreaterThanOrEqualTo("integer", value, max));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateGreaterThanOrEqualTo_Less() {
		Integer value = Integer.valueOf(7);
		Integer max = Integer.valueOf(10);
		XenqttUtil.validateGreaterThanOrEqualTo("integer", value, max);
	}

	@Test
	public void testValidateGreaterThanOrEqualTo_Equal() {
		Integer value = Integer.valueOf(7);
		Integer max = Integer.valueOf(7);
		assertSame(value, XenqttUtil.validateGreaterThanOrEqualTo("integer", value, max));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateGreaterThanOrEqualTo_Null() {
		Integer max = Integer.valueOf(7);
		XenqttUtil.validateGreaterThanOrEqualTo("integer", null, max);
	}

	@Test
	public void testValidateInRange() {
		Integer start = Integer.valueOf(1);
		Integer end = Integer.valueOf(10);
		for (int i = 1; i <= 10; i++) {
			Integer value = Integer.valueOf(i);
			assertSame(value, XenqttUtil.validateInRange("range", value, start, end));
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateInRange_OutOfRange_Below() {
		Integer start = Integer.valueOf(1);
		Integer end = Integer.valueOf(10);
		XenqttUtil.validateInRange("range", Integer.valueOf(0), start, end);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateInRange_OutOfRange_Above() {
		Integer start = Integer.valueOf(1);
		Integer end = Integer.valueOf(10);
		XenqttUtil.validateInRange("range", Integer.valueOf(11), start, end);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testValidateInRange_OutOfRange_Null() {
		Integer start = Integer.valueOf(1);
		Integer end = Integer.valueOf(10);
		XenqttUtil.validateInRange("range", null, start, end);
	}

	@Test
	public void testGetDirectoryHostingRunningXenqttJar() throws Exception {

		assertTrue(XenqttUtil.getXenqttInstallDirectory().getAbsolutePath().endsWith(File.separator + "target"));
	}

	@Test
	public void testGetXenqttClassPathRoot() throws Exception {

		assertTrue(XenqttUtil.getXenqttClassPathRoot().getAbsolutePath().endsWith(File.separator + "target" + File.separator + "classes"));
	}

	@Test
	public void testFindFilesOnClassPath_NoPackageFilter_NoExtensionFilter() throws Exception {

		List<String> files = XenqttUtil.findFilesOnClassPath(null, null);
		assertTrue(files.contains("net/xenqtt/Xenqtt.class"));
		assertTrue(files.contains("net/xenqtt/client/MqttClient.class"));
		assertTrue(files.contains("LICENSE.txt"));
	}

	@Test
	public void testFindFilesOnClassPath_WithPackageFilter_NoExtensionFilter() throws Exception {

		List<String> files = XenqttUtil.findFilesOnClassPath("net.xenqtt", null);
		assertTrue(files.contains("net/xenqtt/Xenqtt.class"));
		assertFalse(files.contains("net/xenqtt/client/MqttClient.class"));
		assertFalse(files.contains("LICENSE.txt"));
	}

	@Test
	public void testFindFilesOnClassPath_NoPackageFilter_WithExtensionFilter() throws Exception {

		List<String> files = XenqttUtil.findFilesOnClassPath(null, ".txt");
		assertFalse(files.contains("net/sf/xenqtt/Xenqtt.class"));
		assertFalse(files.contains("net/sf/xenqtt/client/MqttClient.class"));
		assertTrue(files.contains("LICENSE.txt"));
	}
}
