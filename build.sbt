lazy val root = project
  .in(file("."))
  .settings(
      name         := "HanMQ",
      version      := "0.1.0-SNAPSHOT",
      scalaVersion := "3.4.2",
      libraryDependencies ++= Seq(
          "com.lihaoyi"       %% "upickle"         % "4.0.0",
          "com.outr"          %% "scribe"          % "3.15.0",
          "org.scalameta"     %% "munit"           % "1.0.0" % Test,
          "io.netty"           % "netty-all"       % "4.1.50.Final",
          "org.apache.commons" % "commons-lang3"   % "3.4",
          "ch.qos.logback"     % "logback-classic" % "1.2.10",
          "org.java-websocket" % "Java-WebSocket"  % "1.3.8"
      )
  )
