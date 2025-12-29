package com.muqiuhan.hanmq.core.model

/** 队列领域模型 */
case class Queue(
    name: QueueName,
    bindingKey: BindingKey,
    depth: Long,
    consumerCount: Int
)
