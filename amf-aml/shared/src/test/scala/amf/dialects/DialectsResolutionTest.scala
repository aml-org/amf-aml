package amf.dialects

import amf.core.remote.{Amf, Aml, VocabularyYamlHint}

import scala.concurrent.ExecutionContext

class DialectsResolutionTest extends DialectInstanceResolutionCycleTests {
  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  override val basePath = "amf-aml/shared/src/test/resources/vocabularies2/dialects/"

  test("resolve include test") {
    init().flatMap(_ => cycle("example9.yaml", "example9.resolved.yaml", VocabularyYamlHint, target = Aml))
  }

  test("resolve library test") {
    init().flatMap(_ => cycle("example7.yaml", "example7.resolved.yaml", VocabularyYamlHint, target = Aml))
  }

  test("resolve 13 test") {
    init().flatMap(_ => cycle("example13.yaml", "example13.resolved.yaml", VocabularyYamlHint, target = Aml))
  }

  test("resolve 21 test") {
    init().flatMap(_ => cycle("example21.yaml", "example21.resolved.yaml", VocabularyYamlHint, target = Aml))
  }

  multiGoldenTest("resolve 21 test JSON-LD", "example21.resolved.%s") { config =>
    init().flatMap(
        _ =>
          cycle("example21.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions)))
  }

  multiGoldenTest("Resolve dialect with fragment", "dialect.resolved.%s") { config =>
    init().flatMap(
        _ =>
          cycle("dialect.yaml",
                config.golden,
                VocabularyYamlHint,
                target = Amf,
                renderOptions = Some(config.renderOptions),
                directory = s"$basePath/dialect-fragment"))
  }

  multiGoldenTest("Resolve dialect with library", "dialect.resolved.%s") { config =>
    init().flatMap(
      _ =>
        cycle("dialect.yaml",
          config.golden,
          VocabularyYamlHint,
          target = Amf,
          renderOptions = Some(config.renderOptions),
          directory = s"$basePath/dialect-library"))
  }
}
