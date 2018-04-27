package engine.server


import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, Route}
import com.typesafe.config.{Config}
import play.api.libs.json._
import engine.tick.{TickBook, TickMEActor, TickOrder}
import engine.tick.eventsourcing.{TickMEOrderResponse, TickMEPersistenceActor, TickMEReject, TickMEResponse}
import engine._

import scala.concurrent.{Future}
import scala.concurrent.duration._


class MatchingEngine(system: ActorSystem, config: Config) extends Directives with CORSHandler {

  import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport._

  implicit val ec = scala.concurrent.ExecutionContext.Implicits.global


  val minPrcDecimal: BigDecimal = BigDecimal(config.getString("engine.min_price"))
  val maxPrcDecimal: BigDecimal = BigDecimal(config.getString("engine.max_price"))
  val tickSize: Double = config.getDouble("engine.tick_size")
  val decimalToTicks = new DecimalToTicks(minPrcDecimal, maxPrcDecimal, tickSize)


  // "book" requests will go to a separate actor, we don't want to bother the matching engine with book serialization
  // this could be a set of actors, or a message bus
  val queryActor: ActorRef =
  system.actorOf(TickMEActor.props(decimalToTicks.maxPrcInTicks))


  val tickEngineActor: ActorRef =
    system.actorOf(
      TickMEPersistenceActor.props(decimalToTicks.maxPrcInTicks, Some(queryActor))
        .withDispatcher("engine.tick-exchange-pinned-dispatcher"))

  implicit val askTimeout: akka.util.Timeout = 10.seconds
  lazy val routes: Route =
    corsHandler {
      {

        path("book") {
          get {
            val response: Future[Book] =
              (queryActor ? "book").mapTo[TickBook].map(tickBook =>
                Book(
                  buys = tickBook.buys.map(b => BookEntry(b.qty, decimalToTicks.ticksToPrc(b.prc))),
                  sells = tickBook.sells.map(b => BookEntry(b.qty, decimalToTicks.ticksToPrc(b.prc))),
                )
              )
            onSuccess(response) { book =>
              complete(book)
            }
          }
        } ~ path(Map("buy" -> Buy, "sell" -> Sell)) { orderSide =>
          post {
            entity(as[JsObject]) { js =>
              val prc = (js \ "prc").as[BigDecimal]
              val qty = (js \ "qty").as[Int]
              decimalToTicks.prcToTicks(prc) match {
                case None => complete(StatusCodes.BadRequest)
                case Some(tickPrc) =>
                  val tickOrder = TickOrder(tickPrc, qty, orderSide, (js \ "orderId").asOpt[String].getOrElse("noid"))
                  val response: Future[TickMEResponse] = (tickEngineActor ? tickOrder).mapTo[TickMEResponse]
                  onSuccess(response) {
                    case TickMEOrderResponse(_, executions) =>
                      // TODO: you can report executions here
                      complete(StatusCodes.OK)
                    case TickMEReject =>
                      complete(StatusCodes.ServiceUnavailable)
                  }
              }
            }
          }
        }
      }
    }
}

