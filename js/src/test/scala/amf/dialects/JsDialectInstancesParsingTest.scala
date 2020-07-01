package amf.dialects

import amf.core.remote.{AmfJsonHint, Aml}

class JsDialectInstancesParsingTest extends DialectInstancesParsingTest {
  multiSourceTest("generate 32 test", "example32.%s") { config =>
    withDialect("dialect32.raml", config.source, "example32.js.raml", AmfJsonHint, target = Aml)
  }
}
