package amf.dialects

import amf.ProfileName
import amf.client.parse.DefaultParserErrorHandler
import amf.core.services.RuntimeValidator
import amf.core.unsafe.PlatformSecrets
import amf.core.validation.AMFValidationReport
import amf.core.{AMFCompiler, CompilerContextBuilder}
import amf.core.io.FileAssertionTest
import amf.core.registries.AMFPluginsRegistry
import amf.plugins.document.vocabularies.model.document.Dialect
import amf.plugins.features.validation.AMFValidatorPlugin
import amf.plugins.features.validation.emitters.ValidationReportJSONLDEmitter
import org.scalatest.{Assertion, AsyncFunSuite, Matchers}

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

trait DialectInstanceValidation extends AsyncFunSuite with PlatformSecrets with DefaultAmfInitialization {

  def basePath: String

  protected def validation(dialect: String, instance: String, path: String = basePath): Future[AMFValidationReport] = {
    val dialectContext  = compilerContext(s"$path/$dialect")
    val instanceContext = compilerContext(s"$path/$instance")

    for {
      dialect <- {
        new AMFCompiler(
            dialectContext,
            Some("application/yaml"),
            None
        ).build()
      }
      instance <- {
        new AMFCompiler(
            instanceContext,
            Some("application/yaml"),
            None
        ).build()
      }
      report <- RuntimeValidator(instance, ProfileName(dialect.asInstanceOf[Dialect].nameAndVersion()))
    } yield {
      report
    }
  }

  protected def validationWithCustomProfile(dialect: String,
                                            instance: String,
                                            profile: ProfileName,
                                            name: String,
                                            directory: String = basePath): Future[AMFValidationReport] = {
    val dialectContext  = compilerContext(s"$directory/$dialect")
    val instanceContext = compilerContext(s"$directory/$instance")

    for {
      dialect <- {
        new AMFCompiler(
            dialectContext,
            Some("application/yaml"),
            None
        ).build()
      }
      profile <- {
        AMFValidatorPlugin.loadValidationProfile(s"$directory/${profile.profile}",
                                                 errorHandler = dialectContext.parserContext.eh)
      }
      instance <- {

        new AMFCompiler(
            instanceContext,
            Some("application/yaml"),
            None
        ).build()
      }
      report <- {
        RuntimeValidator(
            instance,
            ProfileName(name)
        )
      }
    } yield {
      report
    }
  }

  private def compilerContext(url: String) =
    new CompilerContextBuilder(url, platform, eh = DefaultParserErrorHandler.withRun())
      .withBaseEnvironment(AMFPluginsRegistry.obtainStaticConfig())
      .build()

}
