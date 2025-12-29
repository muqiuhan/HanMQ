package com.muqiuhan.hanmq.core.model

/** 消息领域模型 */
case class Message(
    id: MessageId,
    content: String,
    routingKey: RoutingKey,
    timestamp: Long,
    status: MessageStatus
)

/** 消息状态 */
enum MessageStatus:
  case Pending      // 待消费
  case InFlight     // 消费中
  case Acknowledged // 已确认
  case Rejected     // 已拒绝
