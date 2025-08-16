package com.muqiuhan.hanmq.core

import zio.http.WebSocketChannel
import java.util.concurrent.ConcurrentHashMap
import scala.collection.mutable.Queue

object BasicMap:
  val queueConsumerMap = new ConcurrentHashMap[String, Queue[WebSocketChannel]]()
end BasicMap
