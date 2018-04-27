package engine.tick.eventsourcing

import engine.tick.TickExecution

sealed trait TickMEResponse
case object TickMEReject extends TickMEResponse
case class TickMEOrderResponse(status: String, executions: Seq[TickExecution]) extends TickMEResponse


