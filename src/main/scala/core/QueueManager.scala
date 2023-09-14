package core

import com.typesafe.scalalogging.Logger
import java.util
import utils.KeyUtils

class QueueManagerData(
    __queues: Array[MessageQueue],
    __queueMap: util.HashMap[String, MessageQueue]
):
    var queues: Array[MessageQueue] = __queues
    var queueMap: util.HashMap[String, MessageQueue] = __queueMap

object QueueManager:
    private var __data: Option[QueueManagerData] = None
    private val log = Logger(getClass)

    def init(
        queueNum: Int,
        bindingKeys: List[String],
        queueNames: Option[List[String]]
    ): Unit =
        synchronized {
            initCheck(queueNum, bindingKeys)

            val queues = new Array[MessageQueue](queueNum)
            val queueMap = new util.HashMap[String, MessageQueue]()
            val nameChooser = new util.HashMap[String, Int]()

            for i <- 0 until queueNum do
                if i >= queueNames.get.length || queueNames.isEmpty
                then queues(i) = new MessageQueue(bindingKeys(i), s"queue_${i}")
                else
                    val name = queueNames.get()(i)
                    if nameChooser.containsKey(name) then
                        val old = nameChooser.get(name);
                        val newName = name + old;

                        queues(i) = new MessageQueue(bindingKeys(i), newName)
                        log.warn(
                          "A duplicated queue queueNames {} is modified to {}",
                          name,
                          newName
                        )
                        nameChooser.put(name, old + 1)
                        queueMap.put(newName, queues(i))
                    else
                        queues(i) = new MessageQueue(bindingKeys(i), name)
                        nameChooser.put(name, 1);
                        queueMap.put(name, queues(i))

            log.info(s"${queueNum} queues are ready")
        }

    private def initCheck(
        queueNum: Int,
        bindingKeys: List[String]
    ): Unit =
        if bindingKeys.length == queueNum then
            log.error("The length of bindingKeys not equal to queueNum.")
            throw RuntimeException(
              "The length of bindingKeys not equal to queueNum."
            )

    def data: QueueManagerData = __data.get
