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
package net.sf.xenqtt.application;

/**
 * Base for {@link XenqttApplication} classes
 */
public abstract class AbstractXenqttApplication implements XenqttApplication {

	/**
	 * Uses the concrete class's name without the trailing "Application" as the application name
	 * 
	 * @see net.sf.xenqtt.application.XenqttApplication#getName()
	 */
	@Override
	public String getName() {
		return getClass().getSimpleName().replaceAll("Application$", "").toLowerCase();
	}
}
