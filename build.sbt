import Dependencies.*

ThisBuild / scalaVersion := "3.7.2"
ThisBuild / version      := "0.3.0"
ThisBuild / organization := "com.muqiuhan"

lazy val root = project
  .in(file("."))
  .aggregate(
    hanmqCore,
    hanmqProtocol,
    hanmqProtocolJson,
    hanmqRouting,
    hanmqPersistence,
    hanmqPersistenceMemory,
    hanmqNetwork,
    hanmqServer,
    hanmqMetrics,
    hanmqManagement,
    hanmqClient
  )
  .settings(
    name := "HanMQ",
    publish / skip := true,
    // 保留旧代码的编译能力，逐步迁移
    libraryDependencies ++= Seq(
      zio,
      zioHttp,
      upickle,
      scribe,
      munit,
      netty,
      commonsLang,
      logback,
      websocket
    )
  )

lazy val hanmqCore = project
  .in(file("modules/hanmq-core"))
  .settings(
    name := "hanmq-core",
    libraryDependencies ++= Seq(
      zio,
      munit
    )
  )

lazy val hanmqProtocol = project
  .in(file("modules/hanmq-protocol"))
  .dependsOn(hanmqCore)
  .settings(
    name := "hanmq-protocol",
    libraryDependencies ++= Seq(
      zio,
      munit
    )
  )

lazy val hanmqProtocolJson = project
  .in(file("modules/hanmq-protocol/json"))
  .dependsOn(hanmqProtocol, hanmqCore)
  .settings(
    name := "hanmq-protocol-json",
    libraryDependencies ++= Seq(
      upickle,
      munit
    )
  )

lazy val hanmqRouting = project
  .in(file("modules/hanmq-routing"))
  .dependsOn(hanmqCore)
  .settings(
    name := "hanmq-routing",
    libraryDependencies ++= Seq(
      zio,
      munit
    )
  )

lazy val hanmqPersistence = project
  .in(file("modules/hanmq-persistence"))
  .dependsOn(hanmqCore)
  .settings(
    name := "hanmq-persistence",
    libraryDependencies ++= Seq(
      zio,
      munit
    )
  )

lazy val hanmqPersistenceMemory = project
  .in(file("modules/hanmq-persistence/memory"))
  .dependsOn(hanmqCore)
  .settings(
    name := "hanmq-persistence-memory",
    libraryDependencies ++= Seq(
      zio,
      munit
    )
  )

lazy val hanmqNetwork = project
  .in(file("modules/hanmq-network"))
  .dependsOn(hanmqCore, hanmqProtocol)
  .settings(
    name := "hanmq-network",
    libraryDependencies ++= Seq(
      zio,
      zioHttp,
      munit
    )
  )

lazy val hanmqMetrics = project
  .in(file("modules/hanmq-metrics"))
  .dependsOn(hanmqCore)
  .settings(
    name := "hanmq-metrics",
    libraryDependencies ++= Seq(
      zio,
      munit
    )
  )

lazy val hanmqManagement = project
  .in(file("modules/hanmq-management"))
  .dependsOn(hanmqCore, hanmqMetrics)
  .settings(
    name := "hanmq-management",
    libraryDependencies ++= Seq(
      zio,
      zioHttp,
      munit
    )
  )

lazy val hanmqServer = project
  .in(file("modules/hanmq-server"))
  .dependsOn(
    hanmqCore,
    hanmqProtocol,
    hanmqProtocolJson,
    hanmqRouting,
    hanmqPersistence,
    hanmqPersistenceMemory,
    hanmqNetwork,
    hanmqMetrics,
    hanmqManagement
  )
  .settings(
    name := "hanmq-server",
    libraryDependencies ++= Seq(
      zio,
      zioHttp,
      upickle,
      scribe,
      logback,
      munit
    )
  )

lazy val hanmqClient = project
  .in(file("modules/hanmq-client/scala"))
  .dependsOn(hanmqProtocol, hanmqProtocolJson, hanmqNetwork)
  .settings(
    name := "hanmq-client",
    libraryDependencies ++= Seq(
      zio,
      websocket,
      upickle,
      munit
    )
  )
