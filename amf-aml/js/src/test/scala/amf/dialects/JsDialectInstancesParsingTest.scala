package amf.dialects

import amf.core.remote.{AmfJsonHint, Aml}
import amf.testing.parsing.DialectInstancesParsingTest

class JsDialectInstancesParsingTest extends DialectInstancesParsingTest {
  multiSourceTest("generate 32 test", "example32.%s") { config =>
    cycleWithDialect("dialect32.yaml", config.source, "example32.js.yaml", AmfJsonHint, target = Aml)
  }
}
