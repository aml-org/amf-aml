package amf.testing.unclassified

import amf.aml.client.scala.AMLConfiguration
import amf.core.client.scala.config.RenderOptions
import amf.core.client.scala.errorhandling.UnhandledErrorHandler
import amf.core.internal.remote._
import amf.testing.common.cycling.FunSuiteCycleTests
import amf.testing.common.utils.DialectInstanceTester

import scala.concurrent.ExecutionContext

class DialectProductionTest extends FunSuiteCycleTests with DialectInstanceTester {

  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  override def defaultRenderOptions: RenderOptions = RenderOptions().withSourceMaps.withPrettyPrint

  val basePath = "amf-aml/shared/src/test/resources/vocabularies2/production/"

  test("Can parse and generated ABOUT dialect") {
    cycle("ABOUT-dialect.yaml", "ABOUT-dialect.yaml.yaml", VocabularyYamlHint, Aml, basePath + "ABOUT/")
  }

  ignore("Can parse the canonical webapi dialect") {
    cycle("canonical_webapi.yaml", "canonical_webapi.json", VocabularyYamlHint, target = Amf, "vocabularies/dialects/")
  }

  multiGoldenTest("Can parse ABOUT dialect", "ABOUT-dialect.%s") { config =>
    cycle(
        "ABOUT-dialect.yaml",
        config.golden,
        VocabularyYamlHint,
        target = Amf,
        directory = basePath + "ABOUT/",
        AMLConfiguration.predefined().withRenderOptions(config.renderOptions)
    )
  }

  // TODO migrate to multiGoldenTest
  test("Can parse validation dialect") {
    cycle(
        "validation_dialect.yaml",
        "validation_dialect.json",
        VocabularyYamlHint,
        target = Amf,
        amlConfig = AMLConfiguration
          .predefined()
          .withErrorHandlerProvider(() => UnhandledErrorHandler)
          .withRenderOptions(RenderOptions().withPrettyPrint.withSourceMaps)
    )
  }

  // TODO migrate to multiGoldenTest
  test("Can parse and generate the Instagram dialect") {
    cycle(
        "dialect.yaml",
        "dialect.json",
        VocabularyYamlHint,
        target = Amf,
        basePath + "Instagram/",
        amlConfig = AMLConfiguration
          .predefined()
          .withErrorHandlerProvider(() => UnhandledErrorHandler)
          .withRenderOptions(RenderOptions().withPrettyPrint.withSourceMaps)
    )
  }

  // TODO migrate to multiGoldenTest
  test("Can parse and generate the activity dialect") {
    cycle(
        "activity.yaml",
        "activity.json",
        VocabularyYamlHint,
        target = Amf,
        basePath + "streams/",
        amlConfig = AMLConfiguration
          .predefined()
          .withErrorHandlerProvider(() => UnhandledErrorHandler)
          .withRenderOptions(RenderOptions().withPrettyPrint.withSourceMaps)
    )
  }

  test("Can parse validation dialect instance") {
    cycleWithDialect("validation_dialect.yaml",
                     "validation_instance1.yaml",
                     "validation_instance1.yaml.yaml",
                     VocabularyYamlHint,
                     Aml)
  }

  multiGoldenTest("Can parse validation dialect cfg1 instance", "example1_instance.%s") { config =>
    cycleWithDialect(
        "example1.yaml",
        "example1_instance.yaml",
        config.golden,
        VocabularyYamlHint,
        target = Amf,
        directory = s"${basePath}cfg/",
        renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("Can parse validation dialect cfg2 instance", "example2_instance.%s") { config =>
    cycleWithDialect(
        "example2.yaml",
        "example2_instance.yaml",
        config.golden,
        VocabularyYamlHint,
        target = Amf,
        directory = basePath + "cfg/",
        renderOptions = Some(config.renderOptions)
    )
  }

  multiGoldenTest("Can parse validation dialect cfg3 instance", "example3_instance.%s") { config =>
    cycleWithDialect(
        "example3.yaml",
        "example3_instance.yaml",
        config.golden,
        VocabularyYamlHint,
        target = Amf,
        directory = basePath + "cfg/",
        renderOptions = Some(config.renderOptions)
    )
  }

  test("Can parse and generate ABOUT dialect instance") {
    cycleWithDialect("ABOUT-dialect.yaml",
                     "ABOUT.yaml",
                     "ABOUT.yaml.yaml",
                     VocabularyYamlHint,
                     Aml,
                     basePath + "ABOUT/")
  }

  test("Can parse and generate ABOUT-github dialect instance") {
    cycleWithDialect("ABOUT-GitHub-dialect.yaml",
                     "example.yaml",
                     "example.yaml.yaml",
                     VocabularyYamlHint,
                     Aml,
                     basePath + "ABOUT/github/")
  }

  multiGoldenTest("Can parse ABOUT-hosted dialectinstance", "ABOUT_hosted.%s") { config =>
    cycleWithDialect(
        "ABOUT-hosted-vcs-dialect.yaml",
        "ABOUT_hosted.yaml",
        config.golden,
        VocabularyYamlHint,
        target = Amf,
        directory = s"${basePath}ABOUT/",
        renderOptions = Some(config.renderOptions)
    )
  }

  // TODO migrate to multiGoldenTest
  test("Can parse and generate Instance dialect instance 1") {
    cycleWithDialect("dialect.yaml",
                     "instance1.yaml",
                     "instance1.json",
                     VocabularyYamlHint,
                     target = Amf,
                     basePath + "Instagram/")
  }

  test("Can parse and generate Instance dialect instance 2") {
    cycleWithDialect("dialect.yaml",
                     "instance2.yaml",
                     "instance2.json",
                     VocabularyYamlHint,
                     target = Amf,
                     basePath + "Instagram/")
  }

  test("Can parse activity instances") {
    cycleWithDialect("activity.yaml",
                     "stream1.yaml",
                     "stream1.json",
                     VocabularyYamlHint,
                     target = Amf,
                     basePath + "streams/")
  }

  test("Can parse activity deployments demo") {
    cycleWithDialect("dialect.yaml",
                     "deployment.yaml",
                     "deployment.json",
                     VocabularyYamlHint,
                     target = Amf,
                     basePath + "deployments_demo/")
  }
}
