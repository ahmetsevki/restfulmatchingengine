package engine.tick.eventsourcing

import akka.actor.{ActorRef, Props}
import akka.persistence.{PersistentActor, SnapshotOffer}
import engine.tick.{TickExecution, TickMatchingEngine, TickOrder}


/**
  *
  * @param maxPrcInTicks The TickMatchingEngine parameter. To this engine, all prices are from 0 to maxPrcInTicks
  * @param fwdActor an optional actor reference where orders are broadcast after getting written to journal.
  */
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
      sender ! TickMEOrderResponse(lastExecutions)
    case "book" =>
      sender ! state.book
  }
}

object TickMEPersistenceActor{
  def props(maxPrcInTicks: Int, fwdActor: Option[ActorRef]) = Props(new TickMEPersistenceActor(maxPrcInTicks, fwdActor))
}