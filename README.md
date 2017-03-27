# remote-akka-test
Test sending big messages via remote akka

Build the packages using sbt-native-packaging, e.g. `sbt universal:packageBin`

Unzip remoteakkatestserver-1.0.zip, cd to bin and execute on server:

`sh remoteakkatestserver -Dconfig.file=../conf/application.conf`

Unzip remoteakkatestclient-1.0.zip, cd to bin and execute on client:

`sh remoteakkatestclient -Dconfig.file=../conf/application.conf`
