package amf.testing.resolution

import amf.aml.client.scala.AMLConfiguration
import amf.core.internal.remote.Syntax

import scala.concurrent.ExecutionContext

class DialectsResolutionTest extends DialectResolutionCycleTests {
  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  val basePath = "amf-aml/shared/src/test/resources/vocabularies2/dialects/"

  test("resolve include test") {
    cycle(source = "example9.yaml", directory = "example9.resolved.yaml", syntax = Some(Syntax.Yaml))
  }

  test("resolve 13 test") {
    cycle("example13.yaml", "example13.resolved.yaml", syntax = Some(Syntax.Yaml))
  }

  multiGoldenTest("Resolve dialect with fragment", "dialect.resolved.%s") { config =>
    cycle(
        "dialect.yaml",
        config.golden,
        syntax = Some(Syntax.JsonLd),
        directory = s"$basePath/dialect-fragment/",
        AMLConfiguration.predefined().withRenderOptions(config.renderOptions)
    )
  }

  multiGoldenTest("Resolve dialect with library", "dialect.resolved.%s") { config =>
    cycle(
        "dialect.yaml",
        config.golden,
        syntax = Some(Syntax.JsonLd),
        directory = s"$basePath/dialect-library/",
        AMLConfiguration.predefined().withRenderOptions(config.renderOptions)
    )
  }
}
