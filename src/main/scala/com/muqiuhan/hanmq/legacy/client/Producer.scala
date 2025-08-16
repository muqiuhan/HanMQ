package com.muqiuhan.hanmq.legacy.client

import java.net.URI
import com.muqiuhan.hanmq.legacy.message.Message

import scala.util.{Try, Failure, Success}
import upickle.default.*

class Producer(serverURI: URI, name: String):
  private val client                      = new Client(serverURI, name, 1)
  private var _routingKey: Option[String] = None

  Try(client.connectBlocking()) match
    case Failure(e) => throw e
    case Success(_) => ()
  end match
  /** Send a message containing routingKey */
  def send(message: String, routingKey: String): Unit =
    if _routingKey.isEmpty then _routingKey = Some(routingKey)
    client.send(write(new Message(1, message, routingKey, com.muqiuhan.hanmq.legacy.utils.Date.getLocalTime())))
  end send

  def send(message: String): Unit =
    _routingKey match
      case None             => scribe.error("Please set a default routing key.")
      case Some(routingKey) => send(message, routingKey)
  end send

  inline def setDefaultRoutingKey(routingKey: String): Unit = _routingKey = Some(routingKey)
end Producer
