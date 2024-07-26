package core

import io.netty.channel.Channel
import io.netty.channel.ChannelId
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import upickle.default.*
import scala.collection.mutable
import utils.CheckInitialized

/// Create and manage worker threads and distribute messages to consumers.
/// The worker thread will be set as a daemon thread
object WorkerManager extends CheckInitialized:
  private val threads = new mutable.Queue[Thread]()

  def init(threadNum: Int): Unit =
    if initialized then return

    synchronized {
      if initialized then return

      for i <- 0 until threadNum do threads.addOne(initThread(i))

      scribe.info(s"${threadNum} worker threads are started.")
      initialize()
    }
  end init

  private def initThread(id: Int): Thread =
    val thread = new Thread(new Task(id), s"worker-${id}")
    QueueManager.get(id).workers.addOne(thread)
    thread.setDaemon(true)
    thread.start()
    thread
  end initThread
end WorkerManager

/// Continuously obtain messages in the queue and forward them to the corresponding channel.
case class Task(index: Int) extends Runnable:
  private val queue: MessageQueue = QueueManager.get(index)

  override def run(): Unit =
    scribe.info(s"worker thread of queue ${queue.name} is working")

    var message = new String();
    while true do
      try
        message = queue.take()
        val channelIds = BasicMap.queueConsumerMap.get(queue.name)

        while queue == null || channelIds.isEmpty do
          try
            Thread.sleep(Long.MaxValue)
            scribe.info("No consumers, sleeping...")
          catch case e: InterruptedException => scribe.warn(e.getMessage)
        end while

        for channelId <- channelIds do
          BasicMap.clients.find(channelId).writeAndFlush(new TextWebSocketFrame(write(message)))
      catch case e: InterruptedException => ()
    end while
  end run
end Task
