package engine.tick.eventsourcing

import akka.persistence.{ReplyToStrategy, StashOverflowStrategy, StashOverflowStrategyConfigurator}
import com.typesafe.config.Config

class ReplyToWithRejectConfigurator extends StashOverflowStrategyConfigurator {
  override def create(config: Config): StashOverflowStrategy = ReplyToStrategy(TickMEReject)
}
