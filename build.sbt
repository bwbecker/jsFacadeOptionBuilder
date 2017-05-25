lazy val root = project.in(file(".")).
  enablePlugins(ScalaJSPlugin)

name := "jsFacadeOptionBuilder Library for Scala.js"

normalizedName := "jsFacadeOptionBuilder"

version := "0.9-SNAPSHOT"

organization := "ca.bwbecker"

scalaVersion := "2.12.2"

crossScalaVersions := Seq("2.11.8", "2.12.2")

homepage := Some(url("https://github.com/bwbecker/jsFacadeOptionBuilder"))

licenses += ("MIT License", url("http://www.opensource.org/licenses/mit-license.php"))

scmInfo := Some(ScmInfo(
    url("https://github.com/bwbecker/jsFacadeOptionBuilder"),
    "scm:git:git@github.com:bwbecker/jsFacadeOptionBuilder.git",
    Some("scm:git:git@github.com:bwbecker/jsFacadeOptionBuilder.git")))

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

pomExtra := (
  <developers>
    <developer>
      <id>jducoeur</id>
      <name>Mark Waks</name>
      <url>https://github.com/jducoeur/</url>
    </developer>
  </developers>
  <contributors>
    <contributor>
      <name>Jasper Moeys</name>
      <url>https://github.com/Jasper-M/</url>
    </contributor>
    <contributor>
      <name>Stefan Larsson</name>
      <url>https://github.com/lastsys/</url>
    </contributor>
    <contributor>
      <name>Byron Weber Becker</name>
      <url>https://github.com/bwbecker/</url>
    </contributor>
  </contributors>
)

pomIncludeRepository := { _ => false }


fork in run := true
