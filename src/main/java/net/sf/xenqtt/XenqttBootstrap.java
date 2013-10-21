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
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * This is the entry point for xenqtt console apps. It extracts internal jars and creats a class loader that includes them. This allows us to package jars
 * within our xenqtt jar. That class loader is then use to invoke {@link Xenqtt#main(String...)}.
 */
public final class XenqttBootstrap {

	public static void main(String... args) throws Throwable {

		try {
			ClassLoader classLoader = createClassLoader();

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

	private static ClassLoader createClassLoader() {

		URL.setURLStreamHandlerFactory(new XenqttUrlStreamHandlerFactory());

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

			// If that happens then they will not be able to see the classes in our internally packages jars resulting in class not found exceptions.
			// This should be ok for all JVMs but if there are issues the solution is to write a class loader that overrides loadClass(String, boolean)
			// (generally a bad idea) and does not call the super class's implementation for class names that start with net.sf.xenqtt.
			return new URLClassLoader(urls.toArray(new URL[urls.size()]), ClassLoader.getSystemClassLoader().getParent());
		} catch (Exception e) {
			throw new RuntimeException("Failed to extract libraries", e);
		}
	}
}
