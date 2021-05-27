package amf.plugins.document.vocabularies

import amf.client.parse.DefaultErrorHandler
import amf.client.remod.amfcore.plugins.validate.ValidationResult
import amf.core.errorhandling.UnhandledErrorHandler
import amf.core.model.document.BaseUnit
import amf.core.resolution.pipelines.TransformationPipelineRunner
import amf.core.services.{RuntimeValidator, ValidationOptions}
import amf.core.validation.core.ValidationProfile
import amf.core.validation.{AMFValidationReport, EffectiveValidations, ShaclReportAdaptation}
import amf.plugins.document.vocabularies.model.document.DialectInstanceUnit
import amf.plugins.document.vocabularies.resolution.pipelines.DialectInstanceTransformationPipeline
import amf.{ProfileName, Raml10Profile}

import scala.concurrent.{ExecutionContext, Future}

class AMLValidator(registry: DialectsRegistry) extends ShaclReportAdaptation {

  def validate(baseUnit: BaseUnit, profile: ProfileName, validations: EffectiveValidations)(
      implicit executionContext: ExecutionContext): Future[ValidationResult] = {

    baseUnit match {
      case dialectInstance: DialectInstanceUnit =>
        val pipelineRunner          = TransformationPipelineRunner(UnhandledErrorHandler)
        val resolvedModel           = pipelineRunner.run(dialectInstance, DialectInstanceTransformationPipeline())
        val dependenciesValidations = computeValidationProfilesOfDependencies(dialectInstance)

        for {
          validationsFromDeps <- dependenciesValidations
          shaclReport <- RuntimeValidator.shaclValidation(resolvedModel,
                                                          addValidations(validations, validationsFromDeps),
                                                          options = new ValidationOptions().withFullValidation())
        } yield {
          val report = adaptToAmfReport(baseUnit, profile, shaclReport, validations)
          ValidationResult(resolvedModel, report)
        }

      case _ =>
        // TODO ARM: add logging that stage was skipped
        Future.successful(ValidationResult(baseUnit, AMFValidationReport.empty(baseUnit.id, profile)))
    }
  }

  private def computeValidationProfilesOfDependencies(dialectInstance: DialectInstanceUnit)(
      implicit executionContext: ExecutionContext) = {
    Future
      .sequence(registerGraphDependencies(dialectInstance)) map { dialects =>
      dialects.map(DialectValidationProfileComputation.computeProfileFor(_, registry))
    }
  }

  private def registerGraphDependencies(dialectInstance: DialectInstanceUnit) = {
    dialectInstance.graphDependencies.map { instance =>
      registry.registerDialect(instance.value())
    }
  }

  private def addValidations(validations: EffectiveValidations,
                             dependenciesValidations: Seq[ValidationProfile]): EffectiveValidations = {
    dependenciesValidations.foldLeft(validations) {
      case (effective, profile) => effective.someEffective(profile)
    }
  }
}
