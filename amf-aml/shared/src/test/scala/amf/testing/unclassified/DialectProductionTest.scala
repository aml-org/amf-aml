package amf.testing.unclassified

import amf.aml.client.scala.AMLConfiguration
import amf.core.client.scala.config.RenderOptions
import amf.core.client.scala.errorhandling.UnhandledErrorHandler
import amf.core.internal.remote.Syntax.Yaml
import amf.core.internal.remote._
import amf.testing.common.cycling.FunSuiteCycleTests
import amf.testing.common.utils.DialectInstanceTester

import scala.concurrent.ExecutionContext

class DialectProductionTest extends FunSuiteCycleTests with DialectInstanceTester {

  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  override def defaultRenderOptions: RenderOptions = RenderOptions().withSourceMaps.withPrettyPrint

  val basePath = "amf-aml/shared/src/test/resources/vocabularies2/production/"

  test("Can parse and generated ABOUT dialect") {
    cycle("ABOUT-dialect.yaml", "ABOUT-dialect.yaml.yaml", syntax = Some(Syntax.Yaml), basePath + "ABOUT/")
  }

  ignore("Can parse the canonical webapi dialect") {
    cycle("canonical_webapi.yaml", "canonical_webapi.json", syntax = Some(Syntax.JsonLd), "vocabularies/dialects/")
  }

  multiGoldenTest("Can parse ABOUT dialect", "ABOUT-dialect.%s") { config =>
    cycle(
        "ABOUT-dialect.yaml",
        config.golden,
        syntax = Some(Syntax.JsonLd),
        directory = basePath + "ABOUT/",
        AMLConfiguration.predefined().withRenderOptions(config.renderOptions)
    )
  }

  // TODO migrate to multiGoldenTest
  test("Can parse validation dialect") {
    cycle(
        "validation_dialect.yaml",
        "validation_dialect.json",
        amlConfig = AMLConfiguration
          .predefined()
          .withErrorHandlerProvider(() => UnhandledErrorHandler)
          .withRenderOptions(RenderOptions().withPrettyPrint.withSourceMaps.withoutFlattenedJsonLd),
        syntax = Some(Syntax.JsonLd)
    )
  }

  // TODO migrate to multiGoldenTest
  test("Can parse and generate the Instagram dialect") {
    cycle(
        "dialect.yaml",
        "dialect.json",
        syntax = Some(Syntax.JsonLd),
        basePath + "Instagram/",
        amlConfig = AMLConfiguration
          .predefined()
          .withErrorHandlerProvider(() => UnhandledErrorHandler)
          .withRenderOptions(RenderOptions().withPrettyPrint.withSourceMaps.withoutFlattenedJsonLd)
    )
  }

  // TODO migrate to multiGoldenTest
  test("Can parse and generate the activity dialect") {
    cycle(
        "activity.yaml",
        "activity.json",
        syntax = Some(Syntax.JsonLd),
        basePath + "streams/",
        amlConfig = AMLConfiguration
          .predefined()
          .withErrorHandlerProvider(() => UnhandledErrorHandler)
          .withRenderOptions(RenderOptions().withPrettyPrint.withSourceMaps.withoutFlattenedJsonLd)
    )
  }

  test("Can parse validation dialect instance") {
    cycleWithDialect("validation_dialect.yaml",
                     "validation_instance1.yaml",
                     "validation_instance1.yaml.yaml",
                     Some(Yaml))
  }

  multiGoldenTest("Can parse validation dialect cfg1 instance", "example1_instance.%s") { config =>
    cycleWithDialect(
        "example1.yaml",
        "example1_instance.yaml",
        config.golden,
        directory = s"${basePath}cfg/",
        renderOptions = Some(config.renderOptions),
        syntax = Some(Syntax.JsonLd)
    )
  }

  multiGoldenTest("Can parse validation dialect cfg2 instance", "example2_instance.%s") { config =>
    cycleWithDialect(
        "example2.yaml",
        "example2_instance.yaml",
        config.golden,
        directory = basePath + "cfg/",
        renderOptions = Some(config.renderOptions),
        syntax = Some(Syntax.JsonLd)
    )
  }

  multiGoldenTest("Can parse validation dialect cfg3 instance", "example3_instance.%s") { config =>
    cycleWithDialect(
        "example3.yaml",
        "example3_instance.yaml",
        config.golden,
        directory = basePath + "cfg/",
        renderOptions = Some(config.renderOptions),
        syntax = Some(Syntax.JsonLd)
    )
  }

  test("Can parse and generate ABOUT dialect instance") {
    cycleWithDialect("ABOUT-dialect.yaml", "ABOUT.yaml", "ABOUT.yaml.yaml", Some(Yaml), basePath + "ABOUT/")
  }

  test("Can parse and generate ABOUT-github dialect instance") {
    cycleWithDialect("ABOUT-GitHub-dialect.yaml",
                     "example.yaml",
                     "example.yaml.yaml",
                     Some(Yaml),
                     basePath + "ABOUT/github/")
  }

  multiGoldenTest("Can parse ABOUT-hosted dialectinstance", "ABOUT_hosted.%s") { config =>
    cycleWithDialect(
        "ABOUT-hosted-vcs-dialect.yaml",
        "ABOUT_hosted.yaml",
        config.golden,
        directory = s"${basePath}ABOUT/",
        renderOptions = Some(config.renderOptions),
        syntax = Some(Syntax.JsonLd)
    )
  }

  // TODO migrate to multiGoldenTest
  test("Can parse and generate Instance dialect instance 1") {
    cycleWithDialect("dialect.yaml",
                     "instance1.yaml",
                     "instance1.json",
                     syntax = Some(Syntax.JsonLd),
                     basePath + "Instagram/")
  }

  test("Can parse and generate Instance dialect instance 2") {
    cycleWithDialect("dialect.yaml",
                     "instance2.yaml",
                     "instance2.json",
                     syntax = Some(Syntax.JsonLd),
                     basePath + "Instagram/")
  }

  test("Can parse activity instances") {
    cycleWithDialect("activity.yaml",
                     "stream1.yaml",
                     "stream1.json",
                     syntax = Some(Syntax.JsonLd),
                     basePath + "streams/")
  }

  test("Can parse activity deployments demo") {
    cycleWithDialect("dialect.yaml",
                     "deployment.yaml",
                     "deployment.json",
                     syntax = Some(Syntax.JsonLd),
                     basePath + "deployments_demo/")
  }
}
