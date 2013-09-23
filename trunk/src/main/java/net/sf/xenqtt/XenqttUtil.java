package net.sf.xenqtt;

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

}
