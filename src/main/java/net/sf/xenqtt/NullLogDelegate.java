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

/**
 * No-op implementation of {@link LogDelegate}
 */
final class NullLogDelegate implements LogDelegate {

	/**
	 * @see net.sf.xenqtt.LogDelegate#trace(java.lang.String, java.lang.Object[])
	 */
	@Override
	public void trace(String message, Object... parameters) {
	}

	/**
	 * @see net.sf.xenqtt.LogDelegate#debug(java.lang.String, java.lang.Object[])
	 */
	@Override
	public void debug(String message, Object... parameters) {
	}

	/**
	 * @see net.sf.xenqtt.LogDelegate#info(java.lang.String, java.lang.Object[])
	 */
	@Override
	public void info(String message, Object... parameters) {
	}

	/**
	 * @see net.sf.xenqtt.LogDelegate#warn(java.lang.String, java.lang.Object[])
	 */
	@Override
	public void warn(String message, Object... parameters) {
	}

	/**
	 * @see net.sf.xenqtt.LogDelegate#warn(java.lang.Throwable, java.lang.String, java.lang.Object[])
	 */
	@Override
	public void warn(Throwable t, String message, Object... parameters) {
	}

	/**
	 * @see net.sf.xenqtt.LogDelegate#error(java.lang.String, java.lang.Object[])
	 */
	@Override
	public void error(String message, Object... parameters) {
	}

	/**
	 * @see net.sf.xenqtt.LogDelegate#error(java.lang.Throwable, java.lang.String, java.lang.Object[])
	 */
	@Override
	public void error(Throwable t, String message, Object... parameters) {
	}

	/**
	 * @see net.sf.xenqtt.LogDelegate#fatal(java.lang.String, java.lang.Object[])
	 */
	@Override
	public void fatal(String message, Object... parameters) {
	}

	/**
	 * @see net.sf.xenqtt.LogDelegate#fatal(java.lang.Throwable, java.lang.String, java.lang.Object[])
	 */
	@Override
	public void fatal(Throwable t, String message, Object... parameters) {
	}
}
