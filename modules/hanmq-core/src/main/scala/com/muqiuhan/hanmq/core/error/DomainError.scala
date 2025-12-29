package com.muqiuhan.hanmq.core.error

/** 领域错误基类 */
sealed trait DomainError extends Exception

/** 队列相关错误 */
sealed trait QueueError extends DomainError
object QueueError:
  case class QueueNotFound(name: String) extends QueueError
  case class QueueAlreadyExists(name: String) extends QueueError
  case class InvalidQueueName(name: String) extends QueueError

/** 消费者相关错误 */
sealed trait ConsumerError extends DomainError
object ConsumerError:
  case class ConsumerNotFound(id: String) extends ConsumerError
  case class ConsumerAlreadyExists(id: String) extends ConsumerError

/** 消息相关错误 */
sealed trait MessageError extends DomainError
object MessageError:
  case class MessageNotFound(id: String) extends MessageError
  case class InvalidMessage(message: String) extends MessageError
  case class MessageDeliveryFailed(reason: String) extends MessageError
