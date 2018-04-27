package engine.tick.eventsourcing

import java.nio.file.Files

import akka.actor.ActorSystem
import akka.testkit.{DefaultTimeout, ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import engine.{Buy, Sell}
import engine.tick.{TickBook, TickOrder}
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}

/**
  * simulate an overflow case, make stash queue only 3 long
  */
object TickMEPersistenceStashOverflowSpec{
  val customConf =
      ConfigFactory.parseString(
        s"""|
            |akka.actor.default-mailbox.stash-capacity=3
            |""".stripMargin
    ).withFallback(engine.customConf("TickMEPersistenceStashOverflowSpec"))
}

/**
  * after 3rd message we should have overflown the stash and receive a "RejectToStash" message
  */
class TickMEPersistenceStashOverflowSpec
  extends TestKit(ActorSystem("TickMEPersistenceStashOverflowSpec", ConfigFactory.load(TickMEPersistenceStashOverflowSpec.customConf)))
  with DefaultTimeout with ImplicitSender with FunSuiteLike with Matchers with BeforeAndAfterAll {
  test("simple start stop along w/ fwd actor") {
    val bookQueryActor1 = system.actorOf(engine.tick.TickMEActor.props(50000))
    val exchange1 = system.actorOf(TickMEPersistenceActor.props(50000, Some(bookQueryActor1)))
    exchange1 ! "book"
    expectMsgType[TickBook] should be(TickBook(Nil, Nil))
    val count = 100
    (1 to count).foreach { _ =>
      val o = TickOrder(1500, 10, Buy, "o1")
      exchange1 ! o
    }
    var totalMsgsRecv = 0
    var rejects = 0
    (1 to count).foreach { _ =>
      val msg = expectMsgType[TickMEResponse]
      msg match {
        case TickMEReject => rejects += 1
        case _ =>
      }
//      if (msg.status == "RejectToStash")
//        rejects += 1
      totalMsgsRecv+=1
    }
    totalMsgsRecv should equal (count)

    // we will have 6 rejected orders, 1 processing, 3 in the queue, the rest is rejected.
    // however since this is an asynchronous test we can't be sure
    // if you want to track exact count of rejected and test them look at here:
    // https://github.com/akka/akka/blob/009214ae07708e8144a279e71d06c4a504907e31/akka-persistence/src/test/scala/akka/persistence/PersistentActorBoundedStashingSpec.scala
    rejects should be > 0

    system.stop(exchange1)
    system.stop(bookQueryActor1)

    /** when the exchange comes up, it should not have the rejected -due to queue overflow- orders */
    val exchange2 = system.actorOf(TickMEPersistenceActor.props(50000, None))
    exchange2 ! "book"
    expectMsgType[TickBook].buys.length should be (count - rejects)
  }
}
