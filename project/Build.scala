import sbt._
import Keys._
import bintray.BintrayPlugin.bintraySettings

object BuildSettings {
  lazy val scalaMacroDependencies: Seq[Setting[_]] = Seq(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "macro-compat" % "1.1.1",
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided",
      "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided",
      compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
    ),
    libraryDependencies ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        // if scala 2.11+ is used, quasiquotes are merged into scala-reflect
        case Some((2, scalaMajor)) if scalaMajor >= 11 => Seq()
        // in Scala 2.10, quasiquotes are provided by macro paradise
        case Some((2, 10)) =>
          Seq("org.scalamacros" %% "quasiquotes" % "2.1.0" cross CrossVersion.binary)
      }
    }
  )

  val buildSettings = Defaults.defaultSettings ++ Seq(
    scalaVersion := "2.11.8",
    crossScalaVersions := Seq("2.10.6", "2.11.8", "2.12.0-M4"),
    resolvers += Resolver.sonatypeRepo("snapshots"),
    resolvers += Resolver.sonatypeRepo("releases"),
    scalacOptions ++= Seq("-deprecation"),
    licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT")),
    publishMavenStyle := true
  ) ++ scalaMacroDependencies
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
      version := "0.2.2"
    ))
  ).settings(bintraySettings: _*)

  lazy val tests: Project = Project(
    "tests",
    file("tests"),
    settings = buildSettings
  ) dependsOn(macros)
}
