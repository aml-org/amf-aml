package amf.dialects

import amf.core.client.common.validation.ProfileName
import amf.testing.common.utils.DialectInstanceValidation
import org.scalatest.Assertion

import scala.concurrent.{ExecutionContext, Future}

class DialectInstancesCustomValidationTest extends DialectInstanceValidation with ReportComparison {

  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  val basePath       = "file://amf-aml/shared/src/test/resources/vocabularies2/validation"
  val productionPath = "file://amf-aml/shared/src/test/resources/vocabularies2/production"

  def validate(dialect: String,
               instance: String,
               golden: Option[String] = None,
               path: String = basePath): Future[Assertion] = {
    validation(dialect, instance, path) flatMap {
      assertReport(_, golden.map(g => s"$path/$g"))
    }
  }

  def validateWithCustomProfile(dialect: String,
                                instance: String,
                                profile: ProfileName,
                                name: String,
                                golden: Option[String] = None,
                                path: String = basePath): Future[Assertion] = {
    validationWithCustomProfile(dialect, instance, profile, name, path) flatMap {
      assertReport(_, golden.map(g => s"$path/$g"))
    }
  }

  test("custom validation profile for dialect") {
    validateWithCustomProfile(
        "eng_demos_dialect1_modified.yaml",
        "eng_demos_instance1.yaml",
        ProfileName("eng_demos_profile.yaml"),
        "Custom Eng-Demos Validation",
        golden = Some("eng_demos_instance1_modified.report.json")
    )
  }

  test("custom validation profile for dialect default profile") {
    validateWithCustomProfile("eng_demos_dialect1.yaml",
                              "eng_demos_instance1.yaml",
                              ProfileName("eng_demos_profile.yaml"),
                              "Eng Demos 0.1")
  }

  test("custom validation profile for ABOUT dialect default profile") {
    validateWithCustomProfile(
        "ABOUT-dialect.yaml",
        "ABOUT.yaml",
        ProfileName("ABOUT-validation.yaml"),
        "ABOUT-validation",
        path = s"$productionPath/ABOUT",
        golden = Some("ABOUT.report.json")
    )
  }

  test("Custom validation profile for ABOUT dialect default profile negative case") {
    validateWithCustomProfile(
        "ABOUT-dialect.yaml",
        "ABOUT.custom.errors.yaml",
        ProfileName("ABOUT-validation.yaml"),
        "ABOUT-validation",
        path = s"$productionPath/ABOUT",
        golden = Some("ABOUT.custom.errors.report.json")
    )
  }

}
