package engine.tick.eventsourcing

import engine.tick.TickExecution

sealed trait TickMEResponse
/** PersistentActor sends this in response to a command if the stash is overflown */
case object TickMEReject extends TickMEResponse

/** response to a buy or sell TickOrder command */
case class TickMEOrderResponse(executions: Seq[TickExecution]) extends TickMEResponse


