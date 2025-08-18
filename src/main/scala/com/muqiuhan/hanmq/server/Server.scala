package com.muqiuhan.hanmq.server

import com.muqiuhan.hanmq.core.{BasicMap, QueueManager}
import com.muqiuhan.hanmq.legacy.message.Message
import zio.*
import zio.http.*
import zio.http.ChannelEvent.*
import zio.http.Server as ZIOHttpServer
import scala.util.Try
import upickle.default as Upickle
import com.muqiuhan.hanmq.legacy.utils.Banner
import com.muqiuhan.hanmq.legacy.config.Config

object Server extends ZIOAppDefault:

  private def route(data: String, channel: WebSocketChannel): ZIO[BasicMap & QueueManager, Throwable, Unit] =
    ZIO.attempt(Upickle.read[Message](data)).flatMap { message =>
      message.typ match
        case 0 /* Subscription */ => processConsumerMessage(channel, message)
        case 1 /* Producer */     => processProducerMessage(message)
        case typ: Int             => ZIO.fail(new Exception(s"Unknown message type: $typ"))
      end match
    }

  private def processConsumerMessage(channel: WebSocketChannel, message: Message): ZIO[BasicMap & QueueManager, Nothing, Unit] =
    ZIO.attempt(Upickle.read[List[String]](message.extend)).orDie.flatMap { queueNames =>
      ZIO.foreachDiscard(queueNames) { queueName =>
        BasicMap.addConsumer(queueName, channel) *> QueueManager.signal(queueName)
      }
    }

  private inline def processProducerMessage(message: Message): ZIO[BasicMap & QueueManager, Nothing, Unit] =
    QueueManager.put(message.content, message.extend)

  private def wrongMessageFormat(channel: WebSocketChannel, data: String): UIO[Unit] =
    for
      _ <- ZIO.succeed(scribe.error(s"Wrong message format: $data"))
      _ <- channel.send(Read(WebSocketFrame.text("Wrong message format"))).orDie
      _ <- channel.shutdown
    yield ()

  private def handleDisconnect(channel: WebSocketChannel): ZIO[BasicMap, Nothing, Unit] =
    BasicMap.removeChannelFromAllQueues(channel)

  private val websocketApp: WebSocketApp[BasicMap & QueueManager] =
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
          route(data, channel).catchAll { e =>
            e match
              case e: upickle.core.AbortException => wrongMessageFormat(channel, data)
              case e                              => ZIO.succeed(scribe.error(s"Error processing message: ${e.getMessage}"))
          }

        case _ => ZIO.unit
      }
    }

  private val routes = Routes(Method.GET / "ws" -> handler(websocketApp.toResponse))
  private val server =
    ZIOHttpServer.serve(routes).provide(
        ZIOHttpServer.default,
        BasicMap.live,
        QueueManager.live
    )

  override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] =
    ZIO.attempt {
      Banner.load()
      Config.init()
    } *> server

end Server
