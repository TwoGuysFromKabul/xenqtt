package net.sf.xenqtt;

/**
 * Specifies a type that implements a Xenqtt application. A Xenqtt application is an application that can be invoked via the command-line by the user.
 */
public interface XenqttApplication {

	/**
	 * Start the application. This method is called by the main thread that is used in launching Xenqtt.
	 * 
	 * @param arguments
	 *            The {@link ApplicationArguments arguments} that were supplied. This includes both normal arguments (e.g. {@code -p port}) and flags (e.g. {@code -a})
	 */
	void start(ApplicationArguments arguments);

	/**
	 * Stop the application. This method is called once the application is halted normally (e.g. {@code CTRL-c}). The application should take all appropriate
	 * shutdown actions at this point. This is called by the main thread that is used in launching Xenqtt
	 */
	void stop();

	/**
	 * Get usage text to display to the user. This is used whenever the user invokes the help option ({@code java -jar xenqtt.jar --help application}) or if the
	 * user supplies invalid arguments and/or flags to a specific application.
	 * 
	 * @return The usage text to display
	 */
	String getUsageText();

}
