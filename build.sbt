lazy val scalaMacroDependencies: Seq[Setting[_]] = Seq(
  version := "0.3.0",
  libraryDependencies ++= Seq(
    "org.typelevel" %% "macro-compat" % "1.1.1",
    "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided",
    "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided"
  ),
  scalacOptions ++= PartialFunction.condOpt(CrossVersion.partialVersion(scalaVersion.value)) {
    case Some((2, n)) if n >= 13 =>
      Seq(
        "-Ymacro-annotations"
      )
  }.toList.flatten,
  libraryDependencies ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      // if scala 2.13.0-M4+ is used, paradise are merged into scala-reflect
      case Some((2, scalaMajor)) if scalaMajor >= 13 =>
        Seq()
      case _ =>
        Seq(compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full))
    }
  },
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

lazy val notPublish = Seq(
  skip in publish := true,
  publishArtifact := false,
  publish := {},
  publishLocal := {}
)

val buildSettings = Seq(
  scalaVersion := "2.11.12",
  crossScalaVersions := Seq("2.10.7", "2.11.12", "2.12.8", "2.13.0-RC1"),
  resolvers += Resolver.sonatypeRepo("snapshots"),
  resolvers += Resolver.sonatypeRepo("releases"),
  scalacOptions ++= Seq("-deprecation"),
  licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT")),
  publishMavenStyle := true
) ++ scalaMacroDependencies

lazy val root: Project = Project(
  "root",
  file(".")
).settings(
  buildSettings,
  notPublish,
  run := (run in Compile in tests).evaluated
).aggregate(macros, tests)

lazy val macros: Project = Project(
  "macros",
  file("macros")
).settings(
  buildSettings,
  organization := "com.higher-order",
  name := "latr"
)

lazy val tests: Project = Project(
  "tests",
  file("tests")
).settings(
  buildSettings,
  notPublish
).dependsOn(macros)
