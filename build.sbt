organization := "io.kontainers"

name := "micrometer-akka"

scalaVersion := "2.13.1"

crossScalaVersions := Seq("2.11.12", "2.12.10", scalaVersion.value)

scalacOptions += "-target:jvm-1.8"

def sysPropOrDefault(propName: String, default: String): String = Option(System.getProperty(propName)) match {
  case Some(propVal) if !propVal.trim.isEmpty => propVal.trim
  case _ => default
}

def akkaDefaultVersion(scalaVersion: String) = if (scalaVersion.startsWith("2.13")) "2.5.27" else "2.4.20"
def akkaVersion(scalaVersion: String) = sysPropOrDefault("akka.version", akkaDefaultVersion(scalaVersion))
val aspectjweaverVersion = "1.9.5"
val micrometerVersion = "1.3.2"

checksums in update := Nil

Global / useGpg := false

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "1.7.30",
  "io.micrometer" % "micrometer-core" % micrometerVersion,
  "com.typesafe.akka" %% "akka-actor" % akkaVersion(scalaVersion.value),
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion(scalaVersion.value),
  "com.typesafe" % "config" % "1.4.0",
  "org.aspectj" % "aspectjweaver" % aspectjweaverVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion(scalaVersion.value) % Test,
  "org.scalatest" %% "scalatest" % "3.1.0" % Test,
  "ch.qos.logback" % "logback-classic" % "1.2.3" % Test
)

enablePlugins(JavaAgent)
javaAgents += "org.aspectj" % "aspectjweaver" % aspectjweaverVersion % Test

testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oD")

parallelExecution in Test := false
logBuffered := false

javaOptions in Test += s"""-Dconfig.resource=${sysPropOrDefault("config.resource", "application.conf")}"""

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

homepage := Some(url("https://github.com/kontainers/micrometer-akka"))

licenses := Seq("The Apache Software License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

releasePublishArtifactsAction := PgpKeys.publishSigned.value

pomExtra := (
  <scm>
    <url>git@github.com:kontainers/micrometer-akka.git</url>
    <connection>scm:git:git@github.com:kontainers/micrometer-akka.git</connection>
  </scm>
  <developers>
    <developer>
      <id>pjfanning</id>
      <name>PJ Fanning</name>
      <url>https://github.com/pjfanning</url>
    </developer>
  </developers>
)
