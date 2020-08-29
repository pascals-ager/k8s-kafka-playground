import Dependencies._

lazy val commonSettings = Seq(
  name := "monixMongoStream",
  scalaVersion := "2.12.10",
  organization := "playground",
  scalacOptions ++= Seq(
    // warnings
    "-unchecked", // able additional warnings where generated code depends on assumptions
    "-deprecation", // emit warning for usages of deprecated APIs
    "-feature", // emit warning usages of features that should be imported explicitly
    // Features enabled by default
    "-language:higherKinds",
    "-language:implicitConversions",
    "-language:experimental.macros",
    // possibly deprecated options
    "-Ywarn-dead-code",
    "-language:higherKinds",
    "-language:existentials"
  )
)

resolvers ++= Seq(
  "Typesafe Releases" at "https://repo.typesafe.com/typesafe/releases",
  Resolver.sonatypeRepo("releases")
)

lazy val akkaMongo = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    libraryDependencies ++= monix ++ mongo ++ rxStreams ++ logging
  )