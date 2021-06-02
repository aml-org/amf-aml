package amf.plugins.document.vocabularies

import amf.ProfileName
import amf.client.remod.amfcore.plugins.validate.ValidationResult
import amf.core.errorhandling.UnhandledErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.resolution.pipelines.TransformationPipelineRunner
import amf.core.services.{RuntimeValidator, ShaclValidationOptions}
import amf.core.validation.core.ValidationProfile
import amf.core.validation.{AMFValidationReport, EffectiveValidations, ShaclReportAdaptation}
import amf.plugins.document.vocabularies.model.document.{Dialect, DialectInstanceUnit}
import amf.plugins.document.vocabularies.resolution.pipelines.DialectInstanceTransformationPipeline
import amf.plugins.features.validation.PlatformValidator
import amf.plugins.features.validation.shacl.FullShaclValidator

import scala.concurrent.{ExecutionContext, Future}

class AMLValidator(knownDialects: Seq[Dialect], constraints: Map[ProfileName, ValidationProfile])
    extends ShaclReportAdaptation {

  def validate(baseUnit: BaseUnit, profile: ProfileName, validations: EffectiveValidations)(
      implicit executionContext: ExecutionContext): Future[ValidationResult] = {

    baseUnit match {
      case dialectInstance: DialectInstanceUnit =>
        val pipelineRunner      = TransformationPipelineRunner(UnhandledErrorHandler)
        val resolvedModel       = pipelineRunner.run(dialectInstance, DialectInstanceTransformationPipeline())
        val validationsFromDeps = computeValidationProfilesOfDependencies(dialectInstance, knownDialects, constraints)
        val validator           = new FullShaclValidator(PlatformValidator.instance(), new ShaclValidationOptions())
        val finalValidations    = addValidations(validations, validationsFromDeps).effective.values.toSeq
        for {
          shaclReport <- validator.validate(resolvedModel, finalValidations)
        } yield {
          val report = adaptToAmfReport(baseUnit, profile, shaclReport, validations)
          ValidationResult(resolvedModel, report)
        }

      case _ =>
        // TODO ARM: add logging that stage was skipped
        Future.successful(ValidationResult(baseUnit, AMFValidationReport.empty(baseUnit.id, profile)))
    }
  }

  private def computeValidationProfilesOfDependencies(
      dialectInstance: DialectInstanceUnit,
      knownDialects: Seq[Dialect],
      constraints: Map[ProfileName, ValidationProfile])(implicit executionContext: ExecutionContext) = {
    dialectInstance.graphDependencies
      .flatMap(_.option())
      .flatMap(dep => knownDialects.find(p => p.location().contains(dep)))
      .flatMap(dialect => dialect.profileName)
      .flatMap(constraints.get)

  }

  private def addValidations(validations: EffectiveValidations,
                             dependenciesValidations: Seq[ValidationProfile]): EffectiveValidations = {
    dependenciesValidations.foldLeft(validations) {
      case (effective, profile) => effective.someEffective(profile)
    }
  }
}
