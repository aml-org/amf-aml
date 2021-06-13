package amf.testing.common.utils

import amf.client.environment.AMLConfiguration
import amf.core.client.scala.errorhandling.DefaultErrorHandler
import amf.core.internal.parser.ParseConfiguration
import amf.core.internal.validation.ValidationConfiguration
import amf.core.internal.unsafe.PlatformSecrets
import amf.core.client.scala.validation.AMFValidationReport
import amf.core.internal.parser.{AMFCompiler, CompilerContextBuilder}
import amf.plugins.document.vocabularies.model.document.Dialect
import org.scalatest.{Assertion, AsyncFunSuite}

import scala.concurrent.Future

trait DialectValidation extends AsyncFunSuite with PlatformSecrets {

  protected val path: String
  protected val reportComparator: ReportComparator = UniquePlatformReportComparator

  protected def validate(dialectPath: String,
                         goldenReport: Option[String],
                         jsonldReport: Boolean = true,
                         multiPlatform: Boolean = true): Future[Assertion] = {

    val eh            = DefaultErrorHandler()
    val configuration = AMLConfiguration.forEH(eh)
    val client        = configuration.createClient()
    for {
      report    <- client.parseDialect("file://" + path + dialectPath).map(AMFValidationReport.unknownProfile(_))
      assertion <- reportComparator.assertReport(report, goldenReport.map(g => s"$path/$g"), jsonldReport)
    } yield {
      assertion
    }
  }
}
