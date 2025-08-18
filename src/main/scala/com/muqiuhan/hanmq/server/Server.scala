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

/** Main server object for HanMQ, implemented using ZIO HTTP.
 *  This object sets up the WebSocket server, defines routing logic for messages,
 *  and integrates with BasicMap and QueueManager for message handling and queue management.
 */
object Server extends ZIOAppDefault:

  /** Routes incoming WebSocket text messages based on their type.
   *  Dispatches to consumer message processing or producer message processing.
   *
   *  @param data    The incoming message content as a String (JSON format).
   *  @param channel The WebSocketChannel from which the message was received.
   *  @return A ZIO effect that processes the message, potentially failing with a Throwable.
   */
  private def route(data: String, channel: WebSocketChannel): ZIO[BasicMap & QueueManager, Throwable, Unit] =
    ZIO.attempt(Upickle.read[Message](data)).flatMap { message =>
      message.typ match
        case 0 /* Subscription message from consumer */ => processConsumerMessage(channel, message)
        case 1 /* General message from producer */      => processProducerMessage(message)
        case typ: Int                                   => ZIO.fail(new Exception(s"Unknown message type: $typ"))
      end match
    }

  /** Process subscription messages from consumers.
   *  It adds the consumer's channel to the specified queues in `BasicMap`
   *  and ensures the queues exist in `QueueManager` (which also starts worker fibers).
   *
   *  @param channel The WebSocketChannel of the subscribing consumer.
   *  @param message The subscription message containing queue names.
   *  @return A ZIO effect that completes when the consumer is processed.
   */
  private def processConsumerMessage(channel: WebSocketChannel, message: Message): ZIO[BasicMap & QueueManager, Nothing, Unit] =
    ZIO.attempt(Upickle.read[List[String]](message.extend)).orDie.flatMap { queueNames =>
      ZIO.foreachDiscard(queueNames) { queueName =>
        for
          // Ensure the queue exists in QueueManager (this also starts its worker fiber).
          _ <- QueueManager.addQueue(queueName, queueName)
          // Add the channel as a consumer to the queue in BasicMap.
          _ <- BasicMap.addConsumer(queueName, channel)
          // Signal the queue (although ZIO.Queue.take is non-blocking, this can be used for logging/monitoring).
          _ <- QueueManager.signal(queueName)
        yield ()
      }
    }

  /** Process general messages from producers.
   *  It puts the message into the `QueueManager` to be routed to appropriate queues.
   *
   *  @param message The message from the producer.
   *  @return A ZIO effect that completes when the message is put into the queues.
   */
  private inline def processProducerMessage(message: Message): ZIO[BasicMap & QueueManager, Nothing, Unit] =
    QueueManager.put(message.content, message.extend)

  /** Handle cases where an incoming message has a wrong or unparseable format.
   *  It logs the error and sends an error message back to the client before shutting down the channel.
   *
   *  @param channel The WebSocketChannel where the wrong message was received.
   *  @param data    The malformed message data.
   *  @return A ZIO effect that completes after handling the error and closing the channel.
   */
  private def wrongMessageFormat(channel: WebSocketChannel, data: String): UIO[Unit] =
    for
      _ <- ZIO.succeed(scribe.error(s"Wrong message format: $data"))
      _ <- channel.send(Read(WebSocketFrame.text("Wrong message format"))).orDie
      _ <- channel.shutdown // Close the channel due to malformed input
    yield ()

  /** Handle client disconnections.
   *  It removes the disconnected channel from all queues in `BasicMap` to ensure resource cleanup.
   *
   *  @param channel The WebSocketChannel that disconnected.
   *  @return A ZIO effect that completes after removing the channel from all queues.
   */
  private def handleDisconnect(channel: WebSocketChannel): ZIO[BasicMap & QueueManager, Nothing, Unit] =
    BasicMap.removeChannelFromAllQueues(channel) // Only BasicMap needs to handle channel removal

  /** Define the WebSocket application logic for handling various WebSocket events.
   *  This includes handshake completion, disconnections (close/exception), and incoming text messages.
   */
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
          // Route the incoming text message for processing.
          // Errors during routing (e.g., parsing issues) are caught and handled.
          route(data, channel).catchAll { e =>
            e match
              case e: upickle.core.AbortException => wrongMessageFormat(channel, data) // Handle JSON parsing errors specifically.
              case e                              => ZIO.succeed(scribe.error(s"Error processing message: ${e.getMessage}"))
          }

        case _ => ZIO.unit // Ignore other WebSocket frame types.
      }
    }

  /** Define the HTTP routes for the server. Currently, it exposes a single WebSocket endpoint at "/ws". */
  private val routes = Routes(Method.GET / "ws" -> handler(websocketApp.toResponse))

  /** Set up the ZIO HTTP server with default configuration and provides the necessary
   *  `BasicMap` and `QueueManager` services as ZLayers.
   */
  private val server =
    ZIOHttpServer.serve(routes).provide(
        ZIOHttpServer.default, // Provides default server configuration.
        BasicMap.live,         // Provides the live implementation of BasicMap.
        QueueManager.live      // Provides the live implementation of QueueManager.
    )

  /** The main entry point for the ZIO application.
   *  It loads the banner, initializes configuration, and starts the server.
   *
   *  @return A ZIO effect representing the application's main logic.
   */
  override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] =
    ZIO.attempt {
      Banner.load() // Load the application banner.
      Config.init() // Initialize application configuration (legacy config, might be refactored).
    } *> server     // Start the server after initialization.

end Server
