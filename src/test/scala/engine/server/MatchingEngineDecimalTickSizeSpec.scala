package engine.server

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import play.api.libs.json.{Json}
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._
import engine.{Book, BookEntry, Buy, Sell}
import engine.tick.{TickOrder}

import scala.concurrent.duration._


/**
  * Test with min price != 0, and a decimal ticksize
  */
class MatchingengineDecimalTickSizeSpec extends WordSpec with Matchers with ScalatestRouteTest with BeforeAndAfterAll {

  lazy val customConf = engine.customConf("MatchingengineDecimalTickSizeSpec")

  override def createActorSystem() =
    ActorSystem("MatchingengineDecimalTickSizeSpec", ConfigFactory.load(customConf))

  implicit def default(implicit system: ActorSystem) = RouteTestTimeout(30.seconds)

  val engineConfig = ConfigFactory.parseString(
    """
      |engine.min_price = 5
      |engine.max_price = 50
      |engine.tick_size = 0.1
    """.stripMargin)

  "MatchingEngine with tick size = 0.1" should {

    val server = new MatchingEngine(system,customConf.withFallback(engineConfig))
    "send orders and check book" in {
      Seq(
        TickOrder(1500, 10, Buy, "o1"),
        TickOrder(1350, 6, Buy, "o2"),
        TickOrder(1300, 7, Buy, "o3"),
        TickOrder(1510, 2, Sell, "o4"),
        TickOrder(1510, 6, Sell, "o5"),
        TickOrder(1550, 10, Sell, "o6"),
        TickOrder(1600, 15, Sell, "o7"),
      ).foreach { o =>
        o.side match {
          case Buy =>
            Post("/buy", Json.obj("prc" -> o.prc.toDouble / 100.0, "qty" -> o.qty)) ~> server.routes ~> check {
              status shouldBe StatusCodes.OK
            }
          case Sell =>
            Post("/sell", Json.obj("prc" -> o.prc.toDouble / 100.0, "qty" -> o.qty)) ~> server.routes ~> check {
              status shouldBe StatusCodes.OK
            }
        }
      }
      Get("/book") ~> server.routes ~> check {
        status shouldBe StatusCodes.OK
        responseAs[Book] shouldBe (
          Book(
            buys = Seq(
              BookEntry(10, 15.00),
              BookEntry(6, 13.50),
              BookEntry(7, 13.00)
            ),
            sells = Seq(
              BookEntry(2, 15.10),
              BookEntry(6, 15.10),
              BookEntry(10, 15.50),
              BookEntry(15, 16.00),
            )
          )
          )
      }
      // this crosses
      Post("/buy", Json.obj("prc" -> 1510.0 / 100.0, "qty" -> 3)) ~> server.routes ~> check {
        status shouldBe StatusCodes.OK
      }
      Get("/book") ~> server.routes ~> check {
        status shouldBe StatusCodes.OK
        responseAs[Book] shouldBe (
          Book(
            buys = Seq(
              BookEntry(10, 15.00),
              BookEntry(6, 13.50),
              BookEntry(7, 13.00)
            ),
            sells = Seq(
              BookEntry(5, 15.10),
              BookEntry(10, 15.50),
              BookEntry(15, 16.00),
            )
          )
          )
      }
    }
  }
}
