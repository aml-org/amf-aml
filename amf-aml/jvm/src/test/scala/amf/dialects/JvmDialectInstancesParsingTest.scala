package amf.dialects

import amf.core.internal.remote.{AmfJsonHint, Aml, Mimes, Syntax}
import amf.testing.parsing.DialectInstancesParsingTest

class JvmDialectInstancesParsingTest extends DialectInstancesParsingTest {
  multiSourceTest("generate 32 test", "example32.%s") { config =>
    cycleWithDialect("dialect32.yaml", config.source, "example32.jvm.yaml", mediaType = Some(Mimes.`application/yaml`))
  }
}
