package utils

class TestKey extends munit.FunSuite:
  test("item.# shoud match one or more") {
    assert(com.muqiuhan.hanmq.legacy.utils.KeyUtils.routingKeyCompare("item", Data.matchOneOrMore))

    assert(com.muqiuhan.hanmq.legacy.utils.KeyUtils.routingKeyCompare("item.a.b", Data.matchOneOrMore))

    assert(com.muqiuhan.hanmq.legacy.utils.KeyUtils.routingKeyCompare("item.a", Data.matchOneOrMore))
  }

  test("item.# should not match ite") {
    assert(!com.muqiuhan.hanmq.legacy.utils.KeyUtils.routingKeyCompare("ite", Data.matchOneOrMore))
  }

  test("item.* should match only one") {
    assert(com.muqiuhan.hanmq.legacy.utils.KeyUtils.routingKeyCompare("item.a", Data.matchOne))
  }

  test("item.* should not match item or item1") {
    assert(!com.muqiuhan.hanmq.legacy.utils.KeyUtils.routingKeyCompare("item", Data.matchOne))

    assert(!com.muqiuhan.hanmq.legacy.utils.KeyUtils.routingKeyCompare("item1", Data.matchOr))
  }

  test("item|item2 should match item or item2") {
    assert(com.muqiuhan.hanmq.legacy.utils.KeyUtils.routingKeyCompare("item", Data.matchOr))

    assert(com.muqiuhan.hanmq.legacy.utils.KeyUtils.routingKeyCompare("item2", Data.matchOr))
  }
end TestKey

case object Data:
  val matchOneOrMore = "item.#"
  val matchOne       = "item.*"
  val matchOr        = "item|item2"
end Data
