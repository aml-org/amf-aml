package amf.testing.validation

import amf.testing.common.utils.DialectValidation
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers

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
    validate("/base-overridable-idTemplate-variable/dialect.yaml",
             Some("/base-overridable-idTemplate-variable/report.json"))
  }

  test("Test mandatory property mapping without value") {
    validate("/mandatory-property-mapping-without-value/dialect.yaml",
             Some("mandatory-property-mapping-without-value/report.json"))
  }

  test("Test un-avoidable ambiguity in node") {
    validate("/unavoidable-ambiguity-node/dialect.yaml", Some("unavoidable-ambiguity-node/report.json"))
  }

  test("Test un-avoidable ambiguity in property") {
    validate("/unavoidable-ambiguity-property/dialect.yaml", Some("unavoidable-ambiguity-property/report.json"))
  }

  test("Test eventual ambiguity in node") {
    validate("/eventual-ambiguity-node/dialect.yaml", Some("eventual-ambiguity-node/report.json"))
  }

  test("Test eventual ambiguity in property") {
    validate("/eventual-ambiguity-property/dialect.yaml", Some("eventual-ambiguity-property/report.json"))
  }

  test("Test nested un-avoidable ambiguity in node") {
    validate("/nested-unavoidable-ambiguity-node/dialect.yaml", Some("nested-unavoidable-ambiguity-node/report.json"))
  }

  test("Test nested un-avoidable ambiguity in property") {
    validate("/nested-unavoidable-ambiguity-property/dialect.yaml",
             Some("nested-unavoidable-ambiguity-property/report.json"))
  }

  test("Test nested eventual ambiguity in node") {
    validate("/nested-eventual-ambiguity-node/dialect.yaml", Some("nested-eventual-ambiguity-node/report.json"))
  }

  test("Test nested eventual ambiguity in property") {
    validate("/nested-eventual-ambiguity-property/dialect.yaml",
             Some("nested-eventual-ambiguity-property/report.json"))
  }

  test("Test node mapping with reserved names") {
    validate("/dialect-with-reserved-names/dialect.yaml", Some("dialect-with-reserved-names/report.json"))
  }

  test("idTemplate uri template references property not present in mapping") {
    validate("/id-template-missing-variable/dialect.yaml", Some("id-template-missing-variable/report.json"))
  }

  test("Scalar property mapping") {
    validate("/scalar-property-mapping/dialect.yaml", Some("scalar-property-mapping/report.json"))
  }

  test("Lexical range in unknown object range terms") {
    validate("/invalid-union-objectRange/dialect.yaml",
             Some("/invalid-union-objectRange/dialect.report"),
             jsonldReport = false)
  }

  test("Lexical range of unknown property used as map key") {
    validate("/unknown-map-key-property/dialect.yaml",
             Some("/unknown-map-key-property/dialect.report"),
             jsonldReport = false)
  }

  test("Empty type discriminator value") {
    validate(
        "/empty-discriminator-value/dialect-with-empty-type-discriminator-value.yaml",
        Some("/empty-discriminator-value/dialect-with-empty-type-discriminator-value.report"),
        jsonldReport = false
    )
  }

  test("Validate annotation mappings & semantic extensions") {
    validate(
        "/annotation-mappings-invalid/dialect.yaml",
        Some("/annotation-mappings-invalid/report.report"),
        jsonldReport = false
    )
  }

  test("Property mapping with null range") {
    validate(
        "/null-range/dialect.yaml",
        Some("/null-range/report.json")
    )
  }

  test("JSON simple should be valid") {
    validate("../../../dialects/json/simple/dialect.json", None)
  }

  test("JSON with-library should be valid") {
    validate("../../../dialects/json/with-library/dialect.json", None)
  }

  test("JSON with-vocabulary should be valid") {
    validate("../../../dialects/json/with-vocabulary/dialect.json", None)
  }

  test("Empty extends property on node mapping reports error") {
    validate("../../../dialects/empty-extends-on-node-mapping.yaml",
             Some("../../dialects/empty-extends-on-node-mapping.validation.jsonld"))
  }

  test("Valid ID directive on dialect") {
    validate("/id-directive-valid/dialect.yaml", Some("/id-directive-valid/report.txt"), jsonldReport = false)
  }

  test("Invalid ID directive on dialect") {
    validate("/id-directive-invalid/dialect.yaml", Some("/id-directive-invalid/report.txt"), jsonldReport = false)
  }

  test("Dialect with property mapping with default key") {
    validate("../../../dialects/default-facet/dialect.yaml", None)
  }

  test("Dialect with if/then/else facets") {
    validate("../../../dialects/conditional/dialect.yaml", None)
  }

  test("Dialect with if/then/else facet that point to nowhere") {
    validate("../../../dialects/conditional/dialect-with-invalid-references.yaml",
             Some("../../dialects/conditional/dialect-with-invalid-references.report.json"))
  }

  test("Dialect with NodeMapping with additional properties") {
    validate("../../../dialects/additional-properties/dialect.yaml", None)
  }

  test("Numeric enums in dialect get validated against their property mapping") {
    validate("../../../dialects/enum-dialect-validation/dialect.yaml",
             Some("../../dialects/enum-dialect-validation/report.json"))
  }
}
