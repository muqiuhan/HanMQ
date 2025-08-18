package com.muqiuhan.hanmq.core

import zio.http.WebSocketFrame
import zio.{Fiber, Queue, Ref, Runtime, Unsafe, ZIO, ZLayer}
import zio.http.ChannelEvent.Read

trait QueueManager:
  def add(name: String): ZIO[Any, Nothing, Unit]
  def put(message: String, queueName: String): ZIO[Any, Nothing, Unit]
  def signal(queueName: String): ZIO[Any, Nothing, Unit]
end QueueManager

object QueueManager:
  def add(name: String): ZIO[QueueManager, Nothing, Unit] =
    ZIO.serviceWithZIO[QueueManager](_.add(name))

  def put(message: String, queueName: String): ZIO[QueueManager, Nothing, Unit] =
    ZIO.serviceWithZIO[QueueManager](_.put(message, queueName))

  def signal(queueName: String): ZIO[QueueManager, Nothing, Unit] =
    ZIO.serviceWithZIO[QueueManager](_.signal(queueName))

  val live: ZLayer[BasicMap, Nothing, QueueManager] =
    ZLayer.fromZIO(
        for
          messageQueuesRef <- Ref.make(Map.empty[String, zio.Queue[String]])
          workersRef       <- Ref.make(Map.empty[String, Fiber.Runtime[?, ?]])
          basicMap         <- ZIO.service[BasicMap]
        yield new QueueManager:
          override def add(name: String): ZIO[Any, Nothing, Unit] =
            messageQueuesRef.get.flatMap { messageQueues =>
              messageQueues.get(name) match
                case Some(_) => ZIO.unit // Queue already exists
                case None =>
                  for
                    queue <- zio.Queue.unbounded[String]
                    _     <- messageQueuesRef.update(_ + (name -> queue))
                    _     <- ZIO.succeed(scribe.info(s"Created queue: $name"))
                    _     <- ZIO.succeed(scribe.info(s"Worker fiber started for queue: $name"))
                    fiber <- (
                        (for
                          message   <- queue.take // Blocks until a message is available
                          _         <- ZIO.succeed(scribe.debug(s"Worker fiber for queue $name received message: $message"))
                          consumers <- basicMap.getConsumers(name)
                          _         <- ZIO.succeed(scribe.debug(s"Found ${consumers.size} consumers for queue: $name"))
                          _ <- ZIO.foreachDiscard(consumers)(channel => channel.send(Read(WebSocketFrame.text(message))).ignore)
                          _ <- ZIO.when(consumers.isEmpty)(ZIO.succeed(scribe.warn(s"No consumers found for queue: $name")))
                        yield ()).forever
                    ).forkDaemon
                    _ <- workersRef.update(_ + (name -> fiber))
                  yield ()
            }

          override def put(message: String, queueName: String): ZIO[Any, Nothing, Unit] =
            for
              _ <- add(queueName) // Ensure queue exists and worker is running
              _ <- messageQueuesRef.get.flatMap(_.get(queueName) match
                case Some(queue) => queue.offer(message).unit
                case None => ZIO.succeed(scribe.error(s"Queue $queueName not found after add attempt! This should not happen."))
              )
            yield ()

          override inline def signal(queueName: String): ZIO[Any, Nothing, Unit] = add(queueName)
    )
end QueueManager
