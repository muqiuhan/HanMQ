package core

import java.util
import java.util.concurrent.LinkedBlockingQueue

/// CAS is not recommended because of the high concurrency of the message queue design,
/// and there should be no limit on the length of the message queue, because in practice,
/// the message queue can be persistent and the thread that put the message into the queue will be
/// blocked when the message is full (this is something that the author has long thought about in the design).
/// Therefore, LinkedBlockingQueue is used as the core implementation of queue.
class MessageQueue(_bindingKey: String, _name: String)
    extends LinkedBlockingQueue[String]:

    /// All worker threads working on the current queue
    private val workers = new util.ArrayList[Thread]()

    def bindingKey: String = _bindingKey

    def name: String = _name

    override def toString: String =
        s"MessageQueue(bindingKey = '${bindingKey}', name = '${name}', elements = ${super.toString}"
