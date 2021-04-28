package amf.testing.common.utils

import amf.ProfileName
import amf.client.parse.DefaultParserErrorHandler
import amf.core.services.RuntimeValidator
import amf.core.unsafe.PlatformSecrets
import amf.core.validation.AMFValidationReport
import amf.core.{AMFCompiler, CompilerContextBuilder}
import amf.plugins.document.vocabularies.model.document.Dialect
import amf.plugins.features.validation.AMFValidatorPlugin
import org.scalatest.AsyncFunSuite

import scala.concurrent.Future

trait DialectInstanceValidation
    extends AsyncFunSuite
    with PlatformSecrets
    with DefaultAMLInitialization
    with DialectRegistrationHelper
    with AMLParsingHelper {

  def basePath: String

  protected def validation(dialect: String, instance: String, path: String = basePath): Future[AMFValidationReport] = {
    withDialect(s"$path/$dialect") { dialect =>
      for {
        instance <- parse(s"$path/$instance", platform, None)
        report   <- RuntimeValidator(instance, ProfileName(dialect.nameAndVersion()))
      } yield {
        report
      }
    }
  }

  protected def validationWithCustomProfile(dialect: String,
                                            instance: String,
                                            profile: ProfileName,
                                            name: String,
                                            directory: String = basePath): Future[AMFValidationReport] = {
    val ctx =
      new CompilerContextBuilder(s"$directory/$dialect", platform, eh = DefaultParserErrorHandler.withRun()).build()
    withDialect(s"$directory/$dialect") { _ =>
      for {
        _ <- {
          AMFValidatorPlugin.loadValidationProfile(s"$directory/${profile.profile}",
                                                   errorHandler = ctx.parserContext.eh)
        }
        instance <- parse(s"$directory/$instance", platform, None)
        report   <- RuntimeValidator(instance, ProfileName(name))
      } yield {
        report
      }
    }
  }
}
