package client

import java.net.URI
import scala.collection.mutable

import message.Message
import scala.util.Try
import scala.util.Failure
import scala.util.Success
import upickle.default.*

class Consumer(serverURI: URI, name: String):

  private val registerInfo = new mutable.Queue[String]
  private val client       = new Client(serverURI, name, 0)

  Try(client.connectBlocking()) match
    case Failure(e: InterruptedException) => scribe.error(e.getMessage())
    case Failure(e)                       => throw e
    case Success(_)                       => ()
  end match
  /** Register and bind a new queue to the Client
    * [append]: true means append binding, false means overwrite binding */
  def register(queueNames: List[String], append: Boolean): Unit =
    if !append then registerInfo.clear()

    registerInfo.addAll(queueNames)
    client.send(write(new Message(0, null, write(queueNames), utils.Date.getLocalTime())))
  end register

  inline def register(queueName: String, append: Boolean): Unit     = register(List(queueName), append)
  inline def onMessage(action: java.util.function.Consumer[String]) = client.setOnMessageAction(action)
  inline def getRegisterInfo(): List[String]                        = registerInfo.toList

end Consumer
