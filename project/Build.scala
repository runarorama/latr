import sbt._
import Keys._
import bintray.BintrayPlugin.bintraySettings

object BuildSettings {
  val buildSettings = Defaults.defaultSettings ++ Seq(
    scalaVersion := "2.11.8",
    crossScalaVersions := Seq("2.10.2", "2.10.3", "2.10.4", "2.10.5", "2.10.6", "2.11.0", "2.11.1", "2.11.2", "2.11.3", "2.11.4", "2.11.5", "2.11.6", "2.11.7", "2.11.8"),
    resolvers += Resolver.sonatypeRepo("snapshots"),
    resolvers += Resolver.sonatypeRepo("releases"),
    scalacOptions ++= Seq("-deprecation"),
    licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT")),
    publishMavenStyle := true,
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
  )
}

object MyBuild extends Build {
  import BuildSettings._

  lazy val root: Project = Project(
    "root",
    file("."),
    settings = buildSettings ++ Seq(
      run <<= run in Compile in tests)
  ) aggregate(macros, tests)

  lazy val macros: Project = Project(
    "macros",
    file("macros"),
    settings = (buildSettings ++ Seq(
      organization := "com.higher-order",
      name := "latr",
      version := "0.2.1",
      libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-reflect" % _),
      libraryDependencies := {
        CrossVersion.partialVersion(scalaVersion.value) match {
          // if Scala 2.11+ is used, quasiquotes are available in the standard distribution
          case Some((2, scalaMajor)) if scalaMajor >= 11 =>
            libraryDependencies.value
          // in Scala 2.10, quasiquotes are provided by macro paradise
          case Some((2, 10)) =>
            libraryDependencies.value ++ Seq(
              compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
              "org.scalamacros" %% "quasiquotes" % "2.1.0" cross CrossVersion.binary)
        }
      }
    ))
  ).settings(bintraySettings: _*)

  lazy val tests: Project = Project(
    "tests",
    file("tests"),
    settings = buildSettings
  ) dependsOn(macros)
}
