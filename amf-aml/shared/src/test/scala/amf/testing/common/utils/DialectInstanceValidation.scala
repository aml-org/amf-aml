package amf.testing.common.utils

import amf.client.environment.AMLConfiguration
import amf.{ProfileName, ProfileNames}
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

  protected def validation(dialect: String,
                           instance: String,
                           path: String = basePath,
                           config: AMLConfiguration = AMLConfiguration.predefined()): Future[AMFValidationReport] = {
    val dialectPath  = s"$path/$dialect"
    val instancePath = s"$path/$instance"
    val client       = config.createClient()
    for {
      dialectResult  <- client.parseDialect(s"$path/$dialect")
      nextConfig     <- config.withDialect(dialectPath)
      instanceResult <- nextConfig.createClient().parseDialectInstance(instancePath)
      report <- {
        if (!instanceResult.report.conforms) Future.successful(instanceResult.report)
        else nextConfig.createClient().validate(instanceResult.dialectInstance, dialectResult.dialect.profileName.get)
      }
    } yield {
      report
    }
  }

  protected def validationWithCustomProfile(dialect: String,
                                            instance: String,
                                            profile: ProfileName,
                                            name: String,
                                            directory: String = basePath): Future[AMFValidationReport] = {

    withDialect(s"$directory/$dialect") { (_, config) =>
      for {
        customValConfig <- config.withCustomValidationsEnabled
        finalConfig     <- customValConfig.withCustomProfile(s"$directory/${profile.profile}")
        instance        <- finalConfig.createClient().parseDialectInstance(s"$directory/$instance").map(_.dialectInstance)
        report          <- finalConfig.createClient().validate(instance, ProfileName(name))
      } yield {
        report
      }
    }
  }
}
