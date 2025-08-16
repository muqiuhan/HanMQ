package com.muqiuhan.hanmq.core

import zio.http.WebSocketFrame
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import zio.{Runtime, Unsafe}
import zio.http.ChannelEvent.Read

object QueueManager:

  private val messageQueues = new ConcurrentHashMap[String, LinkedBlockingQueue[String]]()
  private val workers       = new ConcurrentHashMap[String, Thread]()
  // The ZIO runtime will be set from the main application
  var runtime: Runtime[Any] = null

  def add(name: String): Unit =
    if !messageQueues.containsKey(name) then
      messageQueues.put(name, new LinkedBlockingQueue[String]())
      scribe.info(s"Created queue: $name")
      val worker = new Thread(() =>
        scribe.info(s"Worker thread started for queue: $name")
        while true do
          val message = messageQueues.get(name).take()
          scribe.debug(s"Worker thread for queue $name received message: $message")
          val consumers = BasicMap.queueConsumerMap.get(name)
          scribe.debug(s"Found ${if consumers != null then consumers.size else 0} consumers for queue: $name")
          if consumers != null then
            consumers.foreach(channel =>
              if runtime != null then
                Unsafe.unsafe { implicit unsafe =>
                  runtime.unsafe.run(channel.send(Read(WebSocketFrame.text(message)))).getOrThrowFiberFailure()
                }
              else
                scribe.error("QueueManager runtime not initialized!")
            )
          else
            scribe.warn(s"No consumers found for queue: $name")
          end if
        end while
      )
      worker.start()
      workers.put(name, worker)
    end if
  end add

  def put(message: String, queueName: String): Unit =
    add(queueName)
    messageQueues.get(queueName).put(message)
  end put

  inline def signal(queueName: String): Unit = add(queueName)

end QueueManager
