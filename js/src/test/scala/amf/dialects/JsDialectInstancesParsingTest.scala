package amf.dialects

import amf.core.remote.{AmfJsonHint, Aml}

class JsDialectInstancesParsingTest extends DialectInstancesParsingTest {
  test("generate 32 test") {
    withDialect("dialect32.raml", "example32.json", "example32.js.raml", AmfJsonHint, Aml)
  }
}
