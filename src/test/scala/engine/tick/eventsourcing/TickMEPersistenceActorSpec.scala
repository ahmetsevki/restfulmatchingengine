package engine.tick.eventsourcing

import java.io.File
import java.nio.file.{Files, Path, Paths}

import akka.actor.{ActorSystem, Props}
import akka.testkit.{DefaultTimeout, ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import engine.{Buy, Sell}
import engine.tick.{TickBook, TickOrder}
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}

object TickMEPersistenceActorSpec{
  def customConf = {
    val dataDir = Files.createTempDirectory("TickMEPersistenceActorSpec").toFile
    dataDir.deleteOnExit()
    ConfigFactory.parseString(
      s"""|
          |akka.actor.default-mailbox.stash-capacity=10000
          |akka.persistence.internal-stash-overflow-strategy="engine.tick.eventsourcing.ReplyToWithRejectConfigurator"
          |
          |akka.persistence.journal.plugin = "akka.persistence.journal.leveldb"
          |akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
          |akka.persistence.journal.leveldb.dir = "target/$dataDir/journal"
          |akka.persistence.snapshot-store.local.dir = "target/$dataDir/snapshots"
          |akka.persistence.journal.leveldb.native = false
          |""".stripMargin)
  }
}
class TickMEPersistenceActorSpec
  extends TestKit(ActorSystem("TickMEPersistenceActorSpec", ConfigFactory.load(TickMEPersistenceActorSpec.customConf)))
  with DefaultTimeout with ImplicitSender with FunSuiteLike with Matchers with BeforeAndAfterAll {


  test("simple start stop along w/ fwd actor") {
    val bookQueryActor1 = system.actorOf(engine.tick.TickMEActor.props(5000))
    val exchange1 = system.actorOf(TickMEPersistenceActor.props(5000, Some(bookQueryActor1)))
    exchange1 ! "book"
    expectMsgType[TickBook] should be (TickBook(Nil, Nil))
    Seq(
      TickOrder(1500, 10, Buy, "o1"),
      TickOrder(1350, 6, Buy, "o2"),
      TickOrder(1300, 7, Buy, "o3"),
      TickOrder(1510, 2, Sell, "o4"),
      TickOrder(1510, 6, Sell, "o5"),
      TickOrder(1550, 10, Sell, "o6"),
      TickOrder(1600, 15, Sell, "o7"),
    ).foreach { o =>
      exchange1 ! o
      expectMsgType[TickMEOrderResponse].status should be ("Ok")
    }

    val bookExpected =
      TickBook(
        Vector(
          TickOrder(1500, 10, Buy, "o1"),
          TickOrder(1350, 6, Buy, "o2"),
          TickOrder(1300, 7, Buy, "o3")),
        Vector(
          TickOrder(1510, 2, Sell, "o4"),
          TickOrder(1510, 6, Sell, "o5"),
          TickOrder(1550, 10, Sell, "o6"),
          TickOrder(1600, 15, Sell, "o7")
        )
      )
    exchange1 ! "book"
    expectMsgType[TickBook] should be(bookExpected)

    bookQueryActor1 ! "book"
    expectMsgType[TickBook] should be(bookExpected)

    system.stop(exchange1)
    system.stop(bookQueryActor1)

    val bookQueryActor2 = system.actorOf(engine.tick.TickMEActor.props(5000))
    val exchange2 = system.actorOf(TickMEPersistenceActor.props(5000, Some(bookQueryActor2)))
    exchange2 ! "book"
    expectMsgType[TickBook] should be( bookExpected )
    bookQueryActor2 ! "book"
    expectMsgType[TickBook] should be( bookExpected )
  }
}
