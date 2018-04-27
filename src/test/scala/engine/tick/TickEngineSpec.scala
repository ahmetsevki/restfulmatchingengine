package engine.tick

import engine.{Buy, Sell}
import org.scalatest.{FunSuite, Matchers}

class TickEngineSpec extends FunSuite with Matchers{
  test("match a buy, order size < best offer size"){
    val ng = new TickMatchingEngine(5000)
    // create book
    Seq(
      TickOrder(1500, 10, Buy, "o1"),
      TickOrder(1350, 6, Buy, "o2"),
      TickOrder(1300, 7, Buy, "o3"),
      TickOrder(1510, 2, Sell, "o4"),
      TickOrder(1510, 6, Sell, "o5"),
      TickOrder(1550, 10, Sell, "o6"),
      TickOrder(1600, 15, Sell, "o7"),
    ).foreach{ ng.send(_) }
    ng.book should be (
      TickBook(
        Seq(
          TickOrder(1500, 10, Buy, "o1"),
          TickOrder(1350, 6, Buy, "o2"),
          TickOrder(1300, 7, Buy, "o3")),
        Seq(
          TickOrder(1510, 2, Sell, "o4"),
          TickOrder(1510, 6, Sell, "o5"),
          TickOrder(1550, 10, Sell, "o6"),
          TickOrder(1600, 15, Sell, "o7")
        )
      )
    )
    ng.maxBidIndex should be (1500)
    ng.minOfferIndex should be (1510)

    ng.send(TickOrder(1510, 3, Buy, "o8")) should be (Seq(
      TickExecution(1510,2, "o8", "o4"),
      TickExecution(1510,1, "o8", "o5")
    ))
    ng.book should be (
      TickBook(
        Seq(
          TickOrder(1500, 10, Buy, "o1"),
          TickOrder(1350, 6, Buy, "o2"),
          TickOrder(1300, 7, Buy, "o3")),
        Seq(
          TickOrder(1510, 5, Sell, "o5"),
          TickOrder(1550, 10, Sell, "o6"),
          TickOrder(1600, 15, Sell, "o7")
        )
      )
    )
    ng.maxBidIndex should be (1500)
    ng.minOfferIndex should be (1510)
  }
  test("match a buy, order size > best offer size, and not all get executed"){
    val ng = new TickMatchingEngine(5000)
    // create book
    Seq(
      TickOrder(1500, 10, Buy, "o1"),
      TickOrder(1350, 6, Buy, "o2"),
      TickOrder(1300, 7, Buy, "o3"),
      TickOrder(1510, 2, Sell, "o4"),
      TickOrder(1510, 2, Sell, "o5"),
      TickOrder(1550, 10, Sell, "o6"),
      TickOrder(1600, 15, Sell, "o7"),
    ).foreach{ ng.send(_) }
    ng.book should be (
      TickBook(
        Seq(
          TickOrder(1500, 10, Buy, "o1"),
          TickOrder(1350, 6, Buy, "o2"),
          TickOrder(1300, 7, Buy, "o3")),
        Seq(
          TickOrder(1510, 2, Sell, "o4"),
          TickOrder(1510, 2, Sell, "o5"),
          TickOrder(1550, 10, Sell, "o6"),
          TickOrder(1600, 15, Sell, "o7")
        )
      )
    )
    ng.send(TickOrder(1510, 5, Buy, "o8")) should be (Seq(
      TickExecution(1510,2, "o8", "o4"),
      TickExecution(1510,2, "o8", "o5")
    ))
    ng.maxBidIndex should be (1510)
    ng.minOfferIndex should be (1550)
    ng.book should be (
      TickBook(
        Seq(
          TickOrder(1510, 1, Buy, "o8"),
          TickOrder(1500, 10, Buy, "o1"),
          TickOrder(1350, 6, Buy, "o2"),
          TickOrder(1300, 7, Buy, "o3")
        ),
        Seq(

          TickOrder(1550, 10, Sell, "o6"),
          TickOrder(1600, 15, Sell, "o7")
        )
      )
    )
    ng.maxBidIndex should be (1510)
    ng.minOfferIndex should be (1550)
  }
  test("match a buy, sweeps all book, and not all get executed"){
    val ng = new TickMatchingEngine(5000)
    // create book
    Seq(
      TickOrder(1500, 10, Buy, "o1"),
      TickOrder(1350, 6, Buy, "o2"),
      TickOrder(1300, 7, Buy, "o3"),
      TickOrder(1510, 2, Sell, "o4"),
      TickOrder(1510, 2, Sell, "o5"),
      TickOrder(1550, 10, Sell, "o6"),
      TickOrder(1600, 15, Sell, "o7"),
    ).foreach{ ng.send(_) }

    ng.send(TickOrder(1700, 30, Buy, "o8")) should be (Seq(
      TickExecution(1510,2, "o8", "o4"),
      TickExecution(1510,2, "o8", "o5"),
      TickExecution(1550,10, "o8", "o6"),
      TickExecution(1600,15, "o8", "o7")
    ))
    ng.maxBidIndex should be (1700)
    ng.minOfferIndex should be (5000+1)
    ng.book should be (
      TickBook(
        Seq(
          TickOrder(1700, 1, Buy, "o8"),
          TickOrder(1500, 10, Buy, "o1"),
          TickOrder(1350, 6, Buy, "o2"),
          TickOrder(1300, 7, Buy, "o3")
        ),
        Nil
      )
    )
    ng.send(TickOrder(1700, 5, Buy, "o9")) should be (Nil)
    ng.maxBidIndex should be (1700)
    ng.minOfferIndex should be (5000+1)
    ng.book should be (
      TickBook(
        Seq(
          TickOrder(1700, 1, Buy, "o8"),
          TickOrder(1700, 5, Buy, "o9"),
          TickOrder(1500, 10, Buy, "o1"),
          TickOrder(1350, 6, Buy, "o2"),
          TickOrder(1300, 7, Buy, "o3")
        ),
        Nil
      )
    )
    ng.send(TickOrder(1800, 5, Sell, "o10")) should be (Nil)
    ng.maxBidIndex should be (1700)
    ng.minOfferIndex should be (1800)
    ng.book should be (
      TickBook(
        Seq(
          TickOrder(1700, 1, Buy, "o8"),
          TickOrder(1700, 5, Buy, "o9"),
          TickOrder(1500, 10, Buy, "o1"),
          TickOrder(1350, 6, Buy, "o2"),
          TickOrder(1300, 7, Buy, "o3")
        ),
        Seq(
          TickOrder(1800, 5, Sell, "o10"),
        )
      )
    )
  }
  test("match a buy, order size == best offer size"){
    val ng = new TickMatchingEngine(5000)
    // create book
    Seq(
      TickOrder(1500, 10, Buy, "o1"),
      TickOrder(1350, 6, Buy, "o2"),
      TickOrder(1300, 7, Buy, "o3"),
      TickOrder(1510, 2, Sell, "o4"),
      TickOrder(1550, 10, Sell, "o6"),
      TickOrder(1600, 15, Sell, "o7"),
    ).foreach{ ng.send(_) }
    ng.book should be (
      TickBook(
        Seq(
          TickOrder(1500, 10, Buy, "o1"),
          TickOrder(1350, 6, Buy, "o2"),
          TickOrder(1300, 7, Buy, "o3")),
        Seq(
          TickOrder(1510, 2, Sell, "o4"),
          TickOrder(1550, 10, Sell, "o6"),
          TickOrder(1600, 15, Sell, "o7")
        )
      )
    )
    ng.send(TickOrder(1510, 2, Buy, "o8")) should be (Seq(
      TickExecution(1510,2, "o8", "o4"),
    ))
    ng.maxBidIndex should be (1500)
    ng.minOfferIndex should be (1550)
    ng.book should be (
      TickBook(
        Seq(
          TickOrder(1500, 10, Buy, "o1"),
          TickOrder(1350, 6, Buy, "o2"),
          TickOrder(1300, 7, Buy, "o3")
        ),
        Seq(

          TickOrder(1550, 10, Sell, "o6"),
          TickOrder(1600, 15, Sell, "o7")
        )
      )
    )
    ng.maxBidIndex should be (1500)
    ng.minOfferIndex should be (1550)
  }
  test("match a sell, order size < best bid size"){
    val ng = new TickMatchingEngine(5000)
    // create book
    Seq(
      TickOrder(1500, 10, Buy, "o1"),
      TickOrder(1350, 6, Buy, "o2"),
      TickOrder(1300, 7, Buy, "o3"),
      TickOrder(1510, 2, Sell, "o4"),
      TickOrder(1510, 6, Sell, "o5"),
      TickOrder(1550, 10, Sell, "o6"),
      TickOrder(1600, 15, Sell, "o7"),
    ).foreach{ ng.send(_) }
    ng.book should be (
      TickBook(
        Seq(
          TickOrder(1500, 10, Buy, "o1"),
          TickOrder(1350, 6, Buy, "o2"),
          TickOrder(1300, 7, Buy, "o3")),
        Seq(
          TickOrder(1510, 2, Sell, "o4"),
          TickOrder(1510, 6, Sell, "o5"),
          TickOrder(1550, 10, Sell, "o6"),
          TickOrder(1600, 15, Sell, "o7")
        )
      )
    )
    ng.maxBidIndex should be (1500)
    ng.minOfferIndex should be (1510)

    ng.send(TickOrder(1300, 3, Sell, "o8")) should be (Seq(
      TickExecution(1500,3, "o1", "o8")
    ))
    ng.book should be (
      TickBook(
        Seq(
          TickOrder(1500, 7, Buy, "o1"),
          TickOrder(1350, 6, Buy, "o2"),
          TickOrder(1300, 7, Buy, "o3")),
        Seq(
          TickOrder(1510, 2, Sell, "o4"),
          TickOrder(1510, 6, Sell, "o5"),
          TickOrder(1550, 10, Sell, "o6"),
          TickOrder(1600, 15, Sell, "o7")
        )
      )
    )
  }
  test("match a sell, order size > best offer, and not all get executed"){
    val ng = new TickMatchingEngine(5000)
    // create book
    Seq(
      TickOrder(1500, 4, Buy, "o1"),
      TickOrder(1500, 6, Buy, "o11"),
      TickOrder(1350, 6, Buy, "o2"),
      TickOrder(1300, 7, Buy, "o3"),
      TickOrder(1510, 2, Sell, "o4"),
      TickOrder(1510, 2, Sell, "o5"),
      TickOrder(1550, 10, Sell, "o6"),
      TickOrder(1600, 15, Sell, "o7"),
    ).foreach{ ng.send(_) }
    ng.book should be (
      TickBook(
        Seq(
          TickOrder(1500, 4, Buy, "o1"),
          TickOrder(1500, 6, Buy, "o11"),
          TickOrder(1350, 6, Buy, "o2"),
          TickOrder(1300, 7, Buy, "o3")),
        Seq(
          TickOrder(1510, 2, Sell, "o4"),
          TickOrder(1510, 2, Sell, "o5"),
          TickOrder(1550, 10, Sell, "o6"),
          TickOrder(1600, 15, Sell, "o7")
        )
      )
    )
    ng.maxBidIndex should be (1500)
    ng.minOfferIndex should be (1510)

    ng.send(TickOrder(1500, 15, Sell, "o8")) should be (Seq(
      TickExecution(1500,4, "o1", "o8"),
      TickExecution(1500,6, "o11", "o8")
    ))
    ng.maxBidIndex should be (1350)
    ng.minOfferIndex should be (1500)
    ng.book should be (
      TickBook(
        Seq(
          TickOrder(1350, 6, Buy, "o2"),
          TickOrder(1300, 7, Buy, "o3")
        ),
        Seq(
          TickOrder(1500, 5, Sell, "o8"),
          TickOrder(1510, 2, Sell, "o4"),
          TickOrder(1510, 2, Sell, "o5"),
          TickOrder(1550, 10, Sell, "o6"),
          TickOrder(1600, 15, Sell, "o7")
        )
      )
    )
  }
  test("match a sell, order size == best bid size"){
    val ng = new TickMatchingEngine(5000)
    // create book
    Seq(
      TickOrder(1500, 10, Buy, "o1"),
      TickOrder(1350, 6, Buy, "o2"),
      TickOrder(1300, 7, Buy, "o3"),
      TickOrder(1510, 2, Sell, "o4"),
      TickOrder(1550, 10, Sell, "o6"),
      TickOrder(1600, 15, Sell, "o7"),
    ).foreach{ ng.send(_) }
    ng.book should be (
      TickBook(
        Seq(
          TickOrder(1500, 10, Buy, "o1"),
          TickOrder(1350, 6, Buy, "o2"),
          TickOrder(1300, 7, Buy, "o3")),
        Seq(
          TickOrder(1510, 2, Sell, "o4"),
          TickOrder(1550, 10, Sell, "o6"),
          TickOrder(1600, 15, Sell, "o7")
        )
      )
    )
    ng.send(TickOrder(1500, 10, Sell, "o8")) should be (Seq(
      TickExecution(1500,10, "o1", "o8"),
    ))
    ng.maxBidIndex should be (1350)
    ng.minOfferIndex should be (1510)
    ng.book should be (
      TickBook(
        Seq(
          TickOrder(1350, 6, Buy, "o2"),
          TickOrder(1300, 7, Buy, "o3")
        ),
        Seq(
          TickOrder(1510, 2, Sell, "o4"),
          TickOrder(1550, 10, Sell, "o6"),
          TickOrder(1600, 15, Sell, "o7")
        )
      )
    )
  }
  test("match a sell, sweeps all book and not all get executed"){
    val ng = new TickMatchingEngine(5000)
    // create book
    Seq(
      TickOrder(1500, 4, Buy, "o1"),
      TickOrder(1500, 6, Buy, "o11"),
      TickOrder(1350, 6, Buy, "o2"),
      TickOrder(1300, 7, Buy, "o3"),
      TickOrder(1510, 2, Sell, "o4"),
      TickOrder(1510, 2, Sell, "o5"),
      TickOrder(1550, 10, Sell, "o6"),
      TickOrder(1600, 15, Sell, "o7"),
    ).foreach{ ng.send(_) }
    ng.book should be (
      TickBook(
        Seq(
          TickOrder(1500, 4, Buy, "o1"),
          TickOrder(1500, 6, Buy, "o11"),
          TickOrder(1350, 6, Buy, "o2"),
          TickOrder(1300, 7, Buy, "o3")),
        Seq(
          TickOrder(1510, 2, Sell, "o4"),
          TickOrder(1510, 2, Sell, "o5"),
          TickOrder(1550, 10, Sell, "o6"),
          TickOrder(1600, 15, Sell, "o7")
        )
      )
    )
    ng.maxBidIndex should be (1500)
    ng.minOfferIndex should be (1510)

    ng.send(TickOrder(1250, 25, Sell, "o8")) should be (Seq(
      TickExecution(1500,4, "o1", "o8"),
      TickExecution(1500,6, "o11", "o8"),
      TickExecution(1350,6, "o2", "o8"),
      TickExecution(1300,7, "o3", "o8")
    ))
    ng.maxBidIndex should be (-1) // no bids
    ng.minOfferIndex should be (1250)
    ng.book should be (
      TickBook(
        Nil,
        Seq(
          TickOrder(1250, 2, Sell, "o8"),
          TickOrder(1510, 2, Sell, "o4"),
          TickOrder(1510, 2, Sell, "o5"),
          TickOrder(1550, 10, Sell, "o6"),
          TickOrder(1600, 15, Sell, "o7")
        )
      )
    )
    ng.send(TickOrder(1200, 3, Sell, "o9")) should be (Nil)
    ng.maxBidIndex should be (-1) // no bids
    ng.minOfferIndex should be (1200)
    ng.book should be (
      TickBook(
        Nil,
        Seq(
          TickOrder(1200, 3, Sell, "o9"),
          TickOrder(1250, 2, Sell, "o8"),
          TickOrder(1510, 2, Sell, "o4"),
          TickOrder(1510, 2, Sell, "o5"),
          TickOrder(1550, 10, Sell, "o6"),
          TickOrder(1600, 15, Sell, "o7")
        )
      )
    )

    ng.send(TickOrder(1190, 2, Buy, "o10")) should be (Nil)
    ng.maxBidIndex should be (1190) // no bids
    ng.minOfferIndex should be (1200)
    ng.book should be (
      TickBook(
        Seq(
          TickOrder(1190, 2, Buy, "o10"),
        ),
        Seq(
          TickOrder(1200, 3, Sell, "o9"),
          TickOrder(1250, 2, Sell, "o8"),
          TickOrder(1510, 2, Sell, "o4"),
          TickOrder(1510, 2, Sell, "o5"),
          TickOrder(1550, 10, Sell, "o6"),
          TickOrder(1600, 15, Sell, "o7")
        )
      )
    )
  }
}
