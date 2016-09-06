name := "dragongame"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.4"

libraryDependencies += "com.typesafe" % "config" % "1.3.0"

libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "4.5.2"

libraryDependencies += "com.fasterxml.jackson.core" % "jackson-annotations" % "2.8.0"

libraryDependencies += "com.fasterxml.jackson.core" % "jackson-core" % "2.8.2"
libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.8.2"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.0" % "test" exclude("org.scala-lang.modules", "scala-xml_2.11")

// libraryDependencies += "org.mapdb" % "mapdb" % "3.0.1"