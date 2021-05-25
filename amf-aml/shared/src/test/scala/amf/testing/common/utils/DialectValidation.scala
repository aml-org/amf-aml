package amf.testing.common.utils

import amf.{ProfileName, ProfileNames}
import amf.client.environment.AMLConfiguration
import amf.client.parse.DefaultErrorHandler
import amf.client.remod.ParseConfiguration
import amf.client.remod.amfcore.plugins.validate.ValidationConfiguration
import amf.core.services.RuntimeValidator
import amf.core.unsafe.PlatformSecrets
import amf.core.{AMFCompiler, CompilerContextBuilder}
import amf.plugins.document.vocabularies.model.document.Dialect
import org.scalatest.{Assertion, AsyncFunSuite}

import scala.concurrent.Future

trait DialectValidation extends AsyncFunSuite with PlatformSecrets with DefaultAMLInitialization {

  protected val path: String
  protected val reportComparator: ReportComparator = UniquePlatformReportComparator

  protected def validate(dialectPath: String,
                         goldenReport: Option[String],
                         jsonldReport: Boolean = true,
                         multiPlatform: Boolean = true): Future[Assertion] = {

    val eh            = DefaultErrorHandler()
    val configuration = AMLConfiguration.forEH(eh)
    for {
      dialect <- {
        new AMFCompiler(
            new CompilerContextBuilder("file://" + path + dialectPath, platform, configuration.parseConfiguration)
              .build(),
            Some("application/aml")
        ).build()
      }
      report <- {
        RuntimeValidator(
            dialect,
            ProfileNames.AML,
            resolved = false,
            new ValidationConfiguration(configuration)
        )
      }
      assertion <- reportComparator.assertReport(report, goldenReport.map(g => s"$path/$g"), jsonldReport)
    } yield {
      assertion
    }
  }
}
