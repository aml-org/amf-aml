package amf.dialects

import amf.core.internal.remote.{AmfJsonHint, Aml, Syntax}
import amf.testing.parsing.DialectInstancesParsingTest

class JsDialectInstancesParsingTest extends DialectInstancesParsingTest {
  multiSourceTest("generate 32 test", "example32.%s") { config =>
    cycleWithDialect("dialect32.yaml", config.source, "example32.js.yaml", syntax = Some(Syntax.Yaml))
  }
}
