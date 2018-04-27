name := "exengine"

version := "0.1"

scalaVersion := "2.12.5"

resolvers += "Artima Maven Repository" at "http://repo.artima.com/releases"

libraryDependencies += "com.typesafe" % "config" % "1.3.2"
libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.5"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.12"
libraryDependencies += "com.typesafe.akka" %% "akka-persistence" % "2.5.4"
libraryDependencies += "org.iq80.leveldb" % "leveldb" % "0.10"
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.5.12" % Test
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % "10.1.1",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.1.1" % Test
)


libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.9"
libraryDependencies += "de.heikoseeberger" %% "akka-http-play-json" % "1.20.1"
