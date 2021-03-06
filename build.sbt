def removeSnapshot(str:     String): String = if (str.endsWith("-SNAPSHOT")) str.substring(0, str.length - 9) else str
def katLibDependecy(module: String) = "com.github.Katrix-.KatLib" % s"katlib-$module" % "c4e680ab85" % Provided

lazy val publishResolver = {
  val artifactPattern = s"""${file("publish").absolutePath}/[revision]/[artifact]-[revision](-[classifier]).[ext]"""
  Resolver.file("publish").artifacts(artifactPattern)
}

lazy val commonSettings = Seq(
    name := s"HomeSweetHome-${removeSnapshot(spongeApiVersion.value)}",
    organization := "io.github.katrix",
    version := "2.1.0",
    scalaVersion := "2.12.2",
    resolvers += "jitpack" at "https://jitpack.io",
    libraryDependencies += katLibDependecy("shared"),
    libraryDependencies += "org.jetbrains" % "annotations" % "15.0" % Provided,
    scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-Xlint", "-Yno-adapted-args", "-Ywarn-dead-code", "-Ywarn-unused-import"),
    crossPaths := false,
    assemblyShadeRules in assembly := Seq(
      ShadeRule.rename("scala.**"     -> "io.github.katrix.katlib.shade.scala.@1").inAll,
      ShadeRule.rename("shapeless.**" -> "io.github.katrix.katlib.shade.shapeless.@1").inAll
    ),
    autoScalaLibrary := false,
    publishTo := Some(publishResolver),
    publishArtifact in makePom := false,
    publishArtifact in (Compile, packageBin) := false,
    publishArtifact in (Compile, packageDoc) := false,
    publishArtifact in (Compile, packageSrc) := false,
    artifact in (Compile, assembly) := {
    val art = (artifact in (Compile, assembly)).value
    art.copy(`classifier` = Some("assembly"))
  },
    spongePluginInfo := spongePluginInfo.value.copy(
      id = "homesweethome",
      name = Some("HomeSweetHome"),
      version = Some(s"${removeSnapshot(spongeApiVersion.value)}-${version.value}"),
      authors = Seq("Katrix"),
      dependencies = Set(
        DependencyInfo("spongeapi", Some(removeSnapshot(spongeApiVersion.value))),
        DependencyInfo("katlib", Some(s"${removeSnapshot(spongeApiVersion.value)}-2.0.1"))
      )
    )
  ) ++ addArtifact(artifact in (Compile, assembly), assembly)

lazy val homeShared = (project in file("shared"))
  .enablePlugins(SpongePlugin)
  .settings(
    commonSettings,
    name := "HomeSweetHome-Shared",
    publishArtifact := false,
    assembleArtifact := false,
    spongeMetaCreate := false,
    publish := {},
    publishLocal := {},
    //Default version, needs to build correctly against all supported versions
    spongeApiVersion := "4.1.0"
  )

lazy val homeV410 = (project in file("4.1.0"))
  .enablePlugins(SpongePlugin)
  .dependsOn(homeShared)
  .settings(commonSettings, spongeApiVersion := "4.1.0", libraryDependencies += katLibDependecy("4-1-0"))

lazy val homeV500 = (project in file("5.0.0"))
  .enablePlugins(SpongePlugin)
  .dependsOn(homeShared)
  .settings(commonSettings, spongeApiVersion := "5.0.0", libraryDependencies += katLibDependecy("5-0-0"))

lazy val homeV600 = (project in file("6.0.0"))
  .enablePlugins(SpongePlugin)
  .dependsOn(homeShared)
  .settings(commonSettings, spongeApiVersion := "6.0.0-SNAPSHOT", libraryDependencies += katLibDependecy("6-0-0"))

lazy val homeRoot = (project in file("."))
  .settings(publishArtifact := false, assembleArtifact := false, spongeMetaCreate := false, publish := {}, publishLocal := {})
  .disablePlugins(AssemblyPlugin)
  .aggregate(homeV410, homeV500, homeV600)
