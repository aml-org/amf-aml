package amf.plugins.features.validation.custom

import amf._
import amf.client.execution.BaseExecutionEnvironment
import amf.client.parse.DefaultParserErrorHandler
import amf.client.plugins.{AMFFeaturePlugin, AMFPlugin}
import amf.core.benchmark.ExecutionLog
import amf.core.errorhandling.ErrorHandler
import amf.core.parser.errorhandler.AmfParserErrorHandler
import amf.core.remote._
import amf.core.services.{RuntimeCompiler, RuntimeValidator}
import amf.core.validation.ValidationResultProcessor
import amf.internal.environment.Environment
import amf.plugins.document.graph.AMFGraphPlugin
import amf.plugins.document.vocabularies.AMLPlugin
import amf.plugins.document.vocabularies.model.document.DialectInstance
import amf.plugins.document.vocabularies.model.domain.DialectDomainElement
import amf.plugins.features.validation.custom.model.{ParsedValidationProfile, ValidationDialectText}
import amf.plugins.features.validation.{AMFValidator, PlatformValidator}
import amf.plugins.syntax.SYamlSyntaxPlugin

import scala.concurrent.{ExecutionContext, Future}

object AMFValidatorPlugin extends AMFFeaturePlugin with RuntimeValidator with ValidationResultProcessor with AMFValidator{

  override val ID = "AMF Validation"

  override def init()(implicit executionContext: ExecutionContext): Future[AMFPlugin] = {
    // Registering ourselves as the runtime validator
    RuntimeValidator.register(AMFValidatorPlugin)
    ExecutionLog.log("Register RDF framework")
    platform.rdfFramework = Some(PlatformValidator.instance)
    ExecutionLog.log(s"AMFValidatorPlugin#init: registering validation dialect")
    AMLPlugin().registry.registerDialect(url, ValidationDialectText.text, executionContext) map { _ =>
      ExecutionLog.log(s"AMFValidatorPlugin#init: validation dialect registered")
      this
    }
  }

  override def dependencies() = Seq(SYamlSyntaxPlugin, AMLPlugin, AMFGraphPlugin)

  private val url = "http://a.ml/dialects/profile.raml"

  private def errorHandlerToParser(eh: ErrorHandler): AmfParserErrorHandler =
    DefaultParserErrorHandler.fromErrorHandler(eh)

  override def loadValidationProfile(
      validationProfilePath: String,
      env: Environment = Environment(),
      errorHandler: ErrorHandler,
      exec: BaseExecutionEnvironment = platform.defaultExecutionEnvironment): Future[ProfileName] = {

    implicit val executionContext: ExecutionContext = exec.executionContext

    RuntimeCompiler(
      validationProfilePath,
      Some("application/yaml"),
      Some(AMLPlugin.ID),
      Context(platform),
      cache = Cache(),
      env = env,
      errorHandler = errorHandlerToParser(errorHandler)
    ).map {
        case parsed: DialectInstance if parsed.definedBy().is(url) =>
          parsed.encodes
        case _ =>
          throw new Exception(
            "Trying to load as a validation profile that does not match the Validation Profile dialect")
      }
      .map {
        case encoded: DialectDomainElement if encoded.definedBy.name.is("profileNode") =>
          val profile = ParsedValidationProfile(encoded)
          val domainPlugin = profilesPlugins.get(profile.name.profile) match {
            case Some(plugin) => plugin
            case None =>
              profilesPlugins.get(profile.baseProfile.getOrElse(AmfProfile).profile) match {
                case Some(plugin) =>
                  plugin
                case None => AMLPlugin()

              }
          }
          customValidationProfiles += (profile.name.profile -> { () =>
            profile
          })
          customValidationProfilesPlugins += (profile.name.profile -> domainPlugin)
          profile.name

        case other =>
          throw new Exception(
            "Trying to load as a validation profile that does not match the Validation Profile dialect")
      }
  }
}

object ValidationMutex {}
