import Scalaz._

organization in ThisBuild := "org.scalaz"

version in ThisBuild := "0.1-SNAPSHOT"

publishTo in ThisBuild := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

dynverSonatypeSnapshots in ThisBuild := true

lazy val sonataCredentials = for {
  username <- sys.env.get("SONATYPE_USERNAME")
  password <- sys.env.get("SONATYPE_PASSWORD")
} yield Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", username, password)

credentials in ThisBuild ++= sonataCredentials.toSeq

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")

lazy val root = project
  .in(file("."))
  .settings(
    skip in publish := true
  )
  .aggregate(coreJVM, coreJS, interopJVM, interopJS, interopCatsLaws, benchmarks, microsite)
  .enablePlugins(ScalaJSPlugin)

lazy val core = crossProject
  .in(file("core"))
  .settings(stdSettings("zio"))
  .settings(
    libraryDependencies ++= Seq("org.specs2" %%% "specs2-core"          % "4.3.0" % Test,
                                "org.specs2" %%% "specs2-scalacheck"    % "4.3.0" % Test,
                                "org.specs2" %%% "specs2-matcher-extra" % "4.3.0" % Test),
    scalacOptions in Test ++= Seq("-Yrangepos")
  )

lazy val coreJVM = core.jvm

lazy val coreJS = core.js

lazy val interop = crossProject
  .in(file("interop"))
  .settings(stdSettings("zio-interop"))
  .dependsOn(core % "test->test;compile->compile")
  .settings(
    libraryDependencies ++= Seq(
      "org.scalaz"    %%% "scalaz-core"               % "7.2.+"  % Optional,
      "org.typelevel" %%% "cats-effect"               % "0.10.1" % Optional,
      "org.scalaz"    %%% "scalaz-scalacheck-binding" % "7.2.+"  % Test,
      "co.fs2"        %%% "fs2-core"                  % "0.10.3" % Test
    ),
    scalacOptions in Test ++= Seq("-Yrangepos")
  )

lazy val interopJVM = interop.jvm

lazy val interopJS = interop.js

// Separated due to binary incompatibility in scalacheck 1.13 vs 1.14
// TODO remove it when https://github.com/typelevel/discipline/issues/52 is closed
lazy val interopCatsLaws = project.module
  .in(file("interop-cats-laws"))
  .settings(stdSettings("zio-interop-cats-laws"))
  .dependsOn(coreJVM % "test->test", interopJVM % "test->compile")
  .settings(
    skip in publish := true,
    libraryDependencies ++= Seq(
      "org.typelevel"              %% "cats-effect-laws"          % "0.10.1" % Test,
      "org.typelevel"              %% "cats-testkit"              % "1.1.0"  % Test,
      "com.github.alexarchambault" %% "scalacheck-shapeless_1.13" % "1.1.8"  % Test
    ),
    dependencyOverrides += "org.scalacheck" %% "scalacheck" % "1.13.5" % Test,
    scalacOptions in Test ++= Seq("-Yrangepos")
  )

lazy val benchmarks = project.module
  .dependsOn(coreJVM)
  .enablePlugins(JmhPlugin)
  .settings(
    skip in publish := true,
    libraryDependencies ++=
      Seq(
        "org.scala-lang" % "scala-reflect"  % scalaVersion.value,
        "org.scala-lang" % "scala-compiler" % scalaVersion.value % Provided,
        "io.monix"       %% "monix"         % "3.0.0-RC1",
        "org.typelevel"  %% "cats-effect"   % "1.0.0-RC"
      )
  )

lazy val microsite = project.module
  .dependsOn(coreJVM)
  .enablePlugins(MicrositesPlugin)
  .settings(
    scalacOptions -= "-Yno-imports",
    scalacOptions ~= { _ filterNot (_ startsWith "-Ywarn") },
    scalacOptions ~= { _ filterNot (_ startsWith "-Xlint") },
    skip in publish := true,
    libraryDependencies += "com.github.ghik" %% "silencer-lib" % "1.0",
    micrositeFooterText := Some(
      """
        |<p>&copy; 2018 <a href="https://github.com/scalaz/scalaz-zio">Scalaz Maintainers</a></p>
        |""".stripMargin
    ),
    micrositeName := "Scalaz-ZIO",
    micrositeDescription := "Scalaz-ZIO",
    micrositeAuthor := "Scalaz contributors",
    micrositeOrganizationHomepage := "https://github.com/scalaz/scalaz-zio",
    micrositeGitterChannelUrl := "scalaz/scalaz-zio",
    micrositeGitHostingUrl := "https://github.com/scalaz/scalaz-zio",
    micrositeGithubOwner := "scalaz",
    micrositeGithubRepo := "scalaz-zio",
    micrositeFavicons := Seq(microsites.MicrositeFavicon("favicon.png", "512x512")),
    micrositePalette := Map(
      "brand-primary"   -> "#ED2124",
      "brand-secondary" -> "#251605",
      "brand-tertiary"  -> "#491119",
      "gray-dark"       -> "#453E46",
      "gray"            -> "#837F84",
      "gray-light"      -> "#E3E2E3",
      "gray-lighter"    -> "#F4F3F4",
      "white-color"     -> "#FFFFFF"
    )
  )
