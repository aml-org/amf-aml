package amf.testing.validation

import amf.testing.common.utils.{
  AMLParsingHelper,
  DialectInstanceValidation,
  ReportComparator,
  UniquePlatformReportComparator
}
import org.scalatest.Assertion

import scala.concurrent.Future

class JapaneseDialectInstancesValidationTest extends DialectInstanceValidation with AMLParsingHelper {

  private val reportComparator: ReportComparator = UniquePlatformReportComparator

  val basePath = "file://amf-aml/shared/src/test/resources/vocabularies2/japanese/instances/"

  def validate(
      dialect: String,
      instance: String,
      golden: Option[String] = None,
      path: String = basePath
  ): Future[Assertion] = {
    validation(dialect, instance, path) flatMap {
      reportComparator.assertReport(_, golden.map(g => s"$path/$g"))
    }
  }

  test("validate mixed instance 1") {
    validate("mixed-dialect-1.yaml", "mixed-example-1.yaml")
  }

  test("validate mixed instance 2") {
    validate("mixed-dialect-2.yaml", "mixed-example-2.yaml")
  }

  test("valid pattern") {
    validate("pattern-dialect.yaml", "pattern-valid.yaml")
  }

  test("invalid pattern") {
    validate("pattern-dialect.yaml", "pattern-invalid.yaml", golden = Some("pattern-invalid.report.json"))
  }

  ignore("valid in") {
    validate("in-dialect.yaml", "in-valid.yaml")
  }

  ignore("invalid in") {
    validate("in-dialect.yaml", "in-invalid.yaml")
  }
}
