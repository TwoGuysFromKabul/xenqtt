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
package net.sf.xenqtt;

import java.util.Collection;

/**
 * Provides disparate utility methods useful across the Xenqtt application ecosystem.
 */
public final class XenqttUtil {

	private static final String[] EMPTY_STRING_ARRAY = new String[0];

	private XenqttUtil() {
	}

	/**
	 * <p>
	 * Split a {@link String string} using a single delimiter. This method is useful in cases where one needs to split a string on a single character and
	 * desires optimal performance. The {@link String#split(String)} method builds a regular expression under the covers and, as a result, performs worse than
	 * this method does for the single character use case.
	 * </p>
	 * 
	 * <p>
	 * With respect to this method and the given input parameters, the following holds:
	 * </p>
	 * 
	 * <ul>
	 * <li><code>XenqttUtil.quickSplit("abcd", '/') ==> {"abcd"}</code></li>
	 * <li><code>XenqttUtil.quickSplit("ab/cd", '/') ==> {"ab", "cd"}</code></li>
	 * <li><code>XenqttUtil.quickSplit("a/b/c/d", '/') ==> {"a", "b", "c", "d"}</code></li>
	 * <li><code>XenqttUtil.quickSplit("/ab/cd", '/') ==> {"ab", "cd"}</code></li>
	 * <li><code>XenqttUtil.quickSplit("ab/cd/", '/') ==> {"ab", "cd"}</code></li>
	 * <li><code>XenqttUtil.quickSplit("/ab/cd/", '/') ==> {"ab", "cd"}</code></li>
	 * <li><code>XenqttUtil.quickSplit("///a//bc////d///////", '/') ==> {"a", "bc", "d"}</code></li>
	 * <li><code>XenqttUtil.quickSplit("///////", '/') ==> {}</code></li>
	 * <li><code>XenqttUtil.quickSplit("", '/') ==> {}</code></li>
	 * <li><code>XenqttUtil.quickSplit(null, '/') ==> {}</code></li>
	 * </ul>
	 * 
	 * @param value
	 *            The {@link String string} value that should be split
	 * @param delimiter
	 *            The delimiter to split the specified {@code value} on
	 * 
	 * @return An array of strings derived by splitting the {@code value} on the specified {@code delimiter}
	 */
	public static String[] quickSplit(String value, char delimiter) {
		if (value == null || value.isEmpty()) {
			return EMPTY_STRING_ARRAY;
		}

		int count = getStringValueCount(value, delimiter);
		if (count == 0) {
			return EMPTY_STRING_ARRAY;
		}

		String[] values = new String[count];
		int start = 0;
		int index = 0;
		char last = delimiter;
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (c != delimiter && last == delimiter) {
				start = i;
			} else if (c == delimiter && last != delimiter) {
				values[index++] = value.substring(start, i);
			}
			last = c;
		}

		if (index == count - 1) {
			values[index++] = value.substring(start);
		}

