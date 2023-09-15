package core

import com.typesafe.scalalogging.Logger

import scala.collection.mutable

object QueueManager {
    private val log = Logger(getClass)
    private val queues = new mutable.Queue[MessageQueue]()
    private val queueMap = new mutable.HashMap[String, MessageQueue]()
    private var inited = false

    def init(queueNum: Int, bindingKeys: List[String]): Unit = {
        initWithQueueNames(queueNum, bindingKeys, None)
    }

    def init(
        queueNum: Int,
        bindingKeys: List[String],
        queueNames: Option[List[String]]
    ): Unit = {
        if (inited) { return }

        synchronized {
            // double check
            if (inited) { return }
            if (bindingKeys.length != queueNum) {
                log.error("The length of bindingKeys not equal to queueNum.")
                throw ExceptionInInitializerError()
            } else {
                initWithQueueNames(queueNum, bindingKeys, queueNames)
                inited = true
            }
        }
    }

    private def initWithQueueNames(
        queueNum: Int,
        bindingKeys: List[String],
        queueNames: Option[List[String]]
    ): Unit = {
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
