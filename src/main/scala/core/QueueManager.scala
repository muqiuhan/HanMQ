package core

import com.typesafe.scalalogging.Logger
import scala.collection.mutable
import utils.KeyUtils
import utils.CheckInitialized

/// Initialize the creation and management of all queues,
/// and provide functions such as placing messages, obtaining specific queues,
/// and awakening worker threads sleeping on queues
object QueueManager extends CheckInitialized(Logger(getClass)) {
    private val log = Logger(getClass)
    private val queues = new mutable.Queue[MessageQueue]()
    private val queueMap = new mutable.HashMap[String, MessageQueue]()

    def init(queueNum: Int, bindingKeys: Array[String]): Unit = {
        initWithQueueNames(queueNum, bindingKeys, None)
    }

    def init(
        queueNum: Int,
        bindingKeys: Array[String],
        queueNames: Option[Array[String]]
    ): Unit = {
        if (initialized) { return }

        synchronized {
            // double check
            if (initialized) { return }
            if (bindingKeys.length != queueNum) {
                log.error("The length of bindingKeys not equal to queueNum.")
                throw ExceptionInInitializerError()
            } else {
                initWithQueueNames(queueNum, bindingKeys, queueNames)
                initialize()
            }
        }
    }

    def put(message: String, routingKey: String): Unit = {
        checkInitialized()
        for (queue <- queues) {
            if (KeyUtils.routingKeyCompare(routingKey, queue.bindingKey)) {
                try {
                    queue.put(message)
                } catch {
                    case e: InterruptedException =>
                        ()
                }
            }
        }
    }

    def get(index: Int): MessageQueue = {
        checkInitialized()
        queues(index)
    }

    def contains(name: String): Boolean = {
        checkInitialized()
        queueMap.contains(name)
    }

    /// Wake up a thread waiting on a queue
    def signal(queueName: String): Unit = {
        for (worker <- queueMap(queueName).workers) {
            worker.interrupt()
        }
    }

    private def initWithQueueNames(
        queueNum: Int,
        bindingKeys: Array[String],
        queueNames: Option[Array[String]]
    ): Unit = {
        // Make sure there are no duplicate names.
        // If so, slightly modify the original name (name + id)
        val nameChooser = new mutable.HashMap[String, Int]()
        for (i <- 0 until queueNum) {
            if (queueNames.isEmpty || i >= queueNames.get.length) {
                queues.addOne(
                  new MessageQueue(bindingKeys(i), s"queue_${i}")
                )
            } else {
                queueNames match {
                    case None => throw ExceptionInInitializerError()
                    case Some(queueNames) =>
                        initWhenQueueNamesIsNotEmpty(
                          queueNames(i),
                          nameChooser,
                          bindingKeys(i)
                        ) match {
                            case Some(name, queue) =>
                                queues.addOne(queue)
                                queueMap.put(name, queue)
                            case None => throw ExceptionInInitializerError()
                        }
                }
            }
        }
    }

    private def initWhenQueueNamesIsNotEmpty(
        name: String,
        nameChooser: mutable.HashMap[String, Int],
        bindingKey: String
    ): Option[(String, MessageQueue)] = {
        if (nameChooser.contains(name)) {
            nameChooser
                .get(name)
                .map(old => {
                    val newName = s"${name}${old}"
                    log.warn(
                      s"A duplicated queue queueNames ${name} is modified to ${newName}"
                    )
                    nameChooser.put(name, old + 1)
                    (name, new MessageQueue(bindingKey, newName))
                })
        } else {
            nameChooser.put(name, 1)
            Some(name, new MessageQueue(bindingKey, name))
        }
    }

}
