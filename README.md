# remote-akka-test
Test sending big messages via remote akka

Build the packages using sbt-native-packaging, e.g. `sbt universal:packageBin`

Copy remoteakkatestserver-1.0.zip to server, unzip, cd to bin folder, and execute:

`sh remoteakkatestserver -Dconfig.file=../conf/application.conf`

Copy remoteakkatestclient-1.0.zip to client, unzip, cd to bin folder, and execute:

`sh remoteakkatestclient -Dconfig.file=../conf/application.conf`
