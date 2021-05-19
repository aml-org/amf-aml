package amf.testing.common.utils

import amf.ProfileName
import amf.client.remod.amfcore.plugins.validate.ValidationConfiguration
import amf.core.services.RuntimeValidator
import amf.core.unsafe.PlatformSecrets
import amf.core.validation.AMFValidationReport
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
    withDialect(s"$path/$dialect") { (dialect, configuration) =>
      for {
        instance <- parse(s"$path/$instance", platform, None, configuration)
        report <- RuntimeValidator(instance,
                                   ProfileName(dialect.nameAndVersion()),
                                   resolved = false,
                                   new ValidationConfiguration(configuration))
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

    withDialect(s"$directory/$dialect") { (_, config) =>
      for {
        _ <- {
          AMFValidatorPlugin.loadValidationProfile(s"$directory/${profile.profile}",
                                                   errorHandler = config.errorHandlerProvider.errorHandler())
        }
        instance <- parse(s"$directory/$instance", platform, None, config)
        report   <- RuntimeValidator(instance, ProfileName(name), resolved = false, new ValidationConfiguration(config))
      } yield {
        report
      }
    }
  }
}
