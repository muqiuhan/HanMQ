package com.muqiuhan.hanmq.core

import zio.http.WebSocketChannel
import zio.Queue
import zio.Ref
import zio.ZIO
import zio.ZLayer
import zio.Fiber

trait BasicMap:
  def addConsumer(queueName: String, channel: WebSocketChannel): ZIO[Any, Nothing, Unit]
  def getConsumers(queueName: String): ZIO[Any, Nothing, Set[WebSocketChannel]]
  def removeConsumer(queueName: String, channel: WebSocketChannel): ZIO[Any, Nothing, Unit]
  def removeChannelFromAllQueues(channel: WebSocketChannel): ZIO[Any, Nothing, Unit]
end BasicMap

object BasicMap:
  inline def addConsumer(queueName: String, channel: WebSocketChannel): ZIO[BasicMap, Nothing, Unit] =
    ZIO.serviceWithZIO[BasicMap](_.addConsumer(queueName, channel))

  inline def getConsumers(queueName: String): ZIO[BasicMap, Nothing, Set[WebSocketChannel]] =
    ZIO.serviceWithZIO[BasicMap](_.getConsumers(queueName))

  inline def removeConsumer(queueName: String, channel: WebSocketChannel): ZIO[BasicMap, Nothing, Unit] =
    ZIO.serviceWithZIO[BasicMap](_.removeConsumer(queueName, channel))

  inline def removeChannelFromAllQueues(channel: WebSocketChannel): ZIO[BasicMap, Nothing, Unit] =
    ZIO.serviceWithZIO[BasicMap](_.removeChannelFromAllQueues(channel))

  val live: ZLayer[Any, Nothing, BasicMap] =
    ZLayer.fromZIO(
        Ref.make(Map.empty[String, Ref[Set[WebSocketChannel]]]).map { ref =>
          new BasicMap:
            override def addConsumer(queueName: String, channel: WebSocketChannel): ZIO[Any, Nothing, Unit] =
              ref.get.flatMap { map =>
                map.get(queueName) match
                  case Some(consumerSetRef) => consumerSetRef.update(_ + channel)
                  case None =>
                    Ref.make(Set(channel)).flatMap { newConsumerSetRef =>
                      ref.update(_ + (queueName -> newConsumerSetRef))
                    }
              }.unit

            override inline def getConsumers(queueName: String): ZIO[Any, Nothing, Set[WebSocketChannel]] =
              ref.get.flatMap(_.get(queueName).map(_.get).getOrElse(ZIO.succeed(Set.empty)))

            override inline def removeConsumer(queueName: String, channel: WebSocketChannel): ZIO[Any, Nothing, Unit] =
              ref.get.flatMap(_.get(queueName).map(_.update(_ - channel)).getOrElse(ZIO.unit))

            override inline def removeChannelFromAllQueues(channel: WebSocketChannel): ZIO[Any, Nothing, Unit] =
              ref.get.flatMap { map =>
                ZIO.foreachDiscard(map.values)(consumerSetRef =>
                  consumerSetRef.update(_ - channel)
                )
              }
        }
    )
end BasicMap
