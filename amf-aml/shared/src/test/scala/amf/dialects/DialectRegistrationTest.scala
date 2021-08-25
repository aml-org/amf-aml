package amf.dialects

import amf.plugins.document.vocabularies.AMLPlugin
import org.scalatest.AsyncFunSuite

import scala.concurrent.ExecutionContext

class DialectRegistrationTest extends AsyncFunSuite with DefaultAmfInitialization {

  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  val basePath = "amf-aml/shared/src/test/resources/vocabularies2/dialects/simple"

  test("Test register dialect without any error") {
    AMLPlugin().registry
      .registerDialect(s"file://$basePath/valid-simple.yaml")
      .map(d => assert(d.name().value() == "Test"))
  }

  test("Test register dialect with a warning") {
    AMLPlugin().registry
      .registerDialect(s"file://$basePath/warning-simple.yaml")
      .map(d => assert(d.name().value() == "Test"))
  }

  test("Test register dialect with a violation") {
    recoverToExceptionIf[Exception] {
      AMLPlugin().registry.registerDialect(s"file://$basePath/violation-simple.yaml")
    } map { e =>
      assert(e.getMessage.contains("'version' mandatory in a dialect node"))
    }
  }

}
