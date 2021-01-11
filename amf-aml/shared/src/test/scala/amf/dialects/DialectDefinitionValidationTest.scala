package amf.dialects
import org.scalatest.{AsyncFunSuite, Matchers}

import scala.concurrent.ExecutionContext

class DialectDefinitionValidationTest extends AsyncFunSuite with Matchers with DialectValidation {

  protected val path: String = "amf-aml/shared/src/test/resources/vocabularies2/instances/invalids"

  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  test("Test missing version") {
    validate("/missing-version/dialect.yaml", Some("/missing-version/report.json"))
  }

  test("Test missing dialect name") {
    validate("/missing-dialect-name/dialect.yaml", Some("/missing-dialect-name/report.json"))
  }

  test("Test invalid property term uri for description") {
    validate("/schema-uri/dialect.yaml", Some("/schema-uri/report.json"))
  }

  test("Test missing range in property mapping") {
    validate("/missing-range-in-mapping/dialect.yaml", Some("/missing-range-in-mapping/report.json"))
  }

  test("Test idTemplate variables overridable by $base directive") {
    validate("/base-overridable-idTemplate-variable/dialect.yaml", Some("/base-overridable-idTemplate-variable/report.json"))
  }

  test("Test mandatory property mapping without value") {
    validate("/mandatory-property-mapping-without-value/dialect.yaml", Some("mandatory-property-mapping-without-value/report.json"))
  }
}
