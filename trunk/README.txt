The xenqtt.jar in each release folder is the same as the xenqtt-version.jar. It is supplied for convenience 
when running XenQTT applications. The versioned JAR is better suited when using it as a dependency in another 
application.
 
0.9.2
	Added max in-flight messages configuration to MQTT client.
	Moved all command line application logs to ./logs directory.
	Added command line option to log to the console for command line applications.
	Added max in-flight messages command line option to the proxy.
	Updated client and proxy to be sure the same message ID is never reused before it is acknowledged.
	
0.9.1
	Fixed a bug where ping requests were not always sent to the broker properly resulting in disconnection.
	Updated command line help to be more... helpful
	
0.9.0
	Added the clustering proxy.
	Added statistics to the client API.
	Added received timestamp to PublishMessage for use by clients.
	Added the MqttClientDebugListener interface clients can use to have visibility into the MQTT messaging layers for debugging.
	Added examples of client and mock broker API use cases in the package net.sf.xenqtt.examples.
	Made the test client more robust.
	Fixed various small bugs.
	
0.8.2
	Updated the client API to make it simpler to use and make type names more consistent.
	Fixed memory leak in the asynchronous client 
	Fixed various other minor bugs

0.8.1
    Simplified Client configuration.
	Fixed all known bugs in Client and Mock Broker.
	Added features to Client and Mock Broker.
	Performed various load tests successfully.
	
0.8.0
	Fixed all known bugs in Client and Mock Broker.
	Added more functional/integration test cases.
	Testing publishing to central maven repo 
	Load tests pending before 1.0.0 release.
	
0.5.0
	Client: 
		Feature complete and functionally tested. 
		Load tests pending before 1.0.0 release
	Mock Broker: 
		Feature complete with some minor bugs. 
		Should be usable for most testing scenarios. 
		Load tests pending before 1.0.0 release
	
0.0.1-SNAPSHOT
	Initial release. This should not be used for anything other than API evaluation.