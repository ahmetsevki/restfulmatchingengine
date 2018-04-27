package engine.tick

import engine.Side

case class TickOrder(prc: Int, qty: Int, side: Side, orderId: String)
