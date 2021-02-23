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
import amf.plugins.features.validation.custom.AMFValidatorPlugin
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

trait DialectInstanceValidation extends AsyncFunSuite with PlatformSecrets {

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
    new CompilerContextBuilder(url, platform, eh = DefaultParserErrorHandler.withRun()).build(AMFPluginsRegistry.obtainStaticEnv())

}
