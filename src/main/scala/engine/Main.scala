package engine

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import server.MatchingEngine

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("enginemain")
    implicit val materializer = ActorMaterializer()
    try {
      val server = new MatchingEngine(system, ConfigFactory.load())
      val bindingFuture = Http().bindAndHandle(server.routes, "0.0.0.0", 3000)
      println("Ctrl-C to exit")
      sys.addShutdownHook {
        system.terminate()
      }
    }catch{
      case e: Throwable =>
        println(e.getMessage)
        system.terminate()
    } finally{
      Await.ready(system.whenTerminated, Duration.Inf)
    }
  }
}
