package amf.dialects

import amf.ProfileName
import amf.client.environment.AmlEnvironment
import amf.client.parse.DefaultParserErrorHandler
import amf.core.registries.AMFPluginsRegistry
import amf.core.{AMFCompiler, CompilerContextBuilder}
import amf.core.services.RuntimeValidator
import amf.core.unsafe.PlatformSecrets
import amf.plugins.document.vocabularies.AMLPlugin
import amf.plugins.document.vocabularies.model.document.Dialect
import org.scalatest.{Assertion, AsyncFunSuite}

import scala.concurrent.Future

trait DialectValidation extends AsyncFunSuite with PlatformSecrets with DefaultAmfInitialization {

  protected val path: String
  protected val reportComparator: ReportComparator = UniquePlatformReportComparator

  protected def validate(dialectPath: String, goldenReport: Option[String], jsonldReport: Boolean = true, multiPlatform: Boolean = true): Future[Assertion] = {
    for {
      dialect <- {
        new AMFCompiler(
          new CompilerContextBuilder("file://" + path + dialectPath, platform, eh = DefaultParserErrorHandler.withRun())
            .build(AmlEnvironment.aml()),
          Some("application/yaml"),
          Some(AMLPlugin.ID)
        ).build()
      }
      report <- {
        RuntimeValidator(
          dialect,
          ProfileName(dialect.asInstanceOf[Dialect].nameAndVersion())
        )
      }
      assertion <- reportComparator.assertReport(report, goldenReport.map(g => s"$path/$g"), jsonldReport)
    } yield {
      assertion
    }
  }
}
