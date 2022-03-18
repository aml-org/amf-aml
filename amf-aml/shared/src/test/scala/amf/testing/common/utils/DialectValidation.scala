package amf.testing.common.utils

import amf.aml.client.scala.AMLConfiguration
import amf.core.client.scala.validation.AMFValidationReport
import amf.core.internal.unsafe.PlatformSecrets
import org.scalatest.Assertion
import org.scalatest.funsuite.AsyncFunSuite

import scala.concurrent.Future

trait DialectValidation extends AsyncFunSuite with PlatformSecrets {

  protected val path: String
  protected val reportComparator: ReportComparator = UniquePlatformReportComparator

  protected def validate(dialectPath: String,
                         goldenReport: Option[String],
                         jsonldReport: Boolean = true,
                         multiPlatform: Boolean = true): Future[Assertion] = {

    val configuration = AMLConfiguration.predefined()
    val client        = configuration.baseUnitClient()
    for {
      parsed    <- client.parseDialect("file://" + path + dialectPath)
      report    <- client.validate(parsed.dialect).map(report => report.merge(AMFValidationReport.unknownProfile(parsed)))
      assertion <- reportComparator.assertReport(report, goldenReport.map(g => s"$path/$g"), jsonldReport)
    } yield {
      assertion
    }
  }
}
