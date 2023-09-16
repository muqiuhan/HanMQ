package utils

import org.scalatest.funsuite.AnyFunSuite

class TestKey extends AnyFunSuite {
    test("item.# shoud match one or more") {
        assert(
          KeyUtils.routingKeyCompare("item", Data.matchOneOrMore)
        )

        assert(
          KeyUtils.routingKeyCompare("item.a.b", Data.matchOneOrMore)
        )

        assert(
          KeyUtils.routingKeyCompare("item.a", Data.matchOneOrMore)
        )
    }

    test("item.# should not match ite") {
        assert(
          !KeyUtils.routingKeyCompare("ite", Data.matchOneOrMore)
        )
    }

    test("item.* should match only one") {
        assert(
          KeyUtils.routingKeyCompare("item.a", Data.matchOne)
        )
    }

    test("item.* should not match item or item1") {
        assert(
          !KeyUtils.routingKeyCompare("item", Data.matchOne)
        )

        assert(
          !KeyUtils.routingKeyCompare("item1", Data.matchOr)
        )
    }

    test("item|item2 should match item or item2") {
        assert(
          KeyUtils.routingKeyCompare("item", Data.matchOr)
        )

        assert(
          KeyUtils.routingKeyCompare("item2", Data.matchOr)
        )
    }
}
end TestKey

case object Data {
    val matchOneOrMore = "item.#"
    val matchOne = "item.*"
    val matchOr = "item|item2"
}
