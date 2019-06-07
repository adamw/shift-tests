lazy val commonSettings = commonSmlBuildSettings ++ Seq(
  organization := "com.softwaremill",
  scalaVersion := "2.12.8"
)

lazy val rootProject = (project in file("."))
  .settings(commonSettings: _*)
  .settings(publishArtifact := false, name := "shift-tests")
  .aggregate(core)

lazy val core: Project = (project in file("core"))
  .settings(commonSettings: _*)
  .settings(
    name := "core",
    libraryDependencies ++= Seq(
      "org.scalaz" %% "scalaz-zio" % "1.0-RC5",
      "org.typelevel" %% "cats-effect" % "1.3.1"
    )
  )

