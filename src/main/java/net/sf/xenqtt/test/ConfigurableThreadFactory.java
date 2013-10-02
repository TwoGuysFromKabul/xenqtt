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
package net.sf.xenqtt.test;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link ThreadFactory} implementation that allows for configurable threads to be produced. Presently the only configuration options are the base name
 * assigned to the thread (e.g. {@code Worker}) and whether or not the thread is created as a daemon.
 */
public final class ConfigurableThreadFactory implements ThreadFactory {

	private final AtomicInteger nextId;
	private final String baseName;
	private final boolean daemon;

	/**
	 * Create a new instance of this class.
	 * 
	 * @param baseName
	 *            The base name assigned to each thread. An incrementing ID, starting at 0, is appended to the base name. The format is {@code baseName-id}
	 * @param daemon
	 *            Whether or not each thread should be created as a daemon
	 */
	public ConfigurableThreadFactory(String baseName, boolean daemon) {
		this.baseName = baseName;
		this.daemon = daemon;
		nextId = new AtomicInteger();
	}

	/**
	 * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
	 */
	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(r, baseName + "-" + nextId.getAndIncrement());
		t.setDaemon(daemon);

		return t;
	}

}
