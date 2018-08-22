import sbt._

name := "testJms"

version := "0.1"

scalaVersion := "2.12.4"

//exportJars := true

//retrieveManaged := true


//fullResolvers := {
//  ("JBoss" at "https://repository.jboss.org/nexus/content/groups/public") +: fullResolvers.value
//}

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.6"
libraryDependencies += "com.typesafe" % "config" % "1.3.2"
unmanagedBase := baseDirectory.value / "custom_lib"
//Compile / unmanagedJars := Seq.empty[sbt.Attributed[java.io.File]]
//unmanagedJars in Compile += file("custom_lib/sonic_Client.jar")
unmanagedBase in Compile := baseDirectory.value / "custom_lib"

//libraryDependencies += "javax.jms" % "jms" % "1.1"
//libraryDependencies += "progress.message" % "jms" % "1.1"