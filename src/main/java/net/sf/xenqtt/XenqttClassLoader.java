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

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Class loader that attempts to load classes from the provided URLs before loading from the "parent" class loader.
 */
final class XenqttClassLoader extends URLClassLoader {

	private final ClassLoader delegate;

	public XenqttClassLoader(URL[] urls, ClassLoader parent) {
		super(urls, null);
		this.delegate = parent;
	}

	/**
	 * @see java.net.URLClassLoader#findClass(java.lang.String)
	 */
	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {

		try {
			return super.findClass(name);
		} catch (NoClassDefFoundError e) {
			return delegate.loadClass(name);
		}
	}
}
