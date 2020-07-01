package amf.dialects
import amf.core.remote.{Aml, VocabularyYamlHint}
import amf.core.unsafe.PlatformSecrets

import scala.concurrent.ExecutionContext

class DialectInstanceResolutionTest extends DialectInstanceResolutionCycleTests with PlatformSecrets {

  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  val basePath = "shared/src/test/resources/vocabularies2/instances/"

  test("resolve fragment test") {
    withDialect("dialect8.raml", "example8.raml", "example8.resolved.raml", VocabularyYamlHint, Aml)
  }

  test("resolve library test") {
    withDialect("dialect9.raml", "example9.raml", "example9.resolved.raml", VocabularyYamlHint, Aml)
  }

  test("resolve patch 22a test") {
    withDialect("dialect22.raml", "patch22.raml", "patch22.resolved.raml", VocabularyYamlHint, Aml)
  }

  test("resolve patch 22b test") {
    withDialect("dialect22.raml", "patch22b.raml", "patch22b.resolved.raml", VocabularyYamlHint, Aml)
  }

  test("resolve patch 22c test") {
    withDialect("dialect22.raml", "patch22c.raml", "patch22c.resolved.raml", VocabularyYamlHint, Aml)
  }

  test("resolve patch 22d test") {
    withDialect("dialect22.raml", "patch22d.raml", "patch22d.resolved.raml", VocabularyYamlHint, Aml)
  }

}
