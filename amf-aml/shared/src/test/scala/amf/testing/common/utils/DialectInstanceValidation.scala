package amf.testing.common.utils

import amf.aml.client.scala.AMLConfiguration
import amf.core.client.common.validation.ProfileName
import amf.core.client.scala.validation.AMFValidationReport
import amf.core.internal.unsafe.PlatformSecrets
import org.scalatest.AsyncFunSuite

import scala.concurrent.Future

trait DialectInstanceValidation
    extends AsyncFunSuite
    with PlatformSecrets
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
        if (!instanceResult.conforms) Future.successful(AMFValidationReport.unknownProfile(instanceResult))
        else nextConfig.createClient().validate(instanceResult.dialectInstance, dialectResult.dialect.profileName.get)
      }
    } yield {
      report
    }
  }
}
