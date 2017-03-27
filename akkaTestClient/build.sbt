
name := "RemoteAkkaTestClient"

version := "1.0"

scalaVersion := "2.12.1"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
  "com.typesafe.akka"            %% "akka-remote"               % "2.4.17",
  "com.typesafe.akka"            %% "akka-actor"                % "2.4.17",
  "com.typesafe.akka"            %% "akka-slf4j"                % "2.4.17"
)

enablePlugins(JavaServerAppPackaging)