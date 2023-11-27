package amf.testing.resolution

import amf.aml.client.scala.AMLConfiguration
import amf.core.client.scala.errorhandling.UnhandledErrorHandler
import amf.core.internal.remote.Syntax

class DialectsResolutionTest extends DialectResolutionCycleTests {

  val basePath = "amf-aml/shared/src/test/resources/vocabularies2/dialects/"

  private def multiCycleTest(label: String, directory: String): Unit = {

    multiGoldenTest(s"$label resolution to JSON-LD", "dialect.resolved.%s") { config =>
      cycle(
          "dialect.yaml",
          config.golden,
          syntax = Some(Syntax.JsonLd),
          directory = directory,
          AMLConfiguration.predefined().withRenderOptions(config.renderOptions)
      )
    }

    test(s"$label resolution to YAML") {
      cycle("dialect.yaml", "dialect.resolved.yaml", directory, UnhandledErrorHandler)
    }

  }

  test("resolve include test") {
    cycle("example9.yaml", "example9.resolved.yaml", basePath, UnhandledErrorHandler)
  }

  test("resolve 13 test") {
    cycle("example13.yaml", "example13.resolved.yaml", basePath, UnhandledErrorHandler)
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

  multiCycleTest("AllOf (test combining resolution)", s"$basePath/allOf-complex/")

  multiCycleTest("AllOf nested (test combining resolution)", s"$basePath/allOf-nested/")

  multiCycleTest("AllOf nested with allOf (test combining resolution)", s"$basePath/allOf-nested-allOf/")

  multiCycleTest("Extended mapping 1", s"$basePath/extended-mapping-1/")

  multiCycleTest("Extended mapping 2", s"$basePath/extended-mapping-2/")

  multiCycleTest("Extended mapping 3", s"$basePath/extended-mapping-3/")
}
