package amf.plugins.features.validation

import amf._
import amf.client.plugins.AMFValidationPlugin
import amf.client.remod.amfcore.plugins.validate.{AMFValidatePlugin, ValidationConfiguration, ValidationOptions}
import amf.core.annotations.SourceVendor
import amf.core.model.document.{BaseUnit, Document, Fragment, Module}
import amf.core.rdf.RdfModel
import amf.core.registries.AMFPluginsRegistry
import amf.core.remote._
import amf.core.services.RuntimeValidator
import amf.core.unsafe.PlatformSecrets
import amf.core.validation.core.{ValidationProfile, ValidationSpecification}
import amf.core.validation.{AMFValidationReport, EffectiveValidations, FailFastValidationRunner}
import amf.plugins.features.validation.emitters.ShaclJsonLdShapeGraphEmitter

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

  private def profileForUnit(unit: BaseUnit, given: ProfileName): ProfileName = {
    given match {
      case Oas20Profile if getSource(unit).forall(_ == Oas20)   => Oas20Profile
      case Oas30Profile if getSource(unit).forall(_ == Oas30)   => Oas30Profile
      case Raml10Profile if getSource(unit).forall(_ == Raml10) => Raml10Profile
      case Raml08Profile if getSource(unit).forall(_ == Raml08) => Raml08Profile
      case _                                                    => given
    }

  }

  private def getSource(unit: BaseUnit): Option[Vendor] = unit match {
    case d: Document => d.encodes.annotations.find(classOf[SourceVendor]).map(_.vendor)
    case m: Module   => m.annotations.find(classOf[SourceVendor]).map(_.vendor)
    case f: Fragment => f.encodes.annotations.find(classOf[SourceVendor]).map(_.vendor)
    case _           => None
  }

  def validate(model: BaseUnit,
               givenProfile: ProfileName,
               resolved: Boolean,
               config: ValidationConfiguration): Future[AMFValidationReport] = {

    implicit val executionContext: ExecutionContext = config.executionContext

    val profileName = profileForUnit(model, givenProfile)
    // TODO: we shouldn't compute validations if there are parser errors. This will be removed after ErrorHandler is returned in parsing.

    profilesPlugins
      .get(profileName.profile)
      .map { plugins =>
        val validations = computeValidations(profileName)
        val options     = new ValidationOptions(profileName, validations, config)
        if (resolved) model.resolved = true
        FailFastValidationRunner(plugins, options).run(model)

      }
      .getOrElse(successful(profileNotFoundWarningReport(model, profileName)))
  }

  protected def profileNotFoundWarningReport(model: BaseUnit, profileName: ProfileName): AMFValidationReport = {
    AMFValidationReport(model.location().getOrElse(model.id), profileName, Seq())
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
    PlatformValidator.instance().shapes(validations, functionUrls)

  /**
    * Generates a JSON-LD graph with the SHACL shapes for the requested profile name
    * @return JSON-LD graph
    */
  def emitShapesGraph(profileName: ProfileName): String = {
    val effectiveValidations = computeValidations(profileName)
    shapesGraph(effectiveValidations, profileName)
  }
}
