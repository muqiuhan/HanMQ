package com.muqiuhan.hanmq.core.service

import com.muqiuhan.hanmq.core.model.{Message, MessageId, QueueName, RoutingKey}
import com.muqiuhan.hanmq.core.error.MessageError
import zio.ZIO

/** 消息服务接口 */
trait MessageService:
  /** 投递消息 */
  def put(message: Message, routingKey: RoutingKey): ZIO[Any, MessageError, Unit]

  /** 消费消息 */
  def take(queueName: QueueName): ZIO[Any, MessageError, Option[Message]]

  /** 确认消息 */
  def acknowledge(messageId: MessageId): ZIO[Any, MessageError, Unit]

  /** 拒绝消息 */
  def reject(messageId: MessageId): ZIO[Any, MessageError, Unit]
