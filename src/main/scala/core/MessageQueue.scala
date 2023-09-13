package core

import java.util
import java.util.concurrent.LinkedBlockingQueue

class MessageQueue(_bindingKey: String, _name: String)
    extends LinkedBlockingQueue[String] {

  /// All worker threads working on the current queue
  private val workers = new util.ArrayList[Thread]()

  def bindingKey: String = _bindingKey
  def name: String = _name

  override def toString: String = {
    s"MessageQueue(bindingKey = '${bindingKey}', name = '${name}', elements = ${super.toString}"
  }
}
