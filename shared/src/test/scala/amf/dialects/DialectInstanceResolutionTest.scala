package amf.dialects
import amf.core.remote.{Aml, VocabularyYamlHint}
import amf.core.unsafe.PlatformSecrets

import scala.concurrent.ExecutionContext

class DialectInstanceResolutionTest extends DialectInstanceResolutionCycleTests with PlatformSecrets {

  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  val basePath = "shared/src/test/resources/vocabularies2/instances/"

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

}
