import org.scalajs.core.tools.linker.ModuleKind
import sbt.Keys.{libraryDependencies, resolvers}
import sbtcrossproject.CrossPlugin.autoImport.crossProject
import sbtsonar.SonarPlugin.autoImport.sonarProperties

val ivyLocal = Resolver.file("ivy", file(Path.userHome.absolutePath + "/.ivy2/local"))(Resolver.ivyStylePatterns)

name := "amf-aml"

version in ThisBuild := "5.1.4-RC.0"

publish := {}

jsEnv := new org.scalajs.jsenv.nodejs.NodeJSEnv()

lazy val sonarUrl   = sys.env.getOrElse("SONAR_SERVER_URL", "Not found url.")
lazy val sonarToken = sys.env.getOrElse("SONAR_SERVER_TOKEN", "Not found token.")

sonarProperties ++= Map(
  "sonar.login"                      -> sonarToken,
  "sonar.projectKey"                 -> "mulesoft.amf-aml",
  "sonar.projectName"                -> "AMF-AML",
  "sonar.projectVersion"             -> "4.0.0",
  "sonar.sourceEncoding"             -> "UTF-8",
  "sonar.github.repository"          -> "mulesoft/amf-aml",
  "sonar.sources"                    -> "amf-aml/shared/src/main/scala",
  "sonar.tests"                      -> "amf-aml/shared/src/test/scala",
  "sonar.scala.coverage.reportPaths" -> "aml-aml/jvm/target/scala-2.12/scoverage-report/scoverage.xml"
)

lazy val workspaceDirectory: File =
  sys.props.get("sbt.mulesoft") match {
    case Some(x) => file(x)
    case _       => Path.userHome / "mulesoft"
  }

val amfCoreVersion = "4.1.196"

lazy val amfCoreJVMRef = ProjectRef(workspaceDirectory / "amf-core", "coreJVM")
lazy val amfCoreJSRef  = ProjectRef(workspaceDirectory / "amf-core", "coreJS")
lazy val amfCoreLibJVM = "com.github.amlorg" %% "amf-core" % amfCoreVersion
lazy val amfCoreLibJS  = "com.github.amlorg" %% "amf-core_sjs0.6" % amfCoreVersion

val commonSettings = Common.settings ++ Common.publish ++ Seq(
  organization := "com.github.amlorg",
  resolvers ++= List(ivyLocal, Common.releases, Common.snapshots, Resolver.mavenLocal),
//  resolvers += "jitpack" at "https://jitpack.io",
  credentials ++= Common.credentials(),
  libraryDependencies ++= Seq(
    "org.scalatest"    %%% "scalatest"         % "3.0.5" % Test,
    "org.mule.common"  %%% "scala-common-test" % "0.0.6" % Test,
  ),
  logBuffered in Test := false
)

/** **********************************************
  * AMF-Core
  * ********************************************* */
lazy val defaultProfilesGenerationTask = TaskKey[Unit](
  "defaultValidationProfilesGeneration",
  "Generates the validation dialect documents for the standard profiles")

/** **********************************************
  * AMF-Custom-Validation
  * ********************************************* */
lazy val customValidation = crossProject(JSPlatform, JVMPlatform)
  .settings(
    Seq(
      name := "amf-custom-validation"
    ))
  .in(file("./amf-custom-validation"))
  .dependsOn(aml % "compile->compile;test->test")
  .settings(commonSettings)
  .jvmSettings(
    artifactPath in (Compile, packageDoc) := baseDirectory.value / "target" / "artifact" / "amf-custom-validation-javadoc.jar"
  )
  .jsSettings(
    jsDependencies += ProvidedJS / "shacl.js",
    jsDependencies += ProvidedJS / "ajv.min.js",
    scalaJSModuleKind := ModuleKind.CommonJSModule,
    artifactPath in (Compile, fullOptJS) := baseDirectory.value / "target" / "artifact" / "amf-custom-validation-module.js"
  )
  .disablePlugins(SonarPlugin)

lazy val customValidationJVM =
  customValidation.jvm.in(file("./amf-custom-validation/jvm"))

lazy val customValidationJS = customValidation.js
  .in(file("./amf-custom-validation/js"))
  .disablePlugins(SonarPlugin)

/** **********************************************
  * AMF-AML
  * ********************************************* */
lazy val aml = crossProject(JSPlatform, JVMPlatform)
  .settings(
    Seq(
      name := "amf-aml"
    ))
  .in(file("./amf-aml"))
  .settings(commonSettings)
  .dependsOn(validation)
  .jvmSettings(
    libraryDependencies += "org.scala-js"           %% "scalajs-stubs"          % scalaJSVersion % "provided",
    artifactPath in (Compile, packageDoc) := baseDirectory.value / "target" / "artifact" / "amf-aml-javadoc.jar"
  )
  .jsSettings(
    scalaJSModuleKind := ModuleKind.CommonJSModule,
    artifactPath in (Compile, fullOptJS) := baseDirectory.value / "target" / "artifact" / "amf-aml-module.js",
    scalacOptions += "-P:scalajs:suppressExportDeprecations"
  )
  .disablePlugins(SonarPlugin)

lazy val amlJVM = aml.jvm.in(file("./amf-aml/jvm"))
lazy val amlJS = aml.js.in(file("./amf-aml/js"))

/** **********************************************
  * AMF-Validation
  * ********************************************* */
lazy val validation = crossProject(JSPlatform, JVMPlatform)
  .settings(
    Seq(
      name := "amf-validation"
    ))
  .in(file("./amf-validation"))
  .settings(commonSettings)
  .jvmSettings(
    libraryDependencies += "org.json4s"                 %% "json4s-native"          % "3.5.4",
    libraryDependencies += "org.apache.jena"            % "jena-shacl"              % "3.14.0",
    libraryDependencies += "commons-io" % "commons-io" % "2.6",
    libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.9",
    libraryDependencies += "org.apache.commons"         % "commons-compress"        % "1.19",
    libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind"        % "2.11.0",
    artifactPath in (Compile, packageDoc) := baseDirectory.value / "target" / "artifact" / "amf-validation-javadoc.jar"
  )
  .jsSettings(
    jsDependencies += ProvidedJS / "shacl.js",
    jsDependencies += ProvidedJS / "ajv.min.js",
    scalaJSModuleKind := ModuleKind.CommonJSModule,
    artifactPath in (Compile, fullOptJS) := baseDirectory.value / "target" / "artifact" / "amf-validation-module.js"
  )
  .disablePlugins(SonarPlugin)

lazy val validationJVM = validation.jvm.in(file("./amf-validation/jvm")).sourceDependency(amfCoreJVMRef, amfCoreLibJVM)
lazy val validationJS = validation.js
  .in(file("./amf-validation/js"))
  .sourceDependency(amfCoreJSRef, amfCoreLibJS)
  .disablePlugins(SonarPlugin)
