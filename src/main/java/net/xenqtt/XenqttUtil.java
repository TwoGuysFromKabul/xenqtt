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

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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
			doThrow("The argument %s must be less than %d. Was: %d", name, max, value);
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
			doThrow("The argument %s must less than or equal to %d. Was: %d", name, max, value);
		}

		return value;
	}

	/**
	 * Validate that a number is greater than a defined minimum.
	 * 
	 * @param name
	 *            The name of the object being validated. Included in the exception message if one is thrown
	 * @param value
	 *            The value of the number
	 * @param min
	 *            The minimum that the specified {@code value} must be over
	 * 
	 * @return The specified {@code value}
	 * 
	 * @throws IllegalArgumentException
	 *             If {@code value} is {@code null} or is less than or equal to {@code max}
	 */
	public static <T extends Number> T validateGreaterThan(String name, T value, T min) {
		if (value == null) {
			doThrow("The argument %s must not be null.", name);
		}

		if (value.longValue() <= min.longValue()) {
			doThrow("The argument %s must greater than %d. Was: %d", name, min, value);
		}

		return value;
	}

	/**
	 * Validate that a number is greater than or equal to a defined minimum.
	 * 
	 * @param name
	 *            The name of the object being validated. Included in the exception message if one is thrown
	 * @param value
	 *            The value of the number
	 * @param min
	 *            The minimum that the specified {@code value} must be over
	 * 
	 * @return The specified {@code value}
	 * 
	 * @throws IllegalArgumentException
	 *             If {@code value} is {@code null} or is less than or equal to {@code max}
	 */
	public static <T extends Number> T validateGreaterThanOrEqualTo(String name, T value, T min) {
		if (value == null) {
			doThrow("The argument %s must not be null.", name);
		}

		if (value.longValue() < min.longValue()) {
			doThrow("The argument %s must be greater than or equal to %d. Was: %d", name, min, value);
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

	/**
	 * Converts tabs to 4 spaces and optionally word wraps text at about 100 characters then prints to system.out and appends a newline character. Lines will be
	 * continued past the specified length to avoid breaking in the middle of a word.
	 */
	public static void prettyPrintln(String text, boolean wrap) {
		prettyPrint(text + "\n", wrap);
	}

	/**
	 * Loads the specified file as a class path resource and returns it as a string.
	 * 
	 * @return The contents of the specified resource
	 */
	public static String loadResourceFile(String resourceName) {
		resourceName = resourceName.charAt(0) == '/' ? resourceName : String.format("/%s", resourceName);
		InputStream in = Xenqtt.class.getResourceAsStream(resourceName);
		if (in == null) {
			System.err.println("Unable to load the requested resource. This is a bug!");
			return null;
		}

		StringBuilder resource = new StringBuilder();
		byte[] buffer = new byte[8192];
		int bytesRead = -1;
		try {
			while ((bytesRead = in.read(buffer)) != -1) {
				resource.append(new String(buffer, 0, bytesRead));
			}
			in.close();
		} catch (Exception ex) {
			System.err.println("Unable to load the help documentation. This is a bug!");
			ex.printStackTrace();
			return null;
		}

		return resource.toString();
	}

	/**
	 * Converts tabs to 4 spaces and optionally word wraps text at about 100 characters then prints to system.out. Lines will be continued past the specified
	 * length to avoid breaking in the middle of a word.
	 */
	public static void prettyPrint(String text, boolean wrap) {

		StringBuilder prettyText = new StringBuilder();
		StringBuilder currentLine = new StringBuilder();
		int currentLineSize = 0;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c != '\t') {
				currentLine.append(c);
				currentLineSize++;
			} else {
				currentLine.append("    ");
				currentLineSize += 4;
			}

			if (c == '\n') {
				prettyText.append(currentLine.toString());
				currentLine = new StringBuilder();
				currentLineSize = 0;
				continue;
			}

			if (wrap && currentLineSize > 100) {
				if (c == ' ') {
					prettyText.append(currentLine.toString());
					currentLine = new StringBuilder();
				} else {
					int lastWhitespace = currentLine.lastIndexOf(" ");
					String nextLine = currentLine.substring(lastWhitespace + 1);
					prettyText.append(currentLine.substring(0, lastWhitespace));
					currentLine = new StringBuilder(nextLine);
				}
				prettyText.append('\n');
				currentLineSize = 0;
			}
		}
		prettyText.append(currentLine.toString());

		System.out.print(prettyText);
	}

	/**
	 * @return The xenqtt class path root. If xenqtt is running from a JAR it will be the JAR. Otherwise it will be the class path directory that contains the
	 *         net/sf/xenqtt package. The latter is to support running in development which typically means it will be the 'project/target/classes' directory.
	 */
	public static File getXenqttClassPathRoot() {

		URL url = Xenqtt.class.getResource("/" + XenqttUtil.class.getName().replace('.', '/') + ".class");
		if (url == null) {
			throw new RuntimeException("Unable to find Xenqtt class resource. THIS IS A BUG!");
		}

		String path = url.getPath();
		// uncomment this and edit it to point to an existing xenqtt jar to manually test getting the classpath root from the jar
		// path = "target/xenqtt-0.9.2-SNAPSHOT.jar!/net/sf/xenqtt/XenqttUtil.class";
		int startIndex = path.indexOf(":");
		if (startIndex >= 0) {
			path = path.substring(startIndex + 1);
		}

		int bangIndex = path.indexOf(".jar!");
		if (bangIndex >= 0) {
			path = path.substring(0, bangIndex + 4);
		} else {
			int end = path.length() - XenqttUtil.class.getName().length() - ".class".length() - 1;
			path = path.substring(0, end);
		}

		return new File(path);
	}

	/**
	 * @return The directory where xenqtt is currently installed. If xenqtt is running from a JAR it will be the directory containing the JAR. Otherwise it will
	 *         be the parent directory of the class path directory that contains the net/sf/xenqtt package. The latter is to support running in development
	 *         which typically means it will be the 'project/target' directory.
	 */
	public static File getXenqttInstallDirectory() {

		return getXenqttClassPathRoot().getParentFile();
	}

	/**
	 * Finds files on the xenqtt class path optionally limited to package and/or extension.
	 * 
	 * @param packageName
	 *            Name of package to limit the search to. Sub packages are not included. Null to include all packages.
	 * @param extension
	 *            File extension to limit search to. This should include the dot as in ".class". Null to include files with any extension.
	 * 
	 * @return The list of files found. Each entry will be '/' separated, relative to the class path, and will have no leading '/'.
	 */
	public static List<String> findFilesOnClassPath(String packageName, String extension) {

		try {
			String cpRoot = getXenqttClassPathRoot().getAbsolutePath();

			ClassPathFileListBuilder builder;
			if (cpRoot.endsWith(".jar")) {
				builder = new ClassPathFileListBuilder(null, packageName, extension);
				findFilesInJar(cpRoot, builder);
			} else {
				builder = new ClassPathFileListBuilder(cpRoot, packageName, extension);
				findFilesInDir(new File(cpRoot), builder);
			}

			return builder.files;

		} catch (Exception e) {
			throw new RuntimeException("Failed to find files in package: " + packageName, e);
		}
	}

	private static void findFilesInJar(String jarPath, ClassPathFileListBuilder builder) throws Exception {

		JarFile jarFile = new JarFile(jarPath);
		try {
			Enumeration<JarEntry> entries = jarFile.entries();
			while (entries.hasMoreElements()) {
				JarEntry entry = entries.nextElement();
				builder.accept(entry.getName());
			}
		} finally {
			jarFile.close();
		}
	}

	private static void findFilesInDir(File dirFile, ClassPathFileListBuilder builder) throws Exception {

		File[] files = dirFile.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				findFilesInDir(file, builder);
			} else {
				builder.accept(file.getAbsolutePath());
			}
		}
	}

	private final static class ClassPathFileListBuilder {

		private final int rootDirLength;
		private final List<String> files = new ArrayList<String>();
		private final String filterPackagePath;
		private final String filterExtension;

		public ClassPathFileListBuilder(String dir, String filterPackageName, String filterExtension) {

			this.rootDirLength = dir == null ? 0 : new File(dir).getAbsolutePath().length() + 1;
			this.filterPackagePath = filterPackageName == null ? null : filterPackageName.replace('.', '/');
			this.filterExtension = filterExtension;
		}

		void accept(String file) {

			if (rootDirLength > 0) {
				file = file.substring(rootDirLength, file.length());
				file = file.replace('\\', '/');
			}

			boolean accepted = filterPackagePath == null || (file.startsWith(filterPackagePath) && filterPackagePath.length() == file.lastIndexOf('/'));
			accepted &= (filterExtension == null || file.endsWith(filterExtension));

			if (accepted) {
				files.add(file);
			}
		}
	}
}
