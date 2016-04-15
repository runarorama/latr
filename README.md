### An example SBT project which uses macros (Scala 2.10/2.11, SBT 0.13)

*In order to enable quasiquotes in Scala 2.10.x, use the macro paradise compiler plugin as outlined in  [https://github.com/scalamacros/sbt-example-paradise](https://github.com/scalamacros/sbt-example-paradise)*.

To verify that everything works fine, do `sbt run`.

Note that currently SBT doesn't support recompilation of macro clients if the dependencies of the macro implementation have changed - macro clients are only recompiled when the macro definition itself is:  https://github.com/sbt/sbt/issues/399.

Huge thanks to Paul Butcher (https://github.com/paulbutcher/ScalaMock/blob/typemacros/project/Build.scala) and Adam Warski (https://github.com/adamw/scala-macro-debug) whose SBT projects I used as prototypes for this one.
