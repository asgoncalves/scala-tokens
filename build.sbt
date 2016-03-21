import _root_.sbt.Resolver

name := "scala-tokens"

version := "0.4-SNAPSHOT"

isSnapshot := true

scalaVersion := "2.11.7"

resolvers ++= Seq(
  Resolver.mavenLocal
)

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

publishMavenStyle := true

libraryDependencies ++= Seq(
  "org.scalatest"        %% "scalatest"     % "2.2.4",
  "com.typesafe.akka"    %% "akka-agent"    % "2.3.12",
  "com.typesafe.akka"    %% "akka-actor"    % "2.3.12",
  "com.typesafe.akka"    %% "akka-testkit"  % "2.3.12" % "test",
  "io.spray"             %% "spray-json"    % "1.3.2",
  "io.spray"             %% "spray-client"  % "1.3.3",
  "joda-time"            % "joda-time"      % "2.8.1"
)

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := (
  <url>https://github.com/asgoncalves/scala-tokens</url>
  <licenses>
    <license>
      <name>MIT</name>
      <url>http://opensource.org/licenses/MIT</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:asgoncalves/scala-tokens.git</url>
    <connection>scm:git:git@github.com:asgoncalves/scala-tokens.git</connection>
  </scm>)