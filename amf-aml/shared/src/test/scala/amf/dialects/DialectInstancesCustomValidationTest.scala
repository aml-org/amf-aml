package amf.dialects

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
}
