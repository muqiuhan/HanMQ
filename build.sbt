lazy val root = project
  .in(file("."))
  .settings(
      name         := "HanMQ",
      version      := "0.1.0-SNAPSHOT",
      scalaVersion := "3.4.2",
      libraryDependencies ++= Seq(
          "com.lihaoyi"                %% "upickle"         % "4.0.0",
          "io.netty"                    % "netty-all"       % "4.1.50.Final",
          "org.apache.commons"          % "commons-lang3"   % "3.4",
          "com.alibaba"                 % "fastjson"        % "1.2.52",
          "ch.qos.logback"              % "logback-classic" % "1.2.10",
          "org.scalatest"              %% "scalatest"       % "3.2.17" % "test",
          "org.java-websocket"          % "Java-WebSocket"  % "1.3.8",
          "com.typesafe.scala-logging" %% "scala-logging"   % "3.9.4"
      )
  )
