package com.muqiuhan.hanmq.routing.exchange

import com.muqiuhan.hanmq.core.model.{BindingKey, QueueName, RoutingKey}
import zio.ZIO

/** Exchange 接口 */
trait Exchange:
  /** Exchange 类型 */
  def exchangeType: ExchangeType

  /** 路由消息到匹配的队列 */
  def route(routingKey: RoutingKey, bindings: List[Binding]): ZIO[Any, RoutingError, List[QueueName]]

/** Exchange 类型 */
enum ExchangeType:
  case Direct
  case Topic
  case Fanout
  case Headers

/** 绑定关系 */
case class Binding(
    queueName: QueueName,
    bindingKey: BindingKey
)

/** 路由错误 */
sealed trait RoutingError extends Exception
object RoutingError:
  case class InvalidRoutingKey(key: String) extends RoutingError
  case class InvalidBindingKey(key: String) extends RoutingError
