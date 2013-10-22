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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import net.sf.xenqtt.ArgumentExtractor.Arguments;

/**
 * This is the entry point for xenqtt console apps. It extracts internal jars and creates a class loader that includes them. This allows us to package jars
 * within our xenqtt jar. That class loader is then use to invoke {@link Xenqtt#main(String...)}.
 */
public final class XenqttBootstrap {

	public static void main(String... args) throws Throwable {

		Arguments arguments = ArgumentExtractor.extractArguments(null, args);

		LoggingLevels loggingLevels = arguments != null ? arguments.determineLoggingLevels() : new LoggingLevels(LoggingLevels.DEFAULT_LOGGING_LEVELS);
		String appName = arguments.applicationName;
		boolean consoleLogger = arguments.isConsoleLoggingSpecified();

		try {
			ClassLoader classLoader = createClassLoader(loggingLevels, appName, consoleLogger);

			Class<?> xenqttClass = classLoader.loadClass("net.sf.xenqtt.Xenqtt");
			Method mainMethod = xenqttClass.getMethod("main", new Class<?>[] { String[].class });

			Thread.currentThread().setContextClassLoader(classLoader);
			mainMethod.invoke(null, new Object[] { args });

		} catch (InvocationTargetException e) {
			if (e.getCause() == null) {
				throw e;
			} else {
				throw e.getCause();
			}
		}
	}

	private static ClassLoader createClassLoader(LoggingLevels loggingLevels, String appName, boolean consoleLogger) {

		URL.setURLStreamHandlerFactory(new XenqttUrlStreamHandlerFactory(loggingLevels, appName, consoleLogger));

		try {
			List<String> jars = XenqttUtil.findFilesOnClassPath("net.sf.xenqtt.lib", ".jar");
			if (jars.isEmpty()) {
				return Xenqtt.class.getClassLoader();
			}

			List<URL> urls = new ArrayList<URL>();
			urls.add(XenqttUtil.getXenqttClassPathRoot().toURI().toURL());
			for (String jar : jars) {
				urls.add(new URL("xenqtt:" + jar));
			}
			urls.add(new URL("log4j:log4jconfig.jar"));

			return new XenqttClassLoader(urls.toArray(new URL[urls.size()]), XenqttBootstrap.class.getClassLoader());
		} catch (Exception e) {
			throw new RuntimeException("Failed to extract libraries", e);
		}
	}
}
