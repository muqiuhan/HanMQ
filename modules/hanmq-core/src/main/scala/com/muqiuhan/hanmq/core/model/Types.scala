package com.muqiuhan.hanmq.core.model

/** 消息唯一标识符 */
type MessageId = String
object MessageId:
  def apply(value: String): MessageId = value

/** 路由键 */
type RoutingKey = String
object RoutingKey:
  def apply(value: String): RoutingKey = value

/** 队列名称 */
type QueueName = String
object QueueName:
  def apply(value: String): QueueName = value

/** 绑定键 */
type BindingKey = String
object BindingKey:
  def apply(value: String): BindingKey = value

/** 消费者标识符 */
type ConsumerId = String
object ConsumerId:
  def apply(value: String): ConsumerId = value
