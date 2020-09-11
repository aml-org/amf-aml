package amf.dialects

import amf.core.remote.{AmfJsonHint, Aml}

class JvmDialectInstancesParsingTest extends DialectInstancesParsingTest {
  multiSourceTest("generate 32 test", "example32.%s") { config =>
    withDialect("dialect32.yaml", config.source, "example32.jvm.yaml", AmfJsonHint, target = Aml)
  }
}
