package amf.testing.resolution

import amf.aml.client.scala.AMLConfiguration
import amf.core.internal.remote.{Amf, Aml, VocabularyYamlHint}

import scala.concurrent.ExecutionContext

class DialectsResolutionTest extends DialectResolutionCycleTests {
  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  val basePath = "amf-aml/shared/src/test/resources/vocabularies2/dialects/"

  test("resolve include test") {
    cycle("example9.yaml", "example9.resolved.yaml", VocabularyYamlHint, Aml)
  }

  test("resolve 13 test") {
    cycle("example13.yaml", "example13.resolved.yaml", VocabularyYamlHint, Aml)
  }

  multiGoldenTest("Resolve dialect with fragment", "dialect.resolved.%s") { config =>
    cycle(
        "dialect.yaml",
        config.golden,
        VocabularyYamlHint,
        target = Amf,
        directory = s"$basePath/dialect-fragment/",
        AMLConfiguration.predefined().withRenderOptions(config.renderOptions)
    )
  }

  multiGoldenTest("Resolve dialect with library", "dialect.resolved.%s") { config =>
    cycle(
        "dialect.yaml",
        config.golden,
        VocabularyYamlHint,
        target = Amf,
        directory = s"$basePath/dialect-library/",
        AMLConfiguration.predefined().withRenderOptions(config.renderOptions)
    )
  }
}
