package amf.plugins.features.validation.custom

import amf._
import amf.client.execution.BaseExecutionEnvironment
import amf.client.plugins.{AMFFeaturePlugin, AMFPlugin}
import amf.client.remod.AMFGraphConfiguration
import amf.client.remod.amfcore.plugins.validate.AMFValidatePlugin
import amf.core.AMFCompiler
import amf.core.benchmark.ExecutionLog
import amf.core.errorhandling.AMFErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.model.domain.DomainElement
import amf.core.registries.AMFPluginsRegistry
import amf.core.remote._
import amf.core.services.RuntimeValidator
import amf.core.validation.ShaclReportAdaptation
import amf.core.validation.core.ValidationProfile
import amf.internal.environment.Environment
import amf.plugins.document.graph.AMFGraphPlugin
import amf.plugins.document.vocabularies.AMLPlugin
import amf.plugins.document.vocabularies.AMLValidationLegacyPlugin.amlPlugin
import amf.plugins.document.vocabularies.custom.ParsedValidationProfile
import amf.plugins.document.vocabularies.model.document.DialectInstance
import amf.plugins.document.vocabularies.model.domain.DialectDomainElement
import amf.plugins.features.validation.{AMFValidator, PlatformValidator}
import amf.plugins.syntax.SYamlSyntaxPlugin

import scala.concurrent.{ExecutionContext, Future}

object AMFValidatorPlugin extends AMFFeaturePlugin with RuntimeValidator with ShaclReportAdaptation with AMFValidator {

  override val ID = "AMF Validation"

  override def init()(implicit executionContext: ExecutionContext): Future[AMFPlugin] = {
    // Registering ourselves as the runtime validator
    RuntimeValidator.register(AMFValidatorPlugin)
    ExecutionLog.log("Register RDF framework")
    platform.rdfFramework = Some(PlatformValidator.instance)
    ExecutionLog.log(s"AMFValidatorPlugin#init: registering validation dialect")
//    AMLPlugin.registry.registerDialect(PROFILE_DIALECT_URL, ValidationDialectText.text, executionContext) map { _ =>
//      ExecutionLog.log(s"AMFValidatorPlugin#init: validation dialect registered")
//      this
//    }
    Future.successful(AMLPlugin)
  }

  override def dependencies() = Seq(SYamlSyntaxPlugin, AMLPlugin, AMFGraphPlugin)

  override def loadValidationProfile(
      validationProfilePath: String,
      env: Environment = Environment(),
      errorHandler: AMFErrorHandler,
      exec: BaseExecutionEnvironment = platform.defaultExecutionEnvironment): Future[ProfileName] = {

    implicit val executionContext: ExecutionContext = exec.executionContext

    val conf = AMFPluginsRegistry
      .obtainStaticConfig()
      .withErrorHandlerProvider(() => errorHandler)
      .withResourceLoaders(env.loaders.toList)
    val finalConf = env.resolver.fold(conf)(e => conf.withUnitCache(e))

    parseProfile(validationProfilePath, finalConf)
      .map { getEncodesOrExit }
      .map { loadProfilesFromDialectOrExit }
  }

  private def parseProfile(validationProfilePath: String, amfConf: AMFGraphConfiguration)(
      implicit executionContext: ExecutionContext) = {

    AMFCompiler(
        validationProfilePath,
        Some("application/aml"),
        Context(platform),
        cache = Cache(),
        amfConf.parseConfiguration
    ).build()
  }

  private val PROFILE_DIALECT_URL = "http://a.ml/dialects/profile.raml"

  private def getEncodesOrExit(unit: BaseUnit): DomainElement = unit match {
    case parsed: DialectInstance if parsed.definedBy().is(PROFILE_DIALECT_URL) => parsed.encodes
    case _ =>
      throw new Exception("Trying to load as a validation profile that does not match the Validation Profile dialect")
  }

  private def loadProfilesFromDialectOrExit(domainElement: DomainElement) = domainElement match {
    case encoded: DialectDomainElement if encoded.definedBy.name.is("profileNode") =>
      val validationProfile = ParsedValidationProfile(encoded)
      val domainPlugin: Seq[AMFValidatePlugin] = getProfilePluginFor(validationProfile)
        .orElse(getProfilePluginFor(validationProfile.baseProfile.getOrElse(AmfProfile)))
        .getOrElse(Seq(amlPlugin()))
      AMFPluginsRegistry.registerValidationProfile(validationProfile)
      customValidationProfilesPlugins += (validationProfile.name.profile -> domainPlugin)
      validationProfile.name

    case _ =>
      throw new Exception("Trying to load as a validation profile that does not match the Validation Profile dialect")
  }

  private def getProfilePluginFor(profileName: ProfileName): Option[Seq[AMFValidatePlugin]] =
    profilesPlugins.get(profileName.profile)
  private def getProfilePluginFor(validationProfile: ValidationProfile): Option[Seq[AMFValidatePlugin]] =
    getProfilePluginFor(validationProfile.name)
}
