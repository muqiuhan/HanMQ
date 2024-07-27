package client

import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.util.function.Consumer

class Client(serverURI: URI, name: String, typ: Int) extends WebSocketClient(serverURI):

  private var _onMessageAction: Option[Consumer[String]] = None

  inline def setOnMessageAction(action: Consumer[String]): Unit = _onMessageAction = Some(action)

  override def onOpen(handshakedata: ServerHandshake): Unit = scribe.info(s"Client ${name} connects successfully")

  override def onMessage(message: String): Unit = _onMessageAction.foreach(_.accept(message))

  override def onClose(code: Int, reason: String, remote: Boolean): Unit =
    scribe.info(s"Connection closed. code: ${code}, reason: ${reason}, remote: ${remote}")

  override def onError(ex: Exception): Unit =
    scribe.error(s"Connection error: ${ex.getMessage()}")

end Client
