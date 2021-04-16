package amf.dialects

import amf.core.remote.{AmfJsonHint, Aml}
import amf.testing.parsing.DialectInstancesParsingTest

class JvmDialectInstancesParsingTest extends DialectInstancesParsingTest {
  multiSourceTest("generate 32 test", "example32.%s") { config =>
    cycleWithDialect("dialect32.yaml", config.source, "example32.jvm.yaml", AmfJsonHint, target = Aml)
  }
}
