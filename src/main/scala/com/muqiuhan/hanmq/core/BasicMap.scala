package com.muqiuhan.hanmq.core

import io.netty.channel.ChannelId
import io.netty.channel.group.{ChannelGroup, DefaultChannelGroup}
import java.util.concurrent.ConcurrentHashMap
import com.muqiuhan.hanmq.server.MessageHandler
import scala.collection.mutable.Queue

/** Used to manage the basic mapping relationships in message queues
  * The producer's channelId with its own key makes it easy for nio threads to place messages in the corresponding queue.
  * The sequence number and channelId of the queue so that the dispatcher thread can forward to the corresponding subscription consumer */
object BasicMap:

  /** The name and channelId of the queue.
    * Facilitate the distribution thread to forward to the corresponding subscription consumer. */
  val queueConsumerMap = new ConcurrentHashMap[String, Queue[ChannelId]]()

  /** Used to record and manage all client channels, and can automatically remove disconnected sessions */
  val clients: DefaultChannelGroup = MessageHandler.clients

end BasicMap
