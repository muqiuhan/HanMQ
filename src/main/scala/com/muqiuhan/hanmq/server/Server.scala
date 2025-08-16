package com.muqiuhan.hanmq.server

import com.muqiuhan.hanmq.core.{BasicMap, QueueManager}
import com.muqiuhan.hanmq.legacy.message.Message
import zio.*
import zio.http.*
import zio.http.ChannelEvent.*
import zio.http.Server as ZIOHttpServer
import scala.util.Try
import scala.collection.mutable.Queue
import upickle.default as Upickle
import com.muqiuhan.hanmq.legacy.utils.Banner
import com.muqiuhan.hanmq.legacy.config.Config

object Server extends ZIOAppDefault:

  private def route(data: String, channel: WebSocketChannel): Task[Unit] =
    ZIO.attempt {
      val message = Upickle.read[Message](data)
      message.typ match
        case 0 /* Subscription */ => processConsumerMessage(channel, message)
        case 1 /* Producer */     => processProducerMessage(message)
        case typ: Int             => throw new Exception(s"Unknown message type: $typ")
      end match
    }

  private def processConsumerMessage(channel: WebSocketChannel, message: Message): Unit =
    for queueName <- Upickle.read[List[String]](message.extend) do
      if !BasicMap.queueConsumerMap.containsKey(queueName) then
        val channels = new Queue[WebSocketChannel]()
        channels.addOne(channel)
        BasicMap.queueConsumerMap.put(queueName, channels)
      else
        BasicMap.queueConsumerMap.get(queueName).addOne(channel)
      end if
      QueueManager.signal(queueName)
    end for
  end processConsumerMessage

  private inline def processProducerMessage(message: Message): Unit =
    QueueManager.put(message.content, message.extend)

  private def wrongMessageFormat(channel: WebSocketChannel, data: String): UIO[Unit] =
    for
      _ <- ZIO.succeed(scribe.error(s"Wrong message format: $data"))
      _ <- channel.send(Read(WebSocketFrame.text("Wrong message format"))).orDie
      _ <- channel.shutdown
    yield ()

  private def handleDisconnect(channel: WebSocketChannel): UIO[Unit] =
    ZIO.succeed {
      BasicMap.queueConsumerMap.forEach((_, channels) => channels.filterInPlace(_ != channel))
    }

  private val websocketApp: WebSocketApp[Any] =
    Handler.webSocket { channel =>
      channel.receiveAll {
        case UserEventTriggered(UserEvent.HandshakeComplete) =>
          ZIO.succeed(scribe.info(s"Client connected on $channel"))

        case Read(WebSocketFrame.Close(status, reason)) =>
          for
            _ <- ZIO.succeed(scribe.info(s"Client disconnected with status $status and reason $reason on $channel"))
            _ <- handleDisconnect(channel)
          yield ()

        case ExceptionCaught(cause) =>
          for
            _ <- ZIO.succeed(scribe.error(s"Exception caught on $channel", cause))
            _ <- handleDisconnect(channel)
          yield ()

        case Read(WebSocketFrame.Text(data)) =>
          route(data, channel).catchAll {
            case e: upickle.core.AbortException => wrongMessageFormat(channel, data)
            case e                              => ZIO.succeed(scribe.error(s"Error processing message: ${e.getMessage}"))
          }

        case _ => ZIO.unit
      }
    }

  private val routes = Routes(Method.GET / "ws" -> handler(websocketApp.toResponse))
  private val server =
    for
      runtime <- ZIO.runtime[Any]
      _       <- ZIO.succeed { QueueManager.runtime = runtime }
      _       <- ZIOHttpServer.serve(routes).provide(ZIOHttpServer.default)
    yield ()

  override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] =
    ZIO.attempt {
      Banner.load()
      Config.init()
    } *> server

end Server
