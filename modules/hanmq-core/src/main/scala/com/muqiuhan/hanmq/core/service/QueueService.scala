package com.muqiuhan.hanmq.core.service

import com.muqiuhan.hanmq.core.model.{BindingKey, Queue, QueueName}
import com.muqiuhan.hanmq.core.error.QueueError
import zio.ZIO

/** 队列服务接口 */
trait QueueService:
  /** 创建队列 */
  def createQueue(name: QueueName, bindingKey: BindingKey): ZIO[Any, QueueError, Unit]

  /** 删除队列 */
  def deleteQueue(name: QueueName): ZIO[Any, QueueError, Unit]

  /** 获取队列 */
  def getQueue(name: QueueName): ZIO[Any, QueueError, Option[Queue]]

  /** 获取所有队列 */
  def listQueues: ZIO[Any, QueueError, List[Queue]]
