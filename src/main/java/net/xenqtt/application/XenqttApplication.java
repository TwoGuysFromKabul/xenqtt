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
package net.xenqtt.application;

import net.xenqtt.AppContext;

/**
 * Specifies a type that implements a Xenqtt application. A Xenqtt application is an application that can be invoked via the command-line by the user. All
 * applications are instantiated every time xenqtt is run so they should not do anything significant during construction. All applications must have a no-arg
 * constructor.
 */
public interface XenqttApplication {

	/**
	 * Start the application. This method is called by the main thread that is used in launching Xenqtt.
	 * 
	 * @param appContext
	 *            The {@link AppContext context} for the application.
	 */
	void start(AppContext appContext) throws Exception;

	/**
	 * Stop the application. This method is called once the application is halted normally (e.g. {@code CTRL-c}). The application should take all appropriate
	 * shutdown actions at this point. This is called by the main thread that is used in launching Xenqtt
	 */
	void stop() throws Exception;

	/**
	 * @return The application's name for use on the command line
	 */
	String getName();

	/**
	 * The command line options string to display as part of the usage statement.
	 * 
	 * @return The opts to display
	 */
	String getOptsText();

	/**
	 * The usage description of the opts in {@link #getOptsText()}. There should be a line in the format "\n\topt : description" for each opt in the
	 * {@link #getOptsText()} string.
	 * 
	 * @return The usage text to display
	 */
	String getOptsUsageText();

	/**
	 * This is used to display a single line summary of what this application does. For use in documentation.
	 * 
	 * @return The summary display
	 */
	String getSummary();

	/**
	 * This is used to display a detailed description of what this application does. For use in documentation.
	 * 
	 * @return The description.
	 */
	String getDescription();
}
