package amf.testing.render

import amf.core.client.scala.errorhandling.DefaultErrorHandler
import amf.core.client.scala.errorhandling.UnhandledErrorHandler
import amf.core.internal.remote.VocabularyYamlHint
import amf.core.internal.unsafe.PlatformSecrets
import amf.testing.common.cycling.FunSuiteCycleTests

class ParsedDialectRenderTest extends FunSuiteCycleTests with PlatformSecrets {

  override def basePath: String = "amf-aml/shared/src/test/resources/vocabularies2/dialects/"

  test("Cycle dialect with annotation mappings") {
    cycle("dialect.yaml",
          "dialect.cycled.yaml",
          VocabularyYamlHint,
          s"${basePath}annotation-mappings/",
          UnhandledErrorHandler)
  }

  if (platform.name == "jvm") {
    // Due to issues with how numbers are emitted.
    test("Cycle dialect with annotation mappings with type discriminators") {
      cycle("dialect.yaml",
            "dialect.cycled.yaml",
            VocabularyYamlHint,
            s"${basePath}annotation-mappings-with-extra-facets/",
            DefaultErrorHandler())
    }
  }
}
