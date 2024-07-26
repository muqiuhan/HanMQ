package core

import io.netty.channel.Channel
import io.netty.channel.ChannelId
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import upickle.default.*
import scala.collection.mutable
import utils.CheckInitialized
import scala.util.{Try, Failure, Success}

/** Create and manage worker threads and distribute messages to consumers.
  * The worker thread will be set as a daemon thread */
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

/** Continuously obtain messages in the queue and forward them to the corresponding channel. */
case class Task(index: Int) extends Runnable:
  private val queue: MessageQueue = QueueManager.get(index)

  private def work(): Unit =
    val message    = queue.take()
    val channelIds = BasicMap.queueConsumerMap.get(queue.name)

    while queue == null || channelIds.isEmpty do
      Try(Thread.sleep(Long.MaxValue)) match
        case Failure(e: InterruptedException) => scribe.warn(e.getMessage)
        case Failure(e)                       => throw e
        case _                                => scribe.info("No consumers, sleeping...")
    end while

    for channelId <- channelIds do
      BasicMap.clients.find(channelId).writeAndFlush(new TextWebSocketFrame(write(message)))
  end work

  override def run(): Unit =
    scribe.info(s"worker thread of queue ${queue.name} is working")

    while true do
      Try(work()) match
        case Failure(e: InterruptedException) => ()
        case Failure(e)                       => throw e
        case _                                => ()
    end while
  end run
end Task
