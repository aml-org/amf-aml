package amf.parser

import amf.dialects.{DialectValidation, MultiPlatformReportComparator, ReportComparator}

import scala.concurrent.ExecutionContext

class AmlSyamlConversionExceptionTest extends DialectValidation {

  override implicit def executionContext: ExecutionContext = ExecutionContext.Implicits.global
  override protected val path: String = "amf-aml/shared/src/test/resources/vocabularies2/validations/conversion-exceptions/"
  override protected val reportComparator: ReportComparator = MultiPlatformReportComparator

  test("Invalid uses entry key is not string") {
    validate("invalid-uses-entry-key-is-not-string.yaml", Some("reports/invalid-uses-entry-key-is-not-string.report"), false)
  }

  test("Invalid uses entry value is not string") {
    validate("invalid-uses-entry-value-is-not-string.yaml", Some("reports/invalid-uses-entry-value-is-not-string.report"), false)
  }
}
