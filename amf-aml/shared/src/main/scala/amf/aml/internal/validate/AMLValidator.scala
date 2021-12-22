package amf.aml.internal.validate

import amf.aml.client.scala.model.document.{Dialect, DialectInstanceUnit}
import amf.aml.internal.parse.plugin.AMLDialectInstanceParsingPlugin
import amf.aml.internal.transform.pipelines.DialectInstanceTransformationPipeline
import amf.aml.internal.utils.DialectRegister
import amf.core.client.common.validation.{AmfProfile, ProfileName, UnknownProfile}
import amf.core.client.scala.AMFGraphConfiguration
import amf.core.client.scala.config.SkippedValidationPluginEvent
import amf.core.client.scala.errorhandling.UnhandledErrorHandler
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.transform.TransformationPipelineRunner
import amf.core.client.scala.validation.{AMFValidationReport, EffectiveValidationsCompute}
import amf.core.internal.plugins.validation.{ValidationOptions, ValidationResult}
import amf.core.internal.remote.AmlDialectSpec
import amf.core.internal.validation.core.ValidationProfile
import amf.core.internal.validation.{EffectiveValidations, ShaclReportAdaptation}
import amf.validation.internal.shacl.custom.CustomShaclValidator
import com.github.ghik.silencer.silent

import scala.concurrent.{ExecutionContext, Future}

object AMLValidator extends ShaclReportAdaptation with SemanticExtensionConstraints {

  def validate(baseUnit: BaseUnit, options: ValidationOptions)(
      implicit executionContext: ExecutionContext): Future[ValidationResult] = {

    val configuration = options.config
    val amfConfig     = options.config.amfConfig

    val knownDialects: Seq[Dialect] = collectDialects(amfConfig)

    val instanceProfile =
      baseUnit.sourceSpec.collect { case AmlDialectSpec(id) => ProfileName(id) }.getOrElse(AmfProfile)

    val validations: EffectiveValidations = buildValidations(instanceProfile, configuration.constraints)

    baseUnit match {
      case dialectInstance: DialectInstanceUnit =>
        val pipelineRunner = TransformationPipelineRunner(UnhandledErrorHandler, amfConfig)
        val resolvedModel  = pipelineRunner.run(dialectInstance, DialectInstanceTransformationPipeline())
        val validationsFromDeps =
          computeValidationProfilesOfDependencies(dialectInstance, knownDialects, configuration.constraints)
        val validator        = new CustomShaclValidator(Map.empty, instanceProfile.messageStyle)
        val finalValidations = addValidations(validations, validationsFromDeps)
        for {
          shaclReport <- validator.validate(resolvedModel, finalValidations.effective.values.toSeq)
        } yield {
          val report = adaptToAmfReport(baseUnit, instanceProfile, shaclReport, validations)
          ValidationResult(resolvedModel, report)
        }

      case _ =>
        notifyValidationSkipped(options)
        Future.successful(ValidationResult(baseUnit, AMFValidationReport.empty(baseUnit.id, UnknownProfile)))
    }
  }

  private def buildValidations(profile: ProfileName,
                               constraints: Map[ProfileName, ValidationProfile]): EffectiveValidations = {
    val instanceValidations = EffectiveValidationsCompute
      .build(profile, constraints)
      .getOrElse(EffectiveValidations())
    withSemanticExtensionsConstraints(instanceValidations, constraints)
  }

  private def collectDialects(amfConfig: => AMFGraphConfiguration) = {
    amfConfig.registry.getPluginsRegistry.rootParsePlugins.collect {
      case plugin: AMLDialectInstanceParsingPlugin => plugin.dialect
    }
  }

  private def notifyValidationSkipped(options: ValidationOptions) = {
    options.config.listeners.foreach { listener =>
      listener.notifyEvent(SkippedValidationPluginEvent("AMLValidator", "BaseUnit isn't a DialectInstance"))
    }
  }

  private def computeValidationProfilesOfDependencies(
      dialectInstance: DialectInstanceUnit,
      knownDialects: Seq[Dialect],
      constraints: Map[ProfileName, ValidationProfile])(implicit executionContext: ExecutionContext) = {
    @silent("deprecated") // Silent can only be used in assignment expressions
    val graphDependencies =
      if (dialectInstance.processingData.graphDependencies.nonEmpty) {
        dialectInstance.processingData.graphDependencies
      } else {
        dialectInstance.graphDependencies
      }
    graphDependencies
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
