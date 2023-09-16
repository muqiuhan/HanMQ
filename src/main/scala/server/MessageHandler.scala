package server

import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelId
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.group.ChannelGroup
import io.netty.channel.group.DefaultChannelGroup
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.util.concurrent.GlobalEventExecutor

import com.alibaba.fastjson.JSON
import com.typesafe.scalalogging.Logger

import scala.collection.mutable

import message.Message
import core.BasicMap
import core.QueueManager

/// Key components in Netty, handling messages from clients
class MessageHandler extends SimpleChannelInboundHandler[TextWebSocketFrame] {
    private val log = Logger(getClass)

    protected def channelRead0(
        ctx: ChannelHandlerContext,
        msg: TextWebSocketFrame
    ): Unit = {
        val data = msg.text()
        val channel = ctx.channel()

        MessageHandler.clients.add(channel)
        try {
            val message = JSON.parseObject(data, classOf[Message])

            message.typ match {
                // Subscription registration message from consumer
                case 0 => processConsumerMessage(channel, message)

                // General messages from the producer
                case 1 => processProducerMessage(message)

                case typ: Int =>
                    throw Exception(s"Unknown message type: ${typ}")
            }
        } catch {
            case e: Exception => wrongMessageFormat(channel, data)
        }
    }

    private def processConsumerMessage(
        channel: Channel,
        message: Message
    ): Unit = {
        val map = BasicMap.queueConsumerMap

        for (
          queueName <- JSON.parseObject(message.extend, classOf[List[String]])
        ) {
            // queue has not been registered by any consumer before
            if (!map.containsKey(queueName)) {
                map.put(queueName, mutable.Queue(channel.id()))
            } else {
                map.get(queueName).addOne(channel.id())
            }

            QueueManager.signal(queueName)
        }
    }

    private def processProducerMessage(message: Message): Unit = {
        QueueManager.put(message.content, message.content)
    }

    private def wrongMessageFormat(channel: Channel, data: String): Unit = {
        log.error(s"Wrong message format: ${data}")
        channel
            .writeAndFlush(
              new TextWebSocketFrame("Wrong message format")
            )
            .addListener(future => {
                channel.close()
                log.info("channel removed successfully")
            })
    }

    override def exceptionCaught(
        ctx: ChannelHandlerContext,
        cause: Throwable
    ): Unit = {
        log.error(cause.getMessage())
    }
}

case object MessageHandler {
    /// Used to record and manage all client channels, and can automatically remove disconnected sessions
    val clients = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE)
}
