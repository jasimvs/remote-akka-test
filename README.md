# remote-akka-test
Test sending big messages via remote akka

Build the packages using sbt-native-packaging, e.g. sbt universal:packageBin

unzip, cd to bin and execute 
sh remoteakkatestserver -Dconfig.file=../conf/application.conf
sh remoteakkatestclient -Dconfig.file=../conf/application.conf
