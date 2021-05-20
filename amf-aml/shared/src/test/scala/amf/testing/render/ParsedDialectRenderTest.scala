package amf.testing.render

import amf.client.parse.DefaultParserErrorHandler
import amf.core.parser.errorhandler.UnhandledParserErrorHandler
import amf.core.remote.VocabularyYamlHint
import amf.testing.common.cycling.FunSuiteCycleTests
import amf.testing.common.utils.DefaultAMLInitialization

class ParsedDialectRenderTest extends FunSuiteCycleTests with DefaultAMLInitialization {

  override def basePath: String = "amf-aml/shared/src/test/resources/vocabularies2/dialects/"

  test("Cycle dialect with annotation mappings") {
    cycle("dialect.yaml",
          "dialect.cycled.yaml",
          VocabularyYamlHint,
          s"${basePath}annotation-mappings/",
          UnhandledParserErrorHandler)
  }

  test("Cycle dialect with annotation mappings with type discriminators") {
    cycle("dialect.yaml",
          "dialect.cycled.yaml",
          VocabularyYamlHint,
          s"${basePath}annotation-mappings-with-extra-facets/",
          DefaultParserErrorHandler())
  }
}
