ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.1"

lazy val root = (project in file("."))
  .settings(
    name := "HanMQ"
//    idePackagePrefix := Some("com.muqiuhan")
  )

libraryDependencies += "io.netty" % "netty-all" % "4.1.50.Final"
libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.4"
libraryDependencies += "org.slf4j" % "slf4j-log4j12" % "1.7.26"
libraryDependencies += "com.alibaba" % "fastjson" % "1.2.52"
libraryDependencies += "junit" % "junit" % "4.13.2"
libraryDependencies += "org.java-websocket" % "Java-WebSocket" % "1.3.8"
libraryDependencies += "org.projectlombok" % "lombok" % "1.18.20"
