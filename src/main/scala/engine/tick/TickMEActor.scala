package engine.tick

import akka.actor.{Actor, Props}

class TickMEActor(maxPrcInTicks: Int) extends Actor{
  val state = new TickMatchingEngine(maxPrcInTicks)
  def receive: Receive = {
    case o: TickOrder =>
      state.send(o)
    case "book" =>
      sender ! state.book
  }
}
object TickMEActor{
  def props(maxPrcInTicks: Int) = Props(new TickMEActor(maxPrcInTicks))
}