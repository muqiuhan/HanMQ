package core

import com.typesafe.scalalogging.Logger
import io.netty.channel.Channel
import io.netty.channel.ChannelId
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import upickle.default._
import scala.collection.mutable
import utils.CheckInitialized

/// Create and manage worker threads and distribute messages to consumers.
/// The worker thread will be set as a daemon thread
object WorkerManager extends CheckInitialized(Logger(getClass)):
  private val threads = new mutable.Queue[Thread]()
  private val log     = Logger(getClass)

  def init(threadNum: Int): Unit =
    if initialized then return

    synchronized {
      if initialized then return

      for i <- 0 until threadNum do threads.addOne(initThread(i))

      log.info(s"${threadNum} worker threads are started.")
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
case class Task(id: Int) extends Runnable:
  private val log                          = Logger(getClass)
  private var _queue: Option[MessageQueue] = None

  inline def queue: MessageQueue = _queue.get

  override def run(): Unit =
    log.info(s"worker thread of queue ${queue.name} is working")

    var message = new String();
    while true do
      try
        message = queue.take()
        val channelIds = BasicMap.queueConsumerMap.get(queue.name)

        while queue == null || channelIds.isEmpty do
          try
            Thread.sleep(Long.MaxValue)
            log.info("No consumers, sleeping...")
          catch case e: InterruptedException => log.warn(e.getMessage)
        end while

        for channelId <- channelIds do
          BasicMap.clients.find(channelId).writeAndFlush(new TextWebSocketFrame(write(message)))
      catch case e: InterruptedException => ()
    end while
  end run
end Task
