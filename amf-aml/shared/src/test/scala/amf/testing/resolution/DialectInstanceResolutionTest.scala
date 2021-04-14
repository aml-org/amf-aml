package amf.testing.resolution

import amf.core.remote.{Amf, Aml, VocabularyYamlHint}
import amf.core.unsafe.PlatformSecrets

import scala.concurrent.ExecutionContext

class DialectInstanceResolutionTest extends DialectInstanceResolutionCycleTests with PlatformSecrets {

  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  val basePath = "amf-aml/shared/src/test/resources/vocabularies2/instances/"

  test("resolve fragment test") {
    withDialect("dialect8.yaml", "example8.yaml", "example8.resolved.yaml", VocabularyYamlHint, Aml)
  }

  test("resolve library test") {
    withDialect("dialect9.yaml", "example9.yaml", "example9.resolved.yaml", VocabularyYamlHint, Aml)
  }

  test("resolve patch 22a test") {
    withDialect("dialect22.yaml", "patch22.yaml", "patch22.resolved.yaml", VocabularyYamlHint, Aml)
  }

  test("resolve patch 22b test") {
    withDialect("dialect22.yaml", "patch22b.yaml", "patch22b.resolved.yaml", VocabularyYamlHint, Aml)
  }

  test("resolve patch 22c test") {
    withDialect("dialect22.yaml", "patch22c.yaml", "patch22c.resolved.yaml", VocabularyYamlHint, Aml)
  }

  test("resolve patch 22d test") {
    withDialect("dialect22.yaml", "patch22d.yaml", "patch22d.resolved.yaml", VocabularyYamlHint, Aml)
  }

  test("Resolve patch properties to AML") {
    withDialect("dialect.yaml",
                "patch.yaml",
                "patch.resolved.yaml",
                VocabularyYamlHint,
                target = Aml,
                directory = s"$basePath/patch-properties/")
  }

  multiGoldenTest("Resolve declares on self-encoded dialect instance", "instance.%s") { config =>
    withDialect(
        "dialect.yaml",
        "instance.yaml",
        config.golden,
        VocabularyYamlHint,
        renderOptions = Some(config.renderOptions),
        target = Amf,
        directory = s"$basePath/declares-in-self-encoded/"
    )
  }

  multiGoldenTest("Resolve patch properties to AMF Graph", "patch.resolved.%s") { config =>
    withDialect(
        "dialect.yaml",
        "patch.yaml",
        config.golden,
        VocabularyYamlHint,
        target = Amf,
        renderOptions = Some(config.renderOptions),
        directory = s"$basePath/patch-properties/"
    )
  }

}
