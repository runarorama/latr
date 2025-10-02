lazy val scalaMacroDependencies: Seq[Setting[_]] = Seq(
  version := "0.3.0",
  libraryDependencies ++= Seq(
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
        Seq(compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full))
    }
  },
)

lazy val notPublish = Seq(
  publish / skip := true,
  publishArtifact := false,
  publish := {},
  publishLocal := {}
)

val buildSettings = Seq(
  scalaVersion := "2.11.12",
  crossScalaVersions := Seq("2.11.12", "2.13.16", "2.13.17"),
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
  run := (tests / Compile / run).evaluated
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
