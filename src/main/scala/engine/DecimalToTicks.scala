package engine

class DecimalToTicks(minPrcDecimal: BigDecimal, maxPrcDecimal: BigDecimal, tickSize: Double) {
  require( maxPrcDecimal > minPrcDecimal, "'(maxPrc - minPrc) / tickSize' should be an integer")
  require(((maxPrcDecimal - minPrcDecimal) / tickSize).isValidInt)

  val maxPrcInTicks =
    ((maxPrcDecimal - minPrcDecimal) / tickSize).toInt

  def prcToTicks(prc: BigDecimal): Option[Int] = {
    if (prc > maxPrcDecimal || prc < minPrcDecimal)
      None
    else {
      val ticks: BigDecimal = (prc - minPrcDecimal) / tickSize
      if (!ticks.isValidInt)
        None
      else
        Some(ticks.toInt)
    }
  }

  val tickSizeDecimal = BigDecimal(tickSize)
  def ticksToPrc(ticks: Int): BigDecimal = tickSizeDecimal * ticks + minPrcDecimal

  def orderToTickOrder(decimalOrder: Order): Option[tick.TickOrder] = {
    prcToTicks(decimalOrder.prc).map( tickPrc => tick.TickOrder(tickPrc, decimalOrder.qty, decimalOrder.side, decimalOrder.orderId))
  }
  def tickOrderToOrder(tickOrder: tick.TickOrder): Order =
    Order(ticksToPrc(tickOrder.prc), tickOrder.qty, tickOrder.side, tickOrder.orderId)

  def tickExecutionToExecution(texec: tick.TickExecution): Execution =
    Execution(ticksToPrc(texec.prc), texec.qty, texec.buyerOrderId, texec.sellerOrderId)

}
