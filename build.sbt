name := "fs2-demo"

version := "0.1"

scalaVersion := "2.12.14"

val http4sVersion = "0.23.23"

// Add the version for ScalaTest
val scalatestVersion = "3.2.9"
val Fs2Version = "3.2.4"

lazy val protobuf =
  project
    .in(file("protobuf"))
    .settings(
      name := "protobuf",
      scalaVersion := scalaVersion.value,
    )
    .enablePlugins(Fs2Grpc)

lazy val root =
  project
    .in(file("."))
    .settings(
      name := "root",
      scalaVersion := scalaVersion.value,
      libraryDependencies ++= Seq(
        "io.grpc" % "grpc-netty-shaded" % scalapb.compiler.Version.grpcJavaVersion,
        "co.fs2" %% "fs2-core" % Fs2Version,
        "org.typelevel" %% "cats-effect" % "3.2.0",
       // "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion,
        "org.scalatest" %% "scalatest" % scalatestVersion % Test  // Add ScalaTest dependency
      ),
      testFrameworks += new TestFramework("org.scalatest.tools.Framework") // Use ScalaTest framework
    )
    .dependsOn(protobuf)


