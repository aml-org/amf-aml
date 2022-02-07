package amf.testing.parsing

import amf.core.internal.remote.Syntax.Json
import amf.testing.common.cycling.FunSuiteCycleTests

class JsonVocabularyCycleTest extends FunSuiteCycleTests {
  override def basePath: String = "amf-aml/shared/src/test/resources/vocabularies2/vocabularies/json/"

  test("JSON Vocabulary without references") {
    cycle("vocabulary.json", syntax = Some(Json))
  }
}
