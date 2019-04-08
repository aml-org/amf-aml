import java.io.FileOutputStream
import java.util.Properties

import org.scalajs.core.tools.linker.ModuleKind
import sbt.Keys.{libraryDependencies, resolvers}
import sbtcrossproject.CrossPlugin.autoImport.crossProject

import scala.collection.JavaConversions
val ivyLocal = Resolver.file("ivy", file(Path.userHome.absolutePath + "/.ivy2/local"))(Resolver.ivyStylePatterns)

name := "amf-aml"

version in ThisBuild := {
  val mayor = 4
  val minor = 0

  lazy val buildNumber = sys.env.getOrElse("BUILD_NUMBER", "0")
  lazy val branchName = sys.env.getOrElse("BRANCH_NAME", "fake")
  println("Build number is: "+buildNumber)
  println("Branch number is: "+branchName)

  val v = {
    if(branchName == "master")
      mayor.toString + "." + minor.toString + "." + buildNumber
    else
      mayor.toString + "." + (minor +1).toString + ".0-SNAPSHOT"
  }
  println("Setting version to : "+v)
  v
}

publish := {}

jsEnv := new org.scalajs.jsenv.nodejs.NodeJSEnv()

libraryDependencies += "org.codehaus.sonar.runner" % "sonar-runner-dist" % "2.4"

enablePlugins(SonarRunnerPlugin)

val setSonarProperties = TaskKey[Unit](
  "setSonarProperties",
  "Set sonar properties!"
)

setSonarProperties := {
  lazy val url = sys.env.getOrElse("SONAR_SERVER_URL", "Not found url.")
  lazy val token = sys.env.getOrElse("SONAR_SERVER_TOKEN", "Not found token.")

  val values = Map(
    "sonar.host.url" -> url,
    "sonar.login" -> token,
    "sonar.projectKey" -> "mulesoft.amf-aml",
    "sonar.projectName" -> "AMF-AML",
    "sonar.projectVersion" -> "4.0.0",

    "sonar.sourceEncoding" -> "UTF-8",
    "sonar.github.repository" -> "mulesoft/amf-aml",

    "sonar.sources" -> "shared/src/main/scala",
    "sonar.tests" -> "shared/src/test/scala",
    "scoverage.reportPath" -> "jvm/target/scala-2.12/scoverage-report/scoverage.xml"
  )

  sonarProperties := values

  val p = new Properties()
  values.foreach(v => p.put(v._1, v._2))
  val stream = new FileOutputStream(file("./sonar-project.properties"))
  p.store(stream, null)
  stream.close()
}

val sonarMe = TaskKey[Unit](
  "sonarMe",
  "Run sonar!")
sonarMe := {

//  sonarRunnerOptions := Seq(
//    "-D",
//    s"sonar.host.url=$url",
//    "-D",
//    s"sonar.login=$token"
//  )

//  val a = generateSonarConfiguration.value

  setSonarProperties.value
  sonar.value
}

val settings = Common.settings ++ Common.publish ++ Seq(
  organization := "com.github.amlorg",
  resolvers ++= List(ivyLocal, Common.releases, Common.snapshots, Resolver.mavenLocal),
  resolvers += "jitpack" at "https://jitpack.io",
  credentials ++= Common.credentials(),
  libraryDependencies ++= Seq(
    "org.scalatest"    %%% "scalatest" % "3.0.5" % Test,
    "com.github.scopt" %%% "scopt"     % "3.7.0",
    "com.github.amlorg" %%% "amf-core" % "4.0.2"
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
    libraryDependencies += "org.scala-js"           %% "scalajs-stubs"          % scalaJSVersion % "provided",
    libraryDependencies += "org.scala-lang.modules" % "scala-java8-compat_2.12" % "0.8.0",
    libraryDependencies += "org.json4s"             %% "json4s-native"          % "3.5.4",
    artifactPath in (Compile, packageDoc) := baseDirectory.value / "target" / "artifact" / "amf-aml-javadoc.jar"
  )
  .jsSettings(
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "0.9.2",
    scalaJSModuleKind := ModuleKind.CommonJSModule,
    artifactPath in (Compile, fullOptJS) := baseDirectory.value / "target" / "artifact" / "amf-aml-module.js"
  )

lazy val amlJVM = aml.jvm.in(file("./jvm"))
lazy val amlJS  = aml.js.in(file("./js"))

