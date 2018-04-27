package engine.tick.eventsourcing

import akka.actor.{ActorRef, Props}
import akka.persistence.{PersistentActor, SnapshotOffer}
import engine.tick.{TickBook, TickExecution, TickMatchingEngine, TickOrder}

import scala.concurrent.Promise

class TickMEPersistenceActor(maxPrcInTicks: Int, fwdActor: Option[ActorRef]) extends PersistentActor{
  override val persistenceId = "engine-id"
  val state = new TickMatchingEngine(maxPrcInTicks)
  var lastExecutions: Seq[TickExecution] = Nil

  def updateState(o: TickOrder): Unit = {
    lastExecutions = state.send(o)
  }
  val receiveRecover: Receive = {
    case order: TickOrder =>
      updateState(order)
      fwdActor.foreach( _ ! order)
    case a: SnapshotOffer => ??? // not implemented
  }
  val receiveCommand: Receive = {
    case o: TickOrder =>
      persist(o)(updateState)
      fwdActor.foreach( _ ! o)
      sender ! TickMEOrderResponse("Ok", lastExecutions)
    case "book" =>
      sender ! state.book
  }
}

object TickMEPersistenceActor{
  def props(maxPrcInTicks: Int, fwdActor: Option[ActorRef]) = Props(new TickMEPersistenceActor(maxPrcInTicks, fwdActor))
}