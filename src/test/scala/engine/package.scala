import java.nio.file.Files

import com.typesafe.config.ConfigFactory

package object engine {
  def customConf(prefix: String) = {
    val dataDir = Files.createTempDirectory(s"$prefix").toFile
    dataDir.deleteOnExit()
    ConfigFactory.parseString(
      s"""|
          |akka.actor.default-mailbox.stash-capacity=10000
          |akka.persistence.internal-stash-overflow-strategy="engine.tick.eventsourcing.ReplyToWithRejectConfigurator"
          |
          |akka.persistence.journal.plugin = "akka.persistence.journal.leveldb"
          |akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
          |akka.persistence.journal.leveldb.dir = "target/$dataDir/journal"
          |akka.persistence.snapshot-store.local.dir = "target/$dataDir/snapshots"
          |akka.persistence.journal.leveldb.native = false
          |""".stripMargin)
  }
}
