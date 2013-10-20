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
package sun.net.www.protocol.log4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * URL protocol handler for 'log4j:...' protocol. The actual URL is ignored by the handler. The log4j.xml and log4j.dtd files in the config package are put into
 * a 'virtual' jar that can then be added to the class path in a {@link URLClassLoader}.
 */
public final class Handler extends URLStreamHandler {

	// FIXME [jim] - update log4j.xml and remove logging framework: should use place holders in log4j.xml to set the logging level, directory, etc.
	private final byte[] bytes;

	public Handler() {

		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			JarOutputStream out = new JarOutputStream(baos);
			addEntry(out, "log4j.dtd");
			addEntry(out, "log4j.xml");
			out.close();
			bytes = baos.toByteArray();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Error creating log4j config. THIS IS A BUG!", e);
		}
	}

	private void addEntry(JarOutputStream out, String name) throws Exception {

		out.putNextEntry(new JarEntry(name));
		InputStream in = ClassLoader.getSystemResourceAsStream("config/" + name);
		for (int i = in.read(); i >= 0; i = in.read()) {
			out.write(i);
		}
		out.closeEntry();
	}

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

				return new ByteArrayInputStream(bytes);
			}
		};
	}
}
