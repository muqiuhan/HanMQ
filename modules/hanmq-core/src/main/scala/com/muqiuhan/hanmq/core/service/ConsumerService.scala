package com.muqiuhan.hanmq.core.service

import com.muqiuhan.hanmq.core.model.{ConsumerId, QueueName}
import com.muqiuhan.hanmq.core.error.ConsumerError
import zio.ZIO

/** 消费者服务接口 */
trait ConsumerService:
  /** 添加消费者到队列 */
  def addConsumer(queueName: QueueName, consumerId: ConsumerId): ZIO[Any, ConsumerError, Unit]

  /** 从队列移除消费者 */
  def removeConsumer(queueName: QueueName, consumerId: ConsumerId): ZIO[Any, ConsumerError, Unit]

  /** 获取队列的所有消费者 */
  def getConsumers(queueName: QueueName): ZIO[Any, ConsumerError, Set[ConsumerId]]

  /** 移除消费者的所有订阅 */
  def removeConsumerFromAllQueues(consumerId: ConsumerId): ZIO[Any, ConsumerError, Unit]
