package client

import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.util.function.Consumer

class Client(serverURI: URI, name: String, typ: Int) extends WebSocketClient(serverURI):

  private var onMessageAction: Option[Consumer[String]] = None

  override def onOpen(handshakedata: ServerHandshake): Unit =
    scribe.info(s"Client ${name} connects successfully")

  override def onMessage(message: String): Unit =
    onMessageAction.foreach(onMessageAction => onMessageAction.accept(message))

  override def onClose(code: Int, reason: String, remote: Boolean): Unit =
    scribe.info(s"Connection closed. code: ${code}, reason: ${reason}, remote: ${remote}")

  override def onError(ex: Exception): Unit =
    scribe.error(s"Connection error: ${ex.getMessage()}")

  inline def setOnMessageAction(action: Consumer[String]): Unit =
    onMessageAction = Some(action)

end Client
