package amf.plugins.features.validation

import amf._
import amf.client.execution.BaseExecutionEnvironment
import amf.client.plugins.{AMFFeaturePlugin, AMFPlugin}
import amf.core.benchmark.ExecutionLog
import amf.core.errorhandling.AMFErrorHandler
import amf.core.services.RuntimeValidator
import amf.internal.environment.Environment
import amf.plugins.document.graph.AMFGraphPlugin
import amf.plugins.syntax.SYamlSyntaxPlugin

import scala.concurrent.{ExecutionContext, Future}

object AMFValidatorPlugin extends AMFFeaturePlugin with AMFValidator {

  override val ID = "AMF Validation"

  override def init()(implicit executionContext: ExecutionContext): Future[AMFPlugin] = {
    // Registering ourselves as the runtime validator
    RuntimeValidator.register(AMFValidatorPlugin)
    ExecutionLog.log("Register RDF framework")
    platform.rdfFramework = Some(PlatformValidator.instance())
    Future.successful(this)
  }

  override def dependencies() = Seq(SYamlSyntaxPlugin, AMFGraphPlugin)

  override def loadValidationProfile(
      validationProfilePath: String,
      env: Environment = Environment(),
      errorHandler: AMFErrorHandler,
      exec: BaseExecutionEnvironment = platform.defaultExecutionEnvironment): Future[ProfileName] = {

    throw new Exception("Cannot load a custom validation profile for this")
  }
}
