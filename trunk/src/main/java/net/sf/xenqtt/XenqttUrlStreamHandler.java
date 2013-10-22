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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * URL handler for 'xenqtt:...' protocol. The path portion of the URL is assumed to be a class path reference to a JAR packaged inside this jar.
 */
final class XenqttUrlStreamHandler extends URLStreamHandler {

	private final ClassLoader classLoader = XenqttUrlStreamHandler.class.getClassLoader();

	/**
	 * @see java.net.URLStreamHandler#openConnection(java.net.URL)
	 */
	@Override
	protected URLConnection openConnection(URL u) throws IOException {

		return new URLConnection(u) {
			@Override
			public void connect() throws IOException {
			}

			@Override
			public InputStream getInputStream() throws IOException {

				return classLoader.getResourceAsStream(url.getPath());
			}
		};
	}
}
