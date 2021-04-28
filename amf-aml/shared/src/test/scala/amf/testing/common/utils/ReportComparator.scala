package amf.testing.common.utils

import amf.core.io.FileAssertionTest
import amf.core.unsafe.PlatformSecrets
import amf.core.validation.AMFValidationReport
import amf.plugins.features.validation.emitters.ValidationReportJSONLDEmitter
import org.scalatest.{Assertion, Matchers}

import scala.concurrent.{ExecutionContext, Future}

trait ReportComparator extends FileAssertionTest with Matchers {

  implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  def assertReport(report: AMFValidationReport,
                   goldenOption: Option[String] = None,
                   jsonldReport: Boolean = true): Future[Assertion] = {
    goldenOption
      .map(processGoldenPath)
      .map { golden =>
        for {
          actual    <- writeTemporaryFile(golden)(emitReport(report, jsonldReport))
          assertion <- assertDifferences(actual, golden.stripPrefix("file://"))
        } yield {
          assertion
        }
      }
      .getOrElse { Future.successful { report.conforms shouldBe true } }
  }

  protected def processGoldenPath(path: String): String

  private def emitReport(report: AMFValidationReport, emitJsonLd: Boolean): String = {
    if (emitJsonLd) ValidationReportJSONLDEmitter.emitJSON(report)
    else report.toString()
  }
}

object UniquePlatformReportComparator extends ReportComparator {
  override protected def processGoldenPath(path: String): String = path
}

object MultiPlatformReportComparator extends ReportComparator with PlatformSecrets {
  override protected def processGoldenPath(path: String): String = path + s".${platform.name}"
}
