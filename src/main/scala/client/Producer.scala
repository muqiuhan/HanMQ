package client

import java.net.URI
import message.Message
import com.typesafe.scalalogging.Logger
import scala.util.{Try, Failure, Success}
import upickle.default._

class Producer(serverURI: URI, name: String):
  private val client                     = new Client(serverURI, name, 1)
  private var routingKey: Option[String] = None
  private val log                        = Logger(getClass())

  Try(client.connectBlocking()) match
    case Failure(e) => throw e
    case Success(_) => ()
  end match
  /** Send a message containing routingKey */
  def send(message: String, _routingKey: String): Unit =
    if routingKey.isEmpty then routingKey = (routingKey)
    client.send(write(new Message(1, message, _routingKey, utils.Date.getLocalTime())))
  end send

  def send(message: String): Unit =
    routingKey match
      case None             => log.error("Please set a default routing key.")
      case Some(routingKey) => send(message, routingKey)
  end send

  inline def setDefaultRoutingKey(_routingKey: String): Unit = routingKey = Some(_routingKey)
end Producer
