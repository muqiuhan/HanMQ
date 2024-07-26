ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.1"

lazy val root = (project in file(".")).settings(name :=
  "HanMQ"
//    idePackagePrefix := Some("com.muqiuhan")
)

libraryDependencies += "io.netty"                    % "netty-all"       % "4.1.50.Final"
libraryDependencies += "org.apache.commons"          % "commons-lang3"   % "3.4"
libraryDependencies += "com.alibaba"                 % "fastjson"        % "1.2.52"
libraryDependencies += "ch.qos.logback"              % "logback-classic" % "1.2.10"
libraryDependencies += "org.scalatest"              %% "scalatest"       % "3.2.17" % "test"
libraryDependencies += "org.java-websocket"          % "Java-WebSocket"  % "1.3.8"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging"   % "3.9.4"
