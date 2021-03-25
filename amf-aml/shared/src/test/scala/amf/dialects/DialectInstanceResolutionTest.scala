package amf.dialects
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

  test("test app configuration") {
    withDialect("app-config-dialect.yaml",
                "monitoring-patch.yaml",
                "monitoring-patch.resolved.yaml",
                VocabularyYamlHint,
                Aml)
  }

  test("test dialecv21 configuration") {
    withDialect("dialect21.yaml", "patch21b.yaml", "patch21b.json", VocabularyYamlHint, Aml)
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
