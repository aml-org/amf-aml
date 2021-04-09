package amf.dialects

import amf.core.errorhandling.UnhandledErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.remote.{Amf, Aml, Vendor, VocabularyYamlHint}
import amf.core.io.FunSuiteCycleTests
import amf.core.resolution.pipelines.ResolutionPipeline
import amf.core.services.RuntimeResolver
import amf.plugins.document.vocabularies.AMLPlugin

import scala.concurrent.ExecutionContext

abstract class DialectResolutionCycleTests extends FunSuiteCycleTests {
  override def transform(unit: BaseUnit, config: CycleConfig): BaseUnit =
    RuntimeResolver.resolve(Vendor.AML.name, unit, ResolutionPipeline.DEFAULT_PIPELINE, UnhandledErrorHandler)
}

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
    cycle("dialect.yaml",
          config.golden,
          VocabularyYamlHint,
          target = Amf,
          renderOptions = Some(config.renderOptions),
          directory = s"$basePath/dialect-fragment/")
  }

  multiGoldenTest("Resolve dialect with library", "dialect.resolved.%s") { config =>
    cycle("dialect.yaml",
          config.golden,
          VocabularyYamlHint,
          target = Amf,
          renderOptions = Some(config.renderOptions),
          directory = s"$basePath/dialect-library/")
  }
}
