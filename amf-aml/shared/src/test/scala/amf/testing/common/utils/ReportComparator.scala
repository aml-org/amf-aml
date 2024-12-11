package amf.testing.common.utils

import amf.core.client.scala.validation.AMFValidationReport
import amf.core.internal.unsafe.PlatformSecrets
import amf.core.io.FileAssertionTest
import amf.validation.internal.emitters.ValidationReportJSONLDEmitter
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers

import scala.concurrent.Future

trait ReportComparator extends FileAssertionTest with Matchers {

  def assertReport(
      report: AMFValidationReport,
      goldenOption: Option[String] = None,
      jsonldReport: Boolean = true
  ): Future[Assertion] = {
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
      .getOrElse {
        Future.successful {
          if (!report.conforms) Console.err.println(report.toString())
          report.conforms shouldBe true
        }
      }
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
