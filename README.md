<div align="center">

<img src="./.github/logo.webp" height="150px">

# HanMQ

*Lightweight message queue based on ZIO and WebSocket* 

![Scala 3.7.2](https://img.shields.io/badge/Scala3.7.2-%23DC322F) [![Build](https://github.com/muqiuhan/HanMQ/actions/workflows/Build.yaml/badge.svg)](https://github.com/muqiuhan/HanMQ/actions/workflows/Build.yaml)

</div>

## Introduction
HanMQ communication model is based on RabbitMQ's most basic Topics model:

![](./.github/rabbitmq-topic-mode.png)

The message queue is abstracted into Server and Client:

The original server implementation used Netty directly for low-level network communication and thread management. The current server leverages [ZIO HTTP](https://zio.dev/reference/http/) for a more modern, functional, and asynchronous approach to handle WebSocket connections and message routing. This transition enables:
1.  **Efficient Concurrency**: ZIO's lightweight fibers replace traditional threads, allowing thousands of concurrent operations with minimal overhead.
2.  **Resource Safety**: ZIO's `Scope` ensures proper management and release of resources like network connections.
3.  **Composability**: Building the application from small, testable, and composable ZIO effects.

The core logic for message handling and queue management is now powered by ZIO's `Ref` and `Queue` primitives within the `BasicMap` and `QueueManager` services, respectively. These services are provided as ZLayers, enabling dependency injection and testability.

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

## Usage

server:
```scala 3
@main
def main: Unit =
  com.muqiuhan.hanmq.server.Server.run.provide(
      zio.ZIOAppArgs.empty,
      zio.Scope.default,
      com.muqiuhan.hanmq.core.BasicMap.live,
      com.muqiuhan.hanmq.core.QueueManager.live,
      zio.http.Server.default
  ).exitCode.run.currentOrThrow.run.exitCode
```

client:
```scala 3
import com.muqiuhan.hanmq.client.{Producer, Consumer}
...
  val url = "ws://localhost:9993/";

  test("Test client") {
    val producer1 = new Producer(URI.create(url), "producer_1");
    val producer2 = new Producer(URI.create(url), "producer_2");
    val producer3 = new Producer(URI.create(url), "producer_3");
    val consumer1 = new Consumer(URI.create(url), "American");
    val consumer2 = new Consumer(URI.create(url), "China");
    val consumer3 = new Consumer(URI.create(url), "UK");

    producer1.send("Make America Great Again!", "American.great.again.!");
    producer2.send("China is getting stronger!", "China.daily.com");
    producer2.send("China sees 14.3 percent more domestic trips in H1", "China.xinhua.net");
    producer3.send("The voice from Europe", "UK.Reuters.com");

    consumer1.register("American", true);
    consumer1.onMessage(message => scribe.info("American: " + message));
    consumer2.register("China", true);
    consumer2.onMessage(message => scribe.info("China: " + message));
    consumer3.register("UK", true);
    consumer3.onMessage(message => scribe.info("UK: " + message));
...
```

output:
```
2024.07.27 10:25:54:496 WebSocketConnectReadThread-173 INFO com.muqiuhan.hanmq.client.Client.onOpen:20
    Client producer_1 connects successfully    
2024.07.27 10:25:54:520 WebSocketConnectReadThread-176 INFO com.muqiuhan.hanmq.client.Client.onOpen:20
    Client producer_2 connects successfully    
2024.07.27 10:25:54:527 WebSocketConnectReadThread-179 INFO com.muqiuhan.hanmq.client.Client.onOpen:20
    Client producer_3 connects successfully    
2024.07.27 10:25:54:538 WebSocketConnectReadThread-182 INFO com.muqiuhan.hanmq.client.Client.onOpen:20
    Client American connects successfully    
2024.07.27 10:25:54:544 WebSocketConnectReadThread-185 INFO com.muqiuhan.hanmq.client.Client.onOpen:20
    Client China connects successfully    
2024.07.27 10:25:54:550 WebSocketConnectReadThread-188 INFO com.muqiuhan.hanmq.client.Client.onOpen:20
    Client UK connects successfully    
2024.07.27 10:25:54:636 WebSocketConnectReadThread-185 INFO <empty>.TestClient.TestClient:24
    China: "China is getting stronger!"    
2024.07.27 10:25:54:636 WebSocketConnectReadThread-188 INFO <empty>.TestClient.TestClient:26
    UK: "The voice from Europe"    
2024.07.27 10:25:54:636 WebSocketConnectReadThread-182 INFO <empty>.TestClient.TestClient:22
    American: "Make America Great Again!"    
2024.07.27 10:25:54:636 WebSocketConnectReadThread-185 INFO <empty>.TestClient.TestClient:24
    China: "China sees 14.3 percent more domestic trips in H1"
```

## Dependencies

```scala 3
...
libraryDependencies ++= Seq(
    "com.lihaoyi"       %% "upickle"         % "4.0.0",
    "com.outr"          %% "scribe"          % "3.15.0",
    "org.scalameta"     %% "munit"           % "1.0.0" % Test,
    "io.netty"           % "netty-all"       % "4.2.4.Final",
    "org.apache.commons" % "commons-lang3"   % "3.4",
    "ch.qos.logback"     % "logback-classic" % "1.2.10",
    "org.java-websocket" % "Java-WebSocket"  % "1.3.8",
    "dev.zio"           %% "zio"             % "2.1.20",
    "dev.zio"           %% "zio-http"        % "3.3.3"
)
...
```

## Future Work

### Zombie Queue Problem

If a message queue is dynamically created during the application lifecycle and is no longer needed, its corresponding worker `Fiber` continues to run, constantly attempting to `take` messages from a queue that may no longer receive them. This can lead to unnecessary CPU cycles and context switching, constituting a logical resource leak.

### Persistence and Durability

Implement a pluggable persistence layer, this is the most critical feature of a production message queue. Messages must survive broker restarts and failures.

### Protocol and Serialization

The current JSON-based protocol is readable but can be a performance bottleneck.

Use schema-based serialization formats such as [Protocol Buffers](https://protobuf.dev/) or [Avro](https://avro.apache.org/).

### Message Delivery Guarantees

Implement message acknowledgments (ACK).

### Advanced Features

- Exchanges and Advanced Routing: The current routing is a simplified version of topic exchange. Implement different types of exchanges, as seen in RabbitMQ.

- Dead-Letter Queues (DLQ): When messages are rejected multiple times or cannot be routed, they should not be discarded but sent to a special "dead-letter queue." This allows for subsequent inspection and manual intervention.

- Message TTL (Time-To-Live): Allow producers to set an expiration time for messages. If a message is not consumed within its TTL, it should be discarded or moved to a DLQ.

### Configuration and Management

-  Replace `java.util.Properties` with a more powerful configuration library, such as [PureConfig](https://pureconfig.github.io/) (based on HOCON/Typesafe Config). This provides type-safe configuration access.

#### Management and Monitoring

- Expose broker metrics (e.g., queue depth, message rate, memory usage) using libraries like [Micrometer](https://micrometer.io/) or [Dropwizard Metrics](https://metrics.dropwizard.io/). These metrics can be scraped by monitoring systems like Prometheus.

### Clustering and High Availability

For a truly production-ready system, it needs to be able to run as a cluster.

Use a coordination service like [ZooKeeper](https://zookeeper.apache.org/), or a library implementing the [Raft consensus algorithm](https://raft.github.io/) for leader election and metadata management.

## Reference
- [RabbitMQ3.5.3 source code commented version for easy reading)](https://github.com/sky-big/RabbitMQ)
- [RabbitMQ Documentation](https://www.rabbitmq.com/documentation.html)
- [MiniMQ](https://github.com/Mr-Hades1/minimq)

## LICENSE
The MIT License (MIT)

Copyright (c) 2023 - 2025 Somhairle H. Marisol

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