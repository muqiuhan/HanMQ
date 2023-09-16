package client

import java.net.URI
import scala.collection.mutable
import com.typesafe.scalalogging.Logger
import com.alibaba.fastjson.JSON
import message.Message

class Consumer(serverURI: URI, name: String) {

    private val registerInfo = new mutable.Queue[String]
    private val client = new Client(serverURI, name, 0)
    private val log = Logger(getClass)

    try {
        client.connectBlocking()
    } catch {
        case e: InterruptedException => log.error(e.getMessage())
    }

    /// Register and bind a new queue to the Client
    /// [append]: true means append binding, false means overwrite binding
    def register(queueNames: List[String], append: Boolean): Unit = {
        if (!append) { registerInfo.clear() }

        registerInfo.addAll(queueNames)
        client.send(
          JSON.toJSONString(
            new Message(
              0,
              null,
              JSON.toJSONString(queueNames),
              utils.Date.getLocalTime()
            )
          )
        )
    }

    def register(queueName: String, append: Boolean): Unit = {
        register(List(queueName), append)
    }

    /// Custom message handling
    def onMessage(action: java.util.function.Consumer[String]) = {
        client.setOnMessageAction(action)
    }

    def getRegisterInfo(): List[String] = {
        registerInfo.toList
    }
}
