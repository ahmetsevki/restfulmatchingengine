package engine.tick.eventsourcing

import akka.persistence.{ReplyToStrategy, StashOverflowStrategy, StashOverflowStrategyConfigurator}
import com.typesafe.config.Config

/**
  * If PersistentActor's stash gets overflown, it will automatically reply to the sender actor with TickMEReject message
  */
class ReplyToWithRejectConfigurator extends StashOverflowStrategyConfigurator {
  override def create(config: Config): StashOverflowStrategy = ReplyToStrategy(TickMEReject)
}
