package com.muqiuhan.hanmq.legacy.core

import java.util.concurrent.LinkedBlockingQueue
import scala.collection.mutable.{HashMap, Queue}

/** CAS is not recommended because of the high concurrency of the message queue design,
  * and there should be no limit on the length of the message queue, because in practice,
  * the message queue can be persistent and the thread that put the message into the queue will be
  * blocked when the message is full (this is something that the author has long thought about in the design).
  * Therefore, LinkedBlockingQueue is used as the core implementation of queue. */
case class MessageQueue(bindingKey: String, name: String) extends LinkedBlockingQueue[String]:

  /** All worker threads working on the current queue */
  val workers = new Queue[Thread]()

  override def toString: String =
    s"MessageQueue(bindingKey = '${bindingKey}', name = '${name}', elements = ${super.toString}"

end MessageQueue
