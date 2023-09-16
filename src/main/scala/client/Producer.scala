package client

import java.net.URI
import com.alibaba.fastjson.JSON
import message.Message
import com.typesafe.scalalogging.Logger

class Producer(serverURI: URI, name: String) {
    private val client = new Client(serverURI, name, 1)
    private var routingKey: Option[String] = None
    private val log = Logger(getClass())

    try {
        client.connectBlocking()
    } catch {
        case e: InterruptedException => ()
    }

    /// Send a message containing routingKey
    def send(message: String, _routingKey: String): Unit = {
        if (routingKey.isEmpty) { routingKey = (routingKey) }

        client.send(
          JSON.toJSONString(
            new Message(
              1,
              message,
              _routingKey,
              utils.Date.getLocalTime()
            )
          )
        )
    }

    def send(message: String): Unit = {
        routingKey match {
            case None => log.error("Please set a default routing key.")
            case Some(routingKey) => send(message, routingKey)
        }
    }

    def setDefaultRoutingKey(_routingKey: String): Unit = {
        routingKey = Some(_routingKey)
    }
}
