import org.scalajs.core.tools.linker.ModuleKind
import sbt.Keys.{libraryDependencies, resolvers}
import sbtcrossproject.CrossPlugin.autoImport.crossProject
import sbtsonar.SonarPlugin.autoImport.sonarProperties

val ivyLocal = Resolver.file("ivy", file(Path.userHome.absolutePath + "/.ivy2/local"))(Resolver.ivyStylePatterns)

name := "amf-aml"

ThisBuild / version := "6.0.4-RC.1"
ThisBuild / scalaVersion := "2.12.11"

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

val amfCoreVersion = "5.0.4-RC.0"

lazy val amfCoreJVMRef = ProjectRef(workspaceDirectory / "amf-core", "coreJVM")
lazy val amfCoreJSRef  = ProjectRef(workspaceDirectory / "amf-core", "coreJS")
lazy val amfCoreLibJVM = "com.github.amlorg" %% "amf-core" % amfCoreVersion
lazy val amfCoreLibJS  = "com.github.amlorg" %% "amf-core_sjs0.6" % amfCoreVersion

val commonSettings = Common.settings ++ Common.publish ++ Seq(
  organization := "com.github.amlorg",
  resolvers ++= List(ivyLocal, Common.releases, Common.snapshots, Resolver.mavenLocal, Resolver.mavenCentral),
  credentials ++= Common.credentials(),
  libraryDependencies ++= Seq(
    "org.mule.common"  %%% "scala-common-test" % "0.0.10" % Test,
    "org.slf4j" % "slf4j-nop" % "1.7.32" % Test
  ),
  Test / logBuffered := false
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
  .settings(
    Seq(
      name := "amf-aml"
    ))
  .in(file("./amf-aml"))
  .dependsOn(rdf % "test")
  .settings(commonSettings)
  .dependsOn(validation)
  .jvmSettings(
    libraryDependencies += "org.scala-js"           %% "scalajs-stubs"          % scalaJSVersion % "provided",
    Compile /  packageDoc / artifactPath := baseDirectory.value / "target" / "artifact" / "amf-aml-javadoc.jar"
  )
  .jsSettings(
    scalaJSModuleKind := ModuleKind.CommonJSModule,
    Compile /  fullOptJS / artifactPath := baseDirectory.value / "target" / "artifact" / "amf-aml-module.js",
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
    Compile /  packageDoc / artifactPath := baseDirectory.value / "target" / "artifact" / "amf-validation-javadoc.jar"
  )
  .jsSettings(
    scalaJSModuleKind := ModuleKind.CommonJSModule,
    Compile /  fullOptJS / artifactPath := baseDirectory.value / "target" / "artifact" / "amf-validation-module.js"
  )
  .disablePlugins(SonarPlugin)

lazy val validationJVM = validation.jvm.in(file("./amf-validation/jvm")).sourceDependency(amfCoreJVMRef, amfCoreLibJVM)
lazy val validationJS = validation.js
  .in(file("./amf-validation/js"))
  .sourceDependency(amfCoreJSRef, amfCoreLibJS)
  .disablePlugins(SonarPlugin)

/** **********************************************
  * AMF-RDF
  * ********************************************* */

lazy val rdf = crossProject(JSPlatform, JVMPlatform)
  .settings(
    Seq(
      name := "amf-rdf"
    ))
  .in(file("./amf-rdf"))
  .settings(commonSettings)
  .jvmSettings(
    libraryDependencies += "org.scala-js"                      %% "scalajs-stubs"         % scalaJSVersion % "provided",
    libraryDependencies += "org.json4s"                 %% "json4s-native"          % "3.5.4",
    libraryDependencies += "org.apache.jena" % "jena-arq" % "3.17.0",
    libraryDependencies += "org.apache.thrift"          % "libthrift"               % "0.14.1", // CVE-2020-13949
    excludeDependencies += "org.apache.tomcat.embed"    % "tomcat-embed-core",
    excludeDependencies += "com.fasterxml.jackson.core" % "jackson-databind", // transitive from jena-arq
    libraryDependencies += "commons-io"                 % "commons-io"              % "2.6",
    libraryDependencies += "org.apache.commons"         % "commons-lang3"           % "3.9",
    libraryDependencies += "org.apache.commons"         % "commons-compress"        % "1.21", // CVE-2021-35515 upto CVE-2021-35517
    Compile /  packageDoc / artifactPath := baseDirectory.value / "target" / "artifact" / "amf-rdf-javadoc.jar",
  )
  .jsSettings(
    jsDependencies += ProvidedJS / "shacl.js",
    scalaJSModuleKind := ModuleKind.CommonJSModule,
    Compile /  fullOptJS / artifactPath := baseDirectory.value / "target" / "artifact" / "amf-rdf-module.js",
    scalacOptions += "-P:scalajs:suppressExportDeprecations"
  )
  .disablePlugins(SonarPlugin)

lazy val rdfJVM =
  rdf.jvm.in(file("./amf-rdf/jvm")).sourceDependency(amfCoreJVMRef, amfCoreLibJVM)

lazy val rdfJS =
  rdf.js
    .in(file("./amf-rdf/js"))
    .sourceDependency(amfCoreJSRef, amfCoreLibJS)

ThisBuild / libraryDependencies ++= Seq(
  compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.7.1" cross CrossVersion.constant("2.12.11")),
  "com.github.ghik" % "silencer-lib" % "1.7.1" % Provided cross CrossVersion.constant("2.12.11")
)
