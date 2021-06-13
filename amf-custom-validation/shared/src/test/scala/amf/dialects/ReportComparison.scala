package amf.dialects

import amf.core.io.FileAssertionTest
import amf.core.client.scala.validation.AMFValidationReport
import amf.plugins.features.validation.emitters.ValidationReportJSONLDEmitter
import org.scalatest.{Assertion, AsyncFunSuite}

import scala.concurrent.Future

trait ReportComparison extends AsyncFunSuite with FileAssertionTest {
  def assertReport(report: AMFValidationReport, goldenOption: Option[String] = None): Future[Assertion] = {
    goldenOption match {
      case Some(golden) =>
        for {
          actual    <- writeTemporaryFile(golden)(ValidationReportJSONLDEmitter.emitJSON(report))
          assertion <- assertDifferences(actual, golden.stripPrefix("file://"))
        } yield {
          assertion
        }
      case None =>
        Future.successful {
          assert(report.conforms)
        }
    }
  }
}
