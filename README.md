This is an example matching engine implementation.

## Here are some design points:

- This is a sample implementation. A real matching engine implementation should look into an lockless LMAX Disruptor architecture. We are using akka actors (which have locks at actor mailboxes), mutable and immutable Scala collections (LMAX has its own optimized data structures).

- For performance reasons the actual matching engine is implemented with Int prices that reflect the ticks in the price. For instance, if price is 15.1 and tick size is 0.1, then this price is represented as 151. When creating the matching engine, the minimum and maximum price has to be set as well. The conversion from decimal prices to 'tick' prices are done at the REST entry point (multi threaded, and can be load balanced). The matching engine (`engine.tick.TickMatchingEngine`) has to run on a single thread, thus does not spend time with the conversions.

- We are using akka-persistence and all buy/sell orders are journalled. If the exchange is overwhelmed under load and its stash is overflowing, it sends back a Reject through implementation of `StashOverflowStrategyConfigurator`. Nothing is dropped. All orders are journalled to LevelDB (can be changed to another store through plugins). You can shut-down the server, and bring it up and old orders are replayed to create the current state. We are not taking snapshots (only storing the events, which happen to be the orders)

- Upon receiving and journaling an order, matching engine forwards this to another actor. (In a possible real implementation this probably will be published to a data bus where other replicas get it). The idea is to have multiple copies of the matching engine for backup, failover and querying purposes.

- For performance reasons, the matching engine has to use mutable data structures, which means when engine gets a request for current book, it has has to create immutable data structures before sending them out. We don't want the main engine to spend time doing this, hence a replica engine is created at the server. The requests for book are sent to this replica. We can potentially have a timer on this replica to create the immutable data strcutures periodically and send out the already created book from the previous timer tick.

- The actor that contains the matching engine is run on a PinnedThreadDispatcher so it is always mapped to the same thread (performance considerations)

## Running
The root directory of the project has an `application.conf` file that contains the following:
```bash
engine.min_price = 5
engine.max_price = 50
engine.tick_size = 0.1

akka.persistence.journal.leveldb.dir = "target/sample/journal"
akka.persistence.snapshot-store.local.dir = "target/sample/snapshots"
```
Here you define the minimum and maximum price, and the tick sizes. Also here you put the directory where the journal gets stored. If you want to reset the journal you have to delete the directory, otherwise when you shut down the server and bring it up you will see the orders from previous session. If you change the min/max price for the book or the ticksize, you need to reset the journal. A production implementation would have a migration script. The tests use temporary folders that get deleted on JVM exit. So at project directory:

```bash
rm -rf target/sample
sbt -Dconfig.file=application.conf run
```

To run the tests:
```bash
sbt test
```

## Curl commands to execute sample session
```curl
curl localhost:3000/book
curl localhost:3000/sell --data '{"qty":10,"prc":15}' -H "Content-Type: application/json"
curl localhost:3000/sell --data '{"qty":10,"prc":13}' -H "Content-Type: application/json"
curl localhost:3000/buy  --data '{"qty":10,"prc":7}' -H "Content-Type: application/json"
curl localhost:3000/buy  --data '{"qty":10,"prc":9.5}' -H "Content-Type: application/json"
curl localhost:3000/book
curl localhost:3000/sell --data '{"qty":5, "prc":9.5}' -H "Content-Type: application/json"
curl localhost:3000/book
curl localhost:3000/buy  --data '{"qty":6, "prc":13}' -H "Content-Type: application/json"
curl localhost:3000/book
curl localhost:3000/sell --data '{"qty":7, "prc":7}' -H "Content-Type: application/json"
curl localhost:3000/book
curl localhost:3000/sell --data '{"qty":12, "prc":6}' -H "Content-Type: application/json"
curl localhost:3000/book
```