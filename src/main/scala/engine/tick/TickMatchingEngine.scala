package engine.tick

import engine.{Buy, Sell}

import scala.collection.mutable

class TickMatchingEngine(maxPrcInTicks: Int){

  val arraySize: Int = maxPrcInTicks + 1
  val bookArray: Array[TickPriceLevel] = Array.fill[TickPriceLevel](arraySize)(new TickPriceLevel)

  private [tick] var minOfferIndex = maxPrcInTicks + 1 // no offers, offer prc is out of range considering maxPrc
  private [tick] var maxBidIndex = - 1 // no bids, bid prc is out of range considering minPrc

  def send(o: TickOrder): Seq[TickExecution] = o.side match {
    case Buy => buy(o)
    case Sell => sell(o)
  }
  private def buy(o: TickOrder): Seq[TickExecution] = {
    var remainingOrderQty = o.qty
    var executions =  List.empty[TickExecution]
    while (remainingOrderQty > 0 && o.prc >= minOfferIndex){
      bookArray(minOfferIndex)
        .cross(o.copy(qty = remainingOrderQty)) match {
          case execs if execs.nonEmpty =>
            remainingOrderQty -= execs.foldLeft(0)(_ + _.qty)
            executions = execs ++ executions
        }
      while( minOfferIndex <= maxPrcInTicks && bookArray(minOfferIndex).isEmpty)
        minOfferIndex += 1
    }// while
    if (remainingOrderQty > 0){
      bookArray(o.prc).enqueue(o.copy(qty = remainingOrderQty))
      if (o.prc > maxBidIndex)
        maxBidIndex = o.prc
    }
    executions.reverse
  }

  private def sell(o: TickOrder): Seq[TickExecution] = {
    var remainingOrderQty = o.qty
    var executions = List.empty[TickExecution]
    while (remainingOrderQty > 0 && o.prc <= maxBidIndex){
      bookArray(maxBidIndex)
        .cross(o.copy(qty = remainingOrderQty)) match {
        case execs if execs.nonEmpty =>
          remainingOrderQty -= execs.foldLeft(0)(_ + _.qty)
          executions = execs ++ executions
      }
      while( maxBidIndex >= 0 && bookArray(maxBidIndex).isEmpty)
        maxBidIndex -= 1
    }// while
    if (remainingOrderQty > 0){
      bookArray(o.prc).enqueue(o.copy(qty = remainingOrderQty))
      if (o.prc < minOfferIndex)
        minOfferIndex = o.prc
    }
    executions.reverse
  }

  def book = {
    val buys: Seq[TickOrder] = if (maxBidIndex<0) Nil else {
      val buffer = mutable.ArrayBuffer[TickOrder]()
      var i = maxBidIndex
      while(i>=0){
        bookArray(i).orders.map( seqOfOrders => buffer.appendAll(seqOfOrders))
        i-=1
      }
      buffer.toVector
    }
    val sells: Seq[TickOrder] = if (minOfferIndex > maxPrcInTicks) Nil else {
      val buffer = mutable.ArrayBuffer[TickOrder]()
      var i = minOfferIndex
      while(i<=maxPrcInTicks){
        bookArray(i).orders.map( seqOfOrders => buffer.appendAll(seqOfOrders))
        i+=1
      }
      buffer.toVector
    }
    TickBook(buys, sells)
  }
}
