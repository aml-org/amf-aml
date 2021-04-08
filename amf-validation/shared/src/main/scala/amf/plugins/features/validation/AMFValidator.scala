package amf.plugins.features.validation

import amf.client.execution.BaseExecutionEnvironment
import amf.client.plugins.{AMFDocumentPlugin, AMFValidationPlugin}
import amf.core.annotations.SourceVendor
import amf.core.benchmark.ExecutionLog
import amf.core.errorhandling.AmfStaticReportBuilder
import amf.core.model.document.{BaseUnit, Document, Fragment, Module}
import amf.core.rdf.RdfModel
import amf.core.registries.AMFPluginsRegistry
import amf.core.remote.{Oas30, Raml08, Vendor}
import amf.core.services.RuntimeValidator.{CustomShaclFunctions, validatorOption}
import amf.core.services.{RuntimeValidator, ValidationOptions => LegacyValidationOptions}
import amf.core.unsafe.PlatformSecrets
import amf.core.validation.core.{ValidationProfile, ValidationReport, ValidationSpecification}
import amf.core.validation.{AMFValidationReport, EffectiveValidations}
import amf.internal.environment.Environment
import amf.plugins.features.validation.emitters.{JSLibraryEmitter, ShaclJsonLdShapeGraphEmitter}
import amf._
import amf.client.remod.amfcore.plugins.validate.{AMFValidatePlugin, ValidationOptions}
import amf.plugins.features.validation.shacl.FullShaclValidator
import amf.plugins.features.validation.shacl.custom.CustomShaclValidator

import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

protected[amf] trait AMFValidator extends RuntimeValidator with PlatformSecrets {
  protected var customValidationProfiles: Map[String, () => ValidationProfile]       = Map.empty
  protected var customValidationProfilesPlugins: Map[String, Seq[AMFValidatePlugin]] = Map.empty

  // All the profiles are collected here, plugins can generate their own profiles
  protected def profiles: Map[String, () => ValidationProfile] =
    AMFPluginsRegistry.documentPlugins.foldLeft(Map[String, () => ValidationProfile]()) {
      case (acc, domainPlugin: AMFValidationPlugin) => acc ++ domainPlugin.domainValidationProfiles(platform)
      case (acc, _)                                 => acc
    } ++ customValidationProfiles

  // Mapping from profile to domain plugin
  protected def profilesPlugins: Map[String, Seq[AMFValidatePlugin]] =
    AMFPluginsRegistry.documentPlugins.foldLeft(Map[String, Seq[AMFValidatePlugin]]()) {
      case (acc, domainPlugin: AMFValidationPlugin) =>
        val toPut =
          domainPlugin.domainValidationProfiles(platform).keys.foldLeft(Map[String, Seq[AMFValidatePlugin]]()) {
            case (accProfiles, profileName) =>
              accProfiles.updated(profileName, domainPlugin.getRemodValidatePlugins())
          }
        acc ++ toPut
      case (acc, _) => acc
    } ++ customValidationProfilesPlugins

  protected[amf] def computeValidations(
      profileName: ProfileName,
      computed: EffectiveValidations = new EffectiveValidations()): EffectiveValidations = {
    val maybeProfile = profiles.get(profileName.profile) match {
      case Some(profileGenerator) => Some(profileGenerator())
      case _                      => None
    }

    maybeProfile match {
      case Some(foundProfile) =>
        if (foundProfile.baseProfile.isDefined) {
          computeValidations(foundProfile.baseProfile.get, computed).someEffective(foundProfile)
        } else {
          computed.someEffective(foundProfile)
        }
      case None => computed
    }
  }

  def shaclValidation(
      model: BaseUnit,
      validations: EffectiveValidations,
      customFunctions: CustomShaclFunctions,
      options: LegacyValidationOptions)(implicit executionContext: ExecutionContext): Future[ValidationReport] = {
    val validationSeq = validations.effective.values.toSeq
    if (options.isPartialValidation) new CustomShaclValidator(model, validationSeq, customFunctions, options).run
    else new FullShaclValidator().validate(model, validations, options)
  }

  private def profileForUnit(unit: BaseUnit, given: ProfileName): ProfileName = {
    given match {
      case OasProfile =>
        getSource(unit) match {
          case Some(Oas30) => Oas30Profile
          case _           => Oas20Profile
        }
      case RamlProfile =>
        getSource(unit) match {
          case Some(Raml08) => Raml08Profile
          case _            => Raml10Profile
        }
      case _ => given
    }

  }

  private def getSource(unit: BaseUnit): Option[Vendor] = unit match {
    case d: Document => d.encodes.annotations.find(classOf[SourceVendor]).map(_.vendor)
    case m: Module   => m.annotations.find(classOf[SourceVendor]).map(_.vendor)
    case f: Fragment => f.encodes.annotations.find(classOf[SourceVendor]).map(_.vendor)
    case _           => None
  }

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
  def shapesGraph(validations: EffectiveValidations, profileName: ProfileName = RamlProfile): String = {
    new ShaclJsonLdShapeGraphEmitter(profileName).emit(customValidations(validations))
  }

  def customValidations(validations: EffectiveValidations): Seq[ValidationSpecification] =
    validations.effective.values.toSeq.filter(s => !s.isParserSide)

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
