package amf.aml.internal.validate

import amf.aml.client.scala.model.document.{Dialect, DialectInstanceUnit}
import amf.aml.internal.parse.plugin.AMLDialectInstanceParsingPlugin
import amf.aml.internal.transform.pipelines.DialectInstanceTransformationPipeline
import amf.core.client.common.validation.ProfileName
import amf.core.client.scala.errorhandling.UnhandledErrorHandler
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.transform.TransformationPipelineRunner
import amf.core.client.scala.validation.AMFValidationReport
import amf.core.internal.plugins.validation.{ValidationOptions, ValidationResult}
import amf.core.internal.validation.core.ValidationProfile
import amf.core.internal.validation.{EffectiveValidations, ShaclReportAdaptation, ValidationConfiguration}
import amf.validation.internal.shacl.custom.CustomShaclValidator

import scala.concurrent.{ExecutionContext, Future}

class AMLValidator() extends ShaclReportAdaptation {

  def validate(baseUnit: BaseUnit, options: ValidationOptions)(
      implicit executionContext: ExecutionContext): Future[ValidationResult] = {

    lazy val configuration = options.config
    lazy val amfConfig     = options.config.amfConfig

    lazy val profile = options.profile

    lazy val knownDialects: Seq[Dialect] =
      amfConfig.registry.plugins.parsePlugins.collect {
        case plugin: AMLDialectInstanceParsingPlugin => plugin.dialect
      }

    lazy val validations = options.effectiveValidations

    baseUnit match {
      case dialectInstance: DialectInstanceUnit =>
        val pipelineRunner = TransformationPipelineRunner(UnhandledErrorHandler, amfConfig)
        val resolvedModel  = pipelineRunner.run(dialectInstance, DialectInstanceTransformationPipeline())
        val validationsFromDeps =
          computeValidationProfilesOfDependencies(dialectInstance, knownDialects, configuration.constraints)
        val validator        = new CustomShaclValidator(Map.empty, profile.messageStyle)
        val finalValidations = addValidations(validations, validationsFromDeps).effective.values.toSeq
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
