package com.muqiuhan.hanmq.protocol.message

/** 消息类型枚举 */
enum MessageType:
  case Subscribe  // 订阅消息 (type: 0)
  case Produce    // 生产消息 (type: 1)
  case Ack        // 确认消息 (type: 2)
  case Nack       // 拒绝消息 (type: 3)
  case Error      // 错误消息 (type: 4)
