package com.muqiuhan.hanmq.legacy.server

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelId
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.group.ChannelGroup
import io.netty.channel.group.DefaultChannelGroup
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.util.concurrent.GlobalEventExecutor
import scala.collection.mutable.Queue
import com.muqiuhan.hanmq.legacy.message.Message
import com.muqiuhan.hanmq.legacy.core.BasicMap
import com.muqiuhan.hanmq.legacy.core.QueueManager
import upickle.default.*
import scala.util.{Try, Failure, Success}

/** Key components in Netty, handling messages from clients */
class MessageHandler extends SimpleChannelInboundHandler[TextWebSocketFrame]:

  private def route(data: String, channel: Channel) =
    val message = read[Message](data)

    message.typ match
      case 0 /* Subscription registration message from consumer */ => processConsumerMessage(channel, message)
      case 1 /* General messages from the producer */              => processProducerMessage(message)
      case typ: Int                                                => throw Exception(s"Unknown message type: ${typ}")
    end match
  end route

  protected def channelRead0(ctx: ChannelHandlerContext, msg: TextWebSocketFrame): Unit =
    val data    = msg.text()
    val channel = ctx.channel()

    MessageHandler.clients.add(channel)
    Try(route(data, channel)) match
      case Failure(e) => wrongMessageFormat(channel, data)
      case _          => ()
    end match
  end channelRead0

  private def processConsumerMessage(channel: Channel, message: Message): Unit =
    for queueName <- read[List[String]](message.extend) do
      // queue has not been registered by any consumer before
      if !BasicMap.queueConsumerMap.containsKey(queueName) then
        val channelIds = new Queue[ChannelId]()
        channelIds.addOne(channel.id())
        BasicMap.queueConsumerMap.put(queueName, channelIds)
      else
        BasicMap.queueConsumerMap.get(queueName).addOne(channel.id())
      end if

      QueueManager.signal(queueName)
    end for
  end processConsumerMessage

  private inline def processProducerMessage(message: Message): Unit =
    QueueManager.put(message.content, message.extend)

  private def wrongMessageFormat(channel: Channel, data: String): Unit =
    scribe.error(s"Wrong message format: ${data}")
    channel
      .writeAndFlush(new TextWebSocketFrame("Wrong message format"))
      .addListener(future =>
        channel.close()
        scribe.info("channel removed successfully")
      )
  end wrongMessageFormat

  override inline def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit =
    scribe.error(cause.getMessage())

end MessageHandler

case object MessageHandler:
  /** Used to record and manage all client channels, and can automatically remove disconnected sessions. */
  val clients = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE)
end MessageHandler
