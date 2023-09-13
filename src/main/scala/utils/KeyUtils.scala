package utils

import scala.util.boundary, boundary.break

object KeyUtils:
  def routingKeyCompare(routingKey: String, bindingKey: String): Boolean =
    val keys = bindingKey.split("\\|")
    val part1 = routingKey.split("\\.")

    boundary:
      for (key <- keys) do
        val part2 = key.split("\\.")
        val len2 = part2.length
        val len1 = part1.length

        if key.contains("#") then
          var i = 0
          if part2(0) == "#" then
            for (i <- 1 until Math.min(len1, len2)) do
              if part2(len2 - i) != part1(len1 - i) then break(false)
            if part2(len2 - i) == "#" then break(false)
          else if (part2(len2 - 1) == "#") then
            for (i <- 0 until Math.min(len1, len2)) do
              if part2(i) != part1(i) then break(false)
            if part2(i) == "#" then break(true)
        else
          var flag = true
          if len1 == len2 then
            for (i <- 0 until len1) do
              if !(part2(i) == "*" || part1(i) == part2(i)) then
                flag = false
                break(false)
            if flag then break(true)
    false
