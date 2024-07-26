<div align="center">

# HanMQ

*A simple message queue based on netty, complete routing distribution using basic topic mode and written in Scala3*

![Scala 3.4.2](https://img.shields.io/badge/Scala3.3.0-%23DC322F)

![Build and Run](https://github.com/muqiuhan/HanMQ/actions/workflows/BuildAndTest.yaml/badge.svg)

<img src="./.github/demo.png">

</div>

## Introduction
HanMQ communication model is based on RabbitMQ's most basic Topics model:

![](./.github/rabbitmq-topic-mode.png)

The message queue is abstracted into Server and Client:

The thread group design of Server is mainly divided into three parts:
1. Handle network communication module
2. Put the message from the producer into the corresponding queue
3. Distribute messages in each queue to consumer

The basic model is a blocking queue, which implemented using BlockingQueue under the Java J.U.C package.

HanMQ has customized a simple message protocol. In order to facilitate expansion and take advantage of some existing application layer protocols, the WebSocket protocol is adopted:

```scala
/// Message protocol format:
/// consumer's subscription registration message:
///     { type: 0, extend: ["queue_name1","queue_name1"] }
/// General messages from the producer:
///     { type: 1, content: "message content", extend: "routing key" }
case class Message(
    typ: Int,
    content: String,

    /// If type is 0, extend is the queueName from the consumer, specifying which queue to connect to.
    /// If type is 1, extend is the routingKey from the producer
    extend: String,

    /// The time the message was sent
    date: String
)
```

## Build & Test & Run

`sbt run` then `sbt test`

## Dependencies

```scala 3
...
      libraryDependencies ++= Seq(
          "com.lihaoyi"       %% "upickle"         % "4.0.0",
          "com.outr"          %% "scribe"          % "3.15.0",
          "io.netty"           % "netty-all"       % "4.1.50.Final",
          "org.apache.commons" % "commons-lang3"   % "3.4",
          "com.alibaba"        % "fastjson"        % "1.2.52",
          "ch.qos.logback"     % "logback-classic" % "1.2.10",
          "org.scalatest"     %% "scalatest"       % "3.2.17" % "test",
          "org.java-websocket" % "Java-WebSocket"  % "1.3.8"
      )
...
```

## Reference
- [RabbitMQ3.5.3 source code commented version for easy reading)](https://github.com/sky-big/RabbitMQ)
- [RabbitMQ Documentation](https://www.rabbitmq.com/documentation.html)
- [MiniMQ](https://github.com/Mr-Hades1/minimq)

## Acknowledge
- Thanks to JetBrains for providing free IntelliJ IDEA IDE licenses for open source projects
  <img src="https://resources.jetbrains.com/storage/products/company/brand/logos/jb_beam.svg" height="100px">

## LICENSE
The MIT License (MIT)

Copyright (c) 2023 Muqiu Han

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
