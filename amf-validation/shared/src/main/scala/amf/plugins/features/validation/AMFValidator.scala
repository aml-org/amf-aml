package amf.plugins.features.validation

import amf._
import amf.client.execution.BaseExecutionEnvironment
import amf.client.plugins.AMFValidationPlugin
import amf.client.remod.amfcore.plugins.validate.{AMFValidatePlugin, ValidationOptions}
import amf.core.errorhandling.AmfStaticReportBuilder
import amf.core.model.document.BaseUnit
import amf.core.rdf.RdfModel
import amf.core.registries.AMFPluginsRegistry
import amf.core.remote._
import amf.core.services.RuntimeValidator.CustomShaclFunctions
import amf.core.services.{RuntimeValidator, ValidationOptions => LegacyValidationOptions}
import amf.core.unsafe.PlatformSecrets
import amf.core.validation.core.{ValidationProfile, ValidationReport, ValidationSpecification}
import amf.core.validation.{AMFValidationReport, EffectiveValidations}
import amf.internal.environment.Environment
import amf.plugins.features.validation.emitters.ShaclJsonLdShapeGraphEmitter
import amf.plugins.features.validation.shacl.FullShaclValidator
import amf.plugins.features.validation.shacl.custom.CustomShaclValidator

import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

protected[amf] trait AMFValidator extends RuntimeValidator with PlatformSecrets {

  protected var customValidationProfilesPlugins: Map[String, Seq[AMFValidatePlugin]] = Map.empty

  // All the profiles are collected here, plugins can generate their own profiles
  protected def profiles: Map[String, ValidationProfile] =
    AMFPluginsRegistry.staticConfiguration.registry.constraintsRules.map {
      case (profileName, profile) => (profileName.p, profile)
    }

  // Mapping from profile to domain plugin
  protected def profilesPlugins: Map[String, Seq[AMFValidatePlugin]] =
    AMFPluginsRegistry.documentPlugins.foldLeft(Map[String, Seq[AMFValidatePlugin]]()) {
      case (acc, domainPlugin: AMFValidationPlugin) =>
        val toPut =
          domainPlugin.domainValidationProfiles.foldLeft(Map[String, Seq[AMFValidatePlugin]]()) {
            case (accProfiles, profile) =>
              accProfiles.updated(profile.name.p, domainPlugin.getRemodValidatePlugins())
          }
        acc ++ toPut
      case (acc, _) => acc
    } ++ customValidationProfilesPlugins

  protected[amf] def computeValidations(
      profileName: ProfileName,
      computed: EffectiveValidations = new EffectiveValidations()): EffectiveValidations = {

    profiles
      .get(profileName.profile)
      .map { foundProfile =>
        addBaseProfileValidations(computed, foundProfile)
      }
      .getOrElse(computed)
  }

  private def addBaseProfileValidations(computed: EffectiveValidations, foundProfile: ValidationProfile) = {
    foundProfile.baseProfile
      .map(base => computeValidations(base, computed).someEffective(foundProfile))
      .getOrElse(computed.someEffective(foundProfile))
  }

  def shaclValidation(
      model: BaseUnit,
      validations: EffectiveValidations,
      customFunctions: CustomShaclFunctions,
      options: LegacyValidationOptions)(implicit executionContext: ExecutionContext): Future[ValidationReport] = {
    val validationSet = validations.effective.values.toSet
    if (options.isPartialValidation) new CustomShaclValidator(model, customFunctions, options).run(validationSet)
    else new FullShaclValidator().validate(model, validationSet.toSeq, options)
  }

  private def profileForUnit(unit: BaseUnit, given: ProfileName): ProfileName = {
    given match {
      case Oas20Profile if getSource(unit).forall(_ == Oas20)   => Oas20Profile
      case Oas30Profile if getSource(unit).forall(_ == Oas30)   => Oas30Profile
      case Raml10Profile if getSource(unit).forall(_ == Raml10) => Raml10Profile
      case Raml08Profile if getSource(unit).forall(_ == Raml08) => Raml08Profile
      case _                                                    => given
    }

  }

  private def getSource(unit: BaseUnit): Option[Vendor] = unit.sourceVendor

  def validate(model: BaseUnit,
               given: ProfileName,
               messageStyle: MessageStyle,
               env: Environment,
               resolved: Boolean = false,
               exec: BaseExecutionEnvironment = platform.defaultExecutionEnvironment): Future[AMFValidationReport] = {

    val profileName = profileForUnit(model, given)
    // TODO: we shouldn't compute validations if there are parser errors. This will be removed after ErrorHandler is returned in parsing.
    val report = new AmfStaticReportBuilder(model, profileName).buildFromStatic()

    if (!report.conforms) Future.successful(report)
    else validate(model, profileName, env, resolved, exec)
  }

  private def validate(model: BaseUnit,
                       profileName: ProfileName,
                       env: Environment,
                       resolved: Boolean,
                       exec: BaseExecutionEnvironment): Future[AMFValidationReport] = {

    implicit val executionContext: ExecutionContext = exec.executionContext

    profilesPlugins
      .get(profileName.profile)
      .map { plugins =>
        val validations = computeValidations(profileName)
        val options     = new ValidationOptions(profileName, env, validations)
        if (resolved) model.resolved = true
        FailFastValidationRunner(plugins, options).run(model)

      }
      .getOrElse(successful(profileNotFoundWarningReport(model, profileName)))
  }

  protected def profileNotFoundWarningReport(model: BaseUnit, profileName: ProfileName): AMFValidationReport = {
    AMFValidationReport(conforms = true, model.location().getOrElse(model.id), profileName, Seq())
  }

  /**
    * Generates a JSON-LD graph with the SHACL shapes for the requested profile validations
    * @return JSON-LD graph
    */
  def shapesGraph(validations: EffectiveValidations, profileName: ProfileName = Raml10Profile): String = {
    new ShaclJsonLdShapeGraphEmitter(profileName).emit(customValidations(validations.effective.values.toSeq))
  }

  def customValidations(validations: Seq[ValidationSpecification]): Seq[ValidationSpecification] =
    validations.filter(s => !s.isParserSide)

  /**
    * Returns a native RDF model with the SHACL shapes graph
    */
  def shaclModel(validations: Seq[ValidationSpecification],
                 functionUrls: String,
                 messageStyle: MessageStyle): RdfModel =
    PlatformValidator.instance.shapes(validations, functionUrls)

  /**
    * Generates a JSON-LD graph with the SHACL shapes for the requested profile name
    * @return JSON-LD graph
    */
  def emitShapesGraph(profileName: ProfileName): String = {
    val effectiveValidations = computeValidations(profileName)
    shapesGraph(effectiveValidations, profileName)
  }
}
