import org.scalajs.core.tools.linker.ModuleKind
import sbt.Keys.{libraryDependencies, resolvers}
import sbtcrossproject.CrossPlugin.autoImport.crossProject
import sbtsonar.SonarPlugin.autoImport.sonarProperties

val ivyLocal = Resolver.file("ivy", file(Path.userHome.absolutePath + "/.ivy2/local"))(Resolver.ivyStylePatterns)

name := "amf-aml"

version in ThisBuild := {
  val major = 4
  val minor = 1

  lazy val build = sys.env.getOrElse("BUILD_NUMBER", "0")
  lazy val branch = sys.env.get("BRANCH_NAME")

  if (branch.contains("master")) s"$major.$minor.$build" else s"$major.${minor + 1}.0-SNAPSHOT"
}

publish := {}

jsEnv := new org.scalajs.jsenv.nodejs.NodeJSEnv()

libraryDependencies += "org.codehaus.sonar.runner" % "sonar-runner-dist" % "2.4"

lazy val sonarUrl = sys.env.getOrElse("SONAR_SERVER_URL", "Not found url.")
lazy val sonarToken = sys.env.getOrElse("SONAR_SERVER_TOKEN", "Not found token.")

sonarProperties ++= Map(
  "sonar.login" -> sonarToken,
  "sonar.projectKey" -> "mulesoft.amf-aml",
  "sonar.projectName" -> "AMF-AML",
  "sonar.projectVersion" -> "4.0.0",
  "sonar.sourceEncoding" -> "UTF-8",
  "sonar.github.repository" -> "mulesoft/amf-aml",
  "sonar.sources" -> "shared/src/main/scala",
  "sonar.tests" -> "shared/src/test/scala",
  "sonar.scala.coverage.reportPaths" -> "jvm/target/scala-2.12/scoverage-report/scoverage.xml"
)

lazy val workspaceDirectory: File =
  sys.props.get("sbt.mulesoft") match {
    case Some(x) => file(x)
    case _       => Path.userHome / "mulesoft"
  }

val amfCoreVersion = "4.1.116"

lazy val amfCoreJVMRef = ProjectRef(workspaceDirectory / "amf-core", "coreJVM")
lazy val amfCoreJSRef = ProjectRef(workspaceDirectory / "amf-core", "coreJS")
lazy val amfCoreLibJVM = "com.github.amlorg" %% "amf-core" % amfCoreVersion
lazy val amfCoreLibJS = "com.github.amlorg" %% "amf-core_sjs0.6" % amfCoreVersion

val settings = Common.settings ++ Common.publish ++ Seq(
  organization := "com.github.amlorg",
  resolvers ++= List(ivyLocal, Common.releases, Common.snapshots, Resolver.mavenLocal),
//  resolvers += "jitpack" at "https://jitpack.io",
  credentials ++= Common.credentials(),
  libraryDependencies ++= Seq(
    "org.scalatest" %%% "scalatest" % "3.0.5" % Test,
    "com.github.scopt" %%% "scopt" % "3.7.0"
  )
)

/** **********************************************
  * AMF-Core
  * ********************************************* */
lazy val defaultProfilesGenerationTask = TaskKey[Unit](
  "defaultValidationProfilesGeneration",
  "Generates the validation dialect documents for the standard profiles")

/** **********************************************
  * AMF-AML
  * ********************************************* */
lazy val aml = crossProject(JSPlatform, JVMPlatform)
  .settings(Seq(
    name := "amf-aml"
  ))
  .in(file("."))
  .settings(settings)
  .jvmSettings(
    libraryDependencies += "org.scala-js" %% "scalajs-stubs" % scalaJSVersion % "provided",
    libraryDependencies += "org.scala-lang.modules" % "scala-java8-compat_2.12" % "0.8.0",
    artifactPath in (Compile, packageDoc) := baseDirectory.value / "target" / "artifact" / "amf-aml-javadoc.jar"
  )
  .jsSettings(
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.2",
    scalaJSModuleKind := ModuleKind.CommonJSModule,
    artifactPath in (Compile, fullOptJS) := baseDirectory.value / "target" / "artifact" / "amf-aml-module.js",
    scalacOptions += "-P:scalajs:suppressExportDeprecations"
  ).disablePlugins(SonarPlugin)

lazy val amlJVM =
  aml.jvm.in(file("./jvm")).sourceDependency(amfCoreJVMRef, amfCoreLibJVM)

lazy val amlJS =
  aml.js.in(file("./js")).sourceDependency(amfCoreJSRef, amfCoreLibJS).disablePlugins(SonarPlugin)
