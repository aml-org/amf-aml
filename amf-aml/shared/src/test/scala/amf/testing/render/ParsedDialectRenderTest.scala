package amf.testing.render

import amf.client.errorhandling.DefaultErrorHandler
import amf.core.errorhandling.UnhandledErrorHandler
import amf.core.remote.VocabularyYamlHint
import amf.core.unsafe.PlatformSecrets
import amf.testing.common.cycling.FunSuiteCycleTests
import amf.testing.common.utils.DefaultAMLInitialization

class ParsedDialectRenderTest extends FunSuiteCycleTests with DefaultAMLInitialization with PlatformSecrets {

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
