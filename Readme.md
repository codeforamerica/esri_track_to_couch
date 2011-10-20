# Esri Tracking Server to Couch daemon.

A Free Software project to subscribe to an Esri Tracking Server feed, and post observed events to a Couch database.

## Dependencies

You need to have the Esri connector sdk downloaded. You can get that [here](http://help.arcgis.com/en/trackingserver/10.0/java/index.html)

This requires an Esri account, and is subject to their license.

Unzip this file and copy ConnectorApi/lib/JavaConnectorAPI.jar to esriconnector.jar

## Compilation

    javac -cp "commons-codec.jar:commons-logging.jar:esriconnector.jar:json.jar:httpclient.jar:httpcore.jar" FollowService.java 

For both compilation and running, as an alternative to setting the classpath with -cp, you can unzip the jar files.
## Running

    javac -cp "commons-codec.jar:commons-logging.jar:esriconnector.jar:json.jar:httpclient.jar:httpcore.jar:./" FollowService <tracking server host> <tracking server port> <destination URI> <destination username> <destination password>
