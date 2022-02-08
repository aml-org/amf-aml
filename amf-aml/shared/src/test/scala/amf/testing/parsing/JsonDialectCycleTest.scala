package amf.testing.parsing

import amf.core.internal.remote.Syntax.Json
import amf.testing.common.cycling.FunSuiteCycleTests

class JsonDialectCycleTest extends FunSuiteCycleTests {
  override def basePath: String = "amf-aml/shared/src/test/resources/vocabularies2/dialects/json/"

  test("JSON Dialect without references") {
    cycle("simple/dialect.json", syntax = Some(Json))
  }

  // TODO: Fix lexical information in "uses"
  test("JSON Dialect with library reference") {
    cycle("with-library/dialect.json", golden = "with-library/dialect.cycled.json", syntax = Some(Json))
  }

  // TODO: Fix lexical information in "uses"
  test("JSON Dialect with vocabulary reference") {
    cycle("with-vocabulary/dialect.json", golden = "with-vocabulary/dialect.cycled.json", syntax = Some(Json))
  }
}
