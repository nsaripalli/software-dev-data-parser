name := "Assignment1"

version := "0.1"

ThisBuild / scalaVersion := "2.13.1"

libraryDependencies ++= Seq(
  "com.github.scopt" %% "scopt" % "4.0.0-RC2",
  "org.scalatest" %% "scalatest" % "3.1.0" % "test"
)

artifactName := { (sv: ScalaVersion, module: ModuleID, artifact: Artifact) =>
  "sorer.jar"
}