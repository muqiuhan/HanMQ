package core

import io.netty.channel.ChannelId
import io.netty.channel.group.{ChannelGroup, DefaultChannelGroup}

import java.util.concurrent.ConcurrentHashMap
import network.io.MessageHandler

/// Used to manage the basic mapping relationships in message queues
/// The producer's channelId with its own key makes it easy for nio threads to place messages in the corresponding queue.
/// The sequence number and channelId of the queue so that the dispatcher thread can forward to the corresponding subscription consumer
object BasicMap {
  val queueConsumerMap = new ConcurrentHashMap[String, List[ChannelId]]()
  val clients: DefaultChannelGroup = MessageHandler.clients
}
