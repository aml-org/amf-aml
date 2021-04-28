package amf.testing.common.cycling

import amf.testing.common.jsonld.MultiJsonLDAsyncFunSuite

import scala.concurrent.ExecutionContext

abstract class FunSuiteRdfCycleTests extends MultiJsonLDAsyncFunSuite with BuildCycleRdfTests {
  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global
}
