crossScalaVersions in ThisBuild := Seq("2.12.10")
scalaVersion in ThisBuild := (crossScalaVersions in ThisBuild).value.head



lazy val root = project.in(file(".")).
  enablePlugins(ScalaJSPlugin)

name := "jsFacadeOptionBuilder Library for Scala.js"

normalizedName := "jsFacadeOptionBuilder"

version := "0.9.4"

organization := "ca.bwbecker"



val keyFile: File = Path.userHome / ".ssh" / "oat_rsa"

val publishMavenStyle = true

publishTo in ThisBuild := Some(Resolver.ssh(
	"OAT Lib Cross-platform",
	"linux.cs.uwaterloo.ca",
	"/u1/cs-oat/public_html/maven").as("cs-oat", keyFile).withPublishPermissions("0644"))

