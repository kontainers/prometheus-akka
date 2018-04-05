organization := "com.workday"

name := "micrometer-akka"

scalaVersion := "2.12.4"

crossScalaVersions := Seq("2.11.12", "2.12.4", "2.13.0-M3")

scalacOptions += "-target:jvm-1.8"

def sysPropOrDefault(propName: String, default: String): String = Option(System.getProperty(propName)) match {
  case Some(propVal) if !propVal.trim.isEmpty => propVal.trim
  case _ => default
}

def akkaDefaultVersion(scalaVersion: String) = if (scalaVersion.startsWith("2.13")) "2.5.11" else "2.4.20"
def akkaVersion(scalaVersion: String) = sysPropOrDefault("akka.version", akkaDefaultVersion(scalaVersion))
val aspectjweaverVersion = "1.8.13"
val micrometerVersion = "1.0.3"

checksums in update := Nil

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "1.7.25",
  "io.micrometer" % "micrometer-core" % micrometerVersion,
  "com.typesafe.akka" %% "akka-actor" % akkaVersion(scalaVersion.value),
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion(scalaVersion.value),
  "com.typesafe" % "config" % "1.3.3",
  "org.aspectj" % "aspectjweaver" % aspectjweaverVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion(scalaVersion.value) % "test",
  "org.scalatest" %% "scalatest" % "3.0.5-M1" % "test",
  "ch.qos.logback" % "logback-classic" % "1.2.3" % "test"
)

enablePlugins(JavaAgent)
javaAgents += "org.aspectj" % "aspectjweaver" % aspectjweaverVersion % "test"

testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oD")

parallelExecution in Test := false
logBuffered := false

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

homepage := Some(url("https://github.com/Workday/prometheus-akka"))

licenses := Seq("The Apache Software License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt"))

releasePublishArtifactsAction := PgpKeys.publishSigned.value

pomExtra := (
  <scm>
    <url>git@github.com:Workday/prometheus-akka.git</url>
    <connection>scm:git:git@github.com:Workday/prometheus-akka.git</connection>
  </scm>
  <developers>
    <developer>
      <id>pjfanning</id>
      <name>PJ Fanning</name>
      <url>https://github.com/pjfanning</url>
    </developer>
  </developers>
)
