package com.muqiuhan.hanmq.routing.exchange

import com.muqiuhan.hanmq.core.model.{BindingKey, QueueName, RoutingKey}
import zio.ZIO

/** Topic Exchange 实现 */
object TopicExchange extends Exchange:
  override def exchangeType: ExchangeType = ExchangeType.Topic

  override def route(routingKey: RoutingKey, bindings: List[Binding]): ZIO[Any, RoutingError, List[QueueName]] =
    ZIO.succeed {
      bindings
        .filter(binding => matches(routingKey, binding.bindingKey))
        .map(_.queueName)
    }

  /** 路由键匹配逻辑（兼容旧实现） */
  private def matches(routingKey: String, bindingKey: String): Boolean =
    val keys: Array[String] = bindingKey.split("\\|")
    val part1: Array[String] = routingKey.split("\\.")
    keys.exists { key =>
      val part2: Array[String] = key.split("\\.")
      val len2: Int = part2.length
      val len1: Int = part1.length

      if key.contains("#") then
        var i: Int = -1
        if part2.head == "#" then
          i = 1
          while i <= math.min(len1, len2) && part2(len2 - i) == part1(len1 - i) do i += 1
          if part2(len2 - i) == "#" then return true
        else if part2.last == "#" then
          i = 0
          while i < math.min(len1, len2) && part2(i) == part1(i) do i += 1
          if part2(i) == "#" then return true
        end if
        false
      else
        var flag: Boolean = true
        if len1 == len2 then
          for i <- 0 until len1 if !(part2(i) == "*" || part1(i) == part2(i)) do flag = false
          flag
        else false
    }
