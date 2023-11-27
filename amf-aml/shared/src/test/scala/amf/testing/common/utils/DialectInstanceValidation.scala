package amf.testing.common.utils

import amf.aml.client.scala.AMLConfiguration
import amf.core.client.scala.validation.AMFValidationReport
import amf.core.common.AsyncFunSuiteWithPlatformGlobalExecutionContext

import scala.concurrent.Future

trait DialectInstanceValidation
    extends AsyncFunSuiteWithPlatformGlobalExecutionContext
    with DialectRegistrationHelper
    with AMLParsingHelper {

  def basePath: String

  protected def validation(
      dialect: String,
      instance: String,
      path: String = basePath,
      config: AMLConfiguration = AMLConfiguration.predefined()
  ): Future[AMFValidationReport] = {
    val dialectPath  = s"$path/$dialect"
    val instancePath = s"$path/$instance"
    val client       = config.baseUnitClient()
    for {
      dialectResult  <- client.parseDialect(dialectPath)
      nextConfig     <- Future.successful(config.withDialect(dialectResult.dialect))
      instanceResult <- nextConfig.baseUnitClient().parseDialectInstance(instancePath)
      report <- {
        if (!instanceResult.conforms) Future.successful(AMFValidationReport.unknownProfile(instanceResult))
        else
          nextConfig.baseUnitClient().validate(instanceResult.dialectInstance)
      }
    } yield {
      report
    }
  }
}
