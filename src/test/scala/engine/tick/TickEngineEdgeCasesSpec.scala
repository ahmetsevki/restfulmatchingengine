package engine.tick

import engine.{Buy, Sell}
import org.scalatest.{FunSuite, Matchers}
class TickEngineEdgeCasesSpec extends FunSuite with Matchers{
  test("no bids or offers"){
    // s&p 500 at 5000
    val ng = new TickMatchingEngine(500000)
    ng.book should be (TickBook(Nil, Nil))
    ng.maxBidIndex should be (-1)
    ng.minOfferIndex should be (500000+1)
  }
  test("no offers"){
    val ng = new TickMatchingEngine(500000)
    ng.book should be (TickBook(Nil, Nil))
    Seq(
      TickOrder(1500, 10, Buy, "o1"),
      TickOrder(1300, 7, Buy, "o2"),
      TickOrder(1350, 6, Buy, "o3"),
    ).foreach{ ng.send(_) }
    ng.book should be (
      TickBook(
        Seq(TickOrder(1500,10,Buy,"o1"), TickOrder(1350,6,Buy,"o3"), TickOrder(1300,7,Buy,"o2")),
        Nil
      )
    )
    ng.maxBidIndex should be (1500)
    ng.minOfferIndex should be (500000+1)
  }
  test("no bids"){
    val ng = new TickMatchingEngine(500000)
    ng.book should be (TickBook(Nil, Nil))
    Seq(
      TickOrder(1500, 10, Sell, "o1"),
      TickOrder(1300, 7, Sell, "o2"),
      TickOrder(1350, 6, Sell, "o3"),
    ).foreach{ ng.send(_) }
    ng.book should be (
      TickBook(
        Nil,
        Seq(TickOrder(1300,7,Sell,"o2"), TickOrder(1350,6,Sell,"o3"), TickOrder(1500,10,Sell,"o1"))
      )
    )
    ng.maxBidIndex should be (-1)
    ng.minOfferIndex should be (1300)
  }
}
