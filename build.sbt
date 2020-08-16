name := "kleisli-playground"

version := "0.1"

scalaVersion := "2.13.3"

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full)

libraryDependencies += "org.typelevel" %% "cats-core" % "2.1.1"
libraryDependencies += "org.tpolecat"  %% "natchez-core" % "0.0.12"
libraryDependencies += "org.tpolecat"  %% "natchez-log" % "0.0.12"
libraryDependencies += "org.http4s" %% "http4s-core" % "0.21.7"
libraryDependencies += "org.http4s" %% "http4s-blaze-server" % "0.21.7"
libraryDependencies += "org.http4s" %% "http4s-dsl" % "0.21.7"
libraryDependencies += "io.chrisdavenport" %% "log4cats-slf4j" % "1.1.1"
libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.6.4"