		return values;
	}

	private static int getStringValueCount(String value, char delimiter) {
		int count = 0;
		char last = delimiter;
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			if (c != delimiter && last == delimiter) {
				count++;
			}
			last = c;
		}

		return count;
	}

	/**
	 * Validates that an object is not {@code null}.
	 * 
	 * @param name
	 *            The name of the object being validated. Included in the exception message if one is thrown
	 * @param value
	 *            The object to validate
	 * 
	 * @return The specified {@code value}
	 * 
	 * @throws IllegalArgumentException
	 *             If {@code value} is {@code null}
	 */
	public static <T> T validateNotNull(String name, T value) {
		if (value == null) {
			doThrow("The argument %s must not be null.", name);
		}

		return value;
	}

	/**
	 * Validate that a {@link String string} is not empty. An empty string is either {@code null} or the empty string. Having only whitespace in the string (
	 * {@code \r \n \t ' '}) constitutes an empty string as well.
	 * 
	 * @param name
	 *            The name of the object being validated. Included in the exception message if one is thrown
	 * @param value
	 *            The string to validate
	 * 
	 * @return The specified {@code value}
	 * 
	 * @throws IllegalArgumentException
	 *             If {@code value} is an empty string
	 */
	public static String validateNotEmpty(String name, String value) {
		if (value == null || value.trim().equals("")) {
			doThrow("The argument %s must not be empty.", name);
		}

		return value;
	}

	/**
	 * Validate that an arbitrary array of objects of type {@code T} is not empty. The array is considered empty if it is {@code null} or contains zero
	 * elements.
	 * 
	 * @param name
	 *            The name of the object being validated. Included in the exception message if one is thrown
	 * @param value
	 *            The array to validate
	 * 
	 * @return The specified {@code value}
	 * 
	 * @throws IllegalArgumentException
	 *             If {@code value} is {@code null} or contains no elements
	 */
	public static <T> T[] validateNotEmpty(String name, T[] value) {
		if (value == null || value.length == 0) {
			doThrow("The argument %s must not be empty.", name);
		}

		return value;
	}

	/**
	 * Validate that an arbitrary {@link Collection collection} of objects of type {@code T} is not empty. The collection is considered empty if it is
	 * {@code null} or contains zero elements.
	 * 
	 * @param name
	 *            The name of the object being validated. Included in the exception message if one is thrown
	 * @param value
	 *            The collection to validate
	 * 
	 * @return The specified {@code value}
	 * 
	 * @throws IllegalArgumentException
	 *             If {@code value} is {@code null} or contains no elements
	 */
	public static <T> Collection<T> validateNotEmpty(String name, Collection<T> value) {
		if (value == null || value.size() == 0) {
			doThrow("The argument %s must not be empty.", name);
		}

		return value;
	}

	/**
	 * Validate that a number is less than a defined maximum.
	 * 
	 * @param name
	 *            The name of the object being validated. Included in the exception message if one is thrown
	 * @param value
	 *            The value of the number
	 * @param max
	 *            The maximum that the specified {@code value} must be under
	 * 
	 * @return The specified {@code value}
	 * 
	 * @throws IllegalArgumentException
	 *             If {@code value} is {@code null} or is greater than or equal to {@code max}
	 */
	public static <T extends Number> T validateLessThan(String name, T value, T max) {
		if (value == null) {
			doThrow("The argument %s must not be null.", name);
		}

		if (value.longValue() >= max.longValue()) {
			doThrow("The argument %s must not be greater than or equal to %d. Was: %d", name, value, max);
		}

		return value;
	}

	/**
	 * Validate that a number is less than or equal to a defined maximum.
	 * 
	 * @param name
	 *            The name of the object being validated. Included in the exception message if one is thrown
	 * @param value
	 *            The value of the number
	 * @param max
	 *            The maximum that the specified {@code value} must be under
	 * 
	 * @return The specified {@code value}
	 * 
	 * @throws IllegalArgumentException
	 *             If {@code value} is {@code null} or is greater than {@code max}
	 */
	public static <T extends Number> T validateLessThanOrEqualTo(String name, T value, T max) {
		if (value == null) {
			doThrow("The argument %s must not be null.", name);
		}

		if (value.longValue() > max.longValue()) {
			doThrow("The argument %s must not be greater than %d. Was: %d", name, value, max);
		}

		return value;
	}

	/**
	 * Validate that a number is greater than a defined maximum.
	 * 
	 * @param name
	 *            The name of the object being validated. Included in the exception message if one is thrown
	 * @param value
	 *            The value of the number
	 * @param max
	 *            The maximum that the specified {@code value} must be over
	 * 
	 * @return The specified {@code value}
	 * 
	 * @throws IllegalArgumentException
	 *             If {@code value} is {@code null} or is less than or equal to {@code max}
	 */
	public static <T extends Number> T validateGreaterThan(String name, T value, T max) {
		if (value == null) {
			doThrow("The argument %s must not be null.", name);
		}

		if (value.longValue() <= max.longValue()) {
			doThrow("The argument %s must not be less than or equal to %d. Was: %d", name, value, max);
		}

		return value;
	}

	/**
	 * Validate that a number is greater than or equal to a defined maximum.
	 * 
	 * @param name
	 *            The name of the object being validated. Included in the exception message if one is thrown
	 * @param value
	 *            The value of the number
	 * @param max
	 *            The maximum that the specified {@code value} must be over
	 * 
	 * @return The specified {@code value}
	 * 
	 * @throws IllegalArgumentException
	 *             If {@code value} is {@code null} or is less than or equal to {@code max}
	 */
	public static <T extends Number> T validateGreaterThanOrEqualTo(String name, T value, T max) {
		if (value == null) {
			doThrow("The argument %s must not be null.", name);
		}

		if (value.longValue() < max.longValue()) {
			doThrow("The argument %s must not be less than %d. Was: %d", name, value, max);
		}

		return value;
	}

	/**
	 * Validate that a number falls within a specific range. The start and end of the range are inclusive.
	 * 
	 * @param name
	 *            The name of the object being validated. Included in the exception message if one is thrown
	 * @param value
	 *            The value of the number
	 * @param start
	 *            The start of the range
	 * @param end
	 *            The end of the range
	 * 
	 * @return The specified {@code value}
	 * 
	 * @throws IllegalArgumentException
	 *             If {@code value} is {@code null} or falls outside of the range specified by {@code start} and {@code end}
	 */
	public static <T extends Number> T validateInRange(String name, T value, T start, T end) {
		if (value == null) {
			doThrow("The argument %s must not be null.", name);
		}

		long lValue = value.longValue();
		if (lValue < start.longValue() || lValue > end.longValue()) {
			doThrow("The argument %s must be in the range %d <= value <= %d. Was: %d", name, start, end, value);
		}

		return value;
	}

	private static void doThrow(String message, Object... parameters) {
		throw new IllegalArgumentException(String.format(message, parameters));
	}

	/**
	 * Determine if a particular {@link String string} is blank. A blank string is one that is either {@code null}, the empty string, or contains only
	 * whitespace characters.
	 * 
	 * @param str
	 *            The string to check
	 * 
	 * @return {@code true} if {@code str} is blank, {@code false} if it is not
	 */
	public static boolean isBlank(String str) {
		return str == null || str.trim().equals("");
	}

	/**
	 * Determine if a particular {@link String string} is {@code null}.
	 * 
	 * @param str
	 *            The string to check
	 * 
	 * @return {@code true} if {@code str} is {@code null}, {@code false} if it is not
	 */
	public static boolean isNull(String str) {
		return str == null;
	}

}
