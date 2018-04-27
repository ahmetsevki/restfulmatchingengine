package engine.tick

import engine.{Buy, Sell}

import scala.collection.mutable


sealed trait CrossResult

/**
  * Maintains the list of orders at a price level.
  */
class TickPriceLevel{
  private val queue = mutable.Queue[TickOrder]()
  def isEmpty: Boolean = queue.isEmpty
  def orders: Option[Seq[TickOrder]] = if (queue.isEmpty) None else Some(queue.toList)
  def enqueue(o: TickOrder): Unit = queue.enqueue(o)
  def cross(o: TickOrder): List[TickExecution] = {
    var remains = o.qty
    var executions = List.empty[TickExecution]
    while(remains > 0 && queue.nonEmpty){
      val queueOrder = queue.dequeue()
      if (queueOrder.qty <= remains){
        val exec = o.side match {
          case Buy =>
            TickExecution(queueOrder.prc, queueOrder.qty, o.orderId, queueOrder.orderId)
          case Sell =>
            TickExecution(queueOrder.prc, queueOrder.qty, queueOrder.orderId, o.orderId)
        }
        executions = exec +: executions
        remains -= queueOrder.qty
      }else{
        val partial = queueOrder.copy(qty = queueOrder.qty - remains)
        val exec = o.side match {
          case Buy =>
            TickExecution(queueOrder.prc, remains, o.orderId, queueOrder.orderId)
          case Sell =>
            TickExecution(queueOrder.prc, remains, queueOrder.orderId, o.orderId)
        }
        executions = exec +: executions
        remains = 0
        partial +=: queue // enqueue the remaining qty as partial order to the head
      }
    }
    executions
  }
}
