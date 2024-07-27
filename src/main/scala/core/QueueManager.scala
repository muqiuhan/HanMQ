package core

import utils.KeyUtils
import utils.CheckInitialized
import scala.util.{Try, Failure, Success}
import java.util.ArrayList
import java.util.HashMap

/** Initialize the creation and management of all queues,
  * and provide functions such as placing messages, obtaining specific queues,
  * and awakening worker threads sleeping on queues */
object QueueManager extends CheckInitialized:
  private val queues   = new ArrayList[MessageQueue]()
  private val queueMap = new HashMap[String, MessageQueue]()

  inline def init(queueNum: Int, bindingKeys: Array[String]): Unit = initWithQueueNames(queueNum, bindingKeys, None)

  def init(queueNum: Int, bindingKeys: Array[String], queueNames: Option[Array[String]]): Unit =
    if initialized then return

    synchronized {
      // double check
      if initialized then return
      if bindingKeys.length != queueNum then
        scribe.error("The length of bindingKeys not equal to queueNum.")
        throw ExceptionInInitializerError()
      else
        initWithQueueNames(queueNum, bindingKeys, queueNames)
        initialize()
      end if
    }
  end init

  def put(message: String, routingKey: String): Unit =
    checkInitialized()

    queues.forEach(queue =>
      if KeyUtils.routingKeyCompare(routingKey, queue.bindingKey) then
        Try(queue.put(message)) match
          case Failure(e: InterruptedException) => ()
          case Failure(e)                       => throw e
          case _                                => ()
    )
  end put

  inline def get(index: Int): MessageQueue =
    checkInitialized()
    queues.get(index)
  end get

  inline def contains(name: String): Boolean =
    checkInitialized()
    queueMap.containsKey(name)
  end contains

  /** Wake up a thread waiting on a queue */
  inline def signal(queueName: String): Unit = queueMap.get(queueName).workers.forEach(_.interrupt())

  private def initWithQueueNames(queueNum: Int, bindingKeys: Array[String], queueNames: Option[Array[String]]): Unit =
    // Make sure there are no duplicate names.
    // If so, slightly modify the original name (name + id)
    val nameChooser = new HashMap[String, Int]()

    for i <- 0 until queueNum do
      if queueNames.isEmpty || i >= queueNames.get.length then
        queues.add(new MessageQueue(bindingKeys(i), s"queue_${i}"))
      else
        queueNames match
          case None => throw ExceptionInInitializerError()
          case Some(queueNames) => initWhenQueueNamesIsNotEmpty(queueNames(i), nameChooser, bindingKeys(i)) match
              case Some(name, queue) =>
                queues.add(queue)
                queueMap.put(name, queue)
              case None => throw ExceptionInInitializerError()
        end match
    end for
  end initWithQueueNames

  private def initWhenQueueNamesIsNotEmpty(
      name: String, nameChooser: HashMap[String, Int], bindingKey: String
  ): Option[(String, MessageQueue)] =
    if nameChooser.containsKey(name) then
      val old     = nameChooser.get(name)
      val newName = s"${name}${old}"

      scribe.warn(s"A duplicated queue queueNames ${name} is modified to ${newName}")

      nameChooser.put(name, old + 1)
      Some(name, new MessageQueue(bindingKey, newName))
    else
      nameChooser.put(name, 1)
      Some(name, new MessageQueue(bindingKey, name))
    end if
  end initWhenQueueNamesIsNotEmpty

end QueueManager
