package amf.testing.common.cycling

import amf.testing.common.jsonld.MultiJsonLDAsyncFunSuite

import scala.concurrent.ExecutionContext

abstract class FunSuiteCycleTests extends MultiJsonLDAsyncFunSuite with BuildCycleTests {
  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global
}
