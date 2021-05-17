package amf.plugins.document.vocabularies

import amf.core.errorhandling.UnhandledErrorHandler
import amf.core.resolution.pipelines.TransformationPipelineRunner
import amf.core.validation.core.ValidationProfile
import amf.plugins.document.vocabularies.model.document.Dialect
import amf.plugins.document.vocabularies.resolution.pipelines.DialectTransformationPipeline
import amf.plugins.document.vocabularies.validation.AMFDialectValidations

object DialectValidationProfileComputation {

  def computeProfileFor(dialect: Dialect, registry: DialectsRegistry): ValidationProfile = {
    registry.registeredValidationProfileOf(dialect) match {
      case Some(profile) => profile
      case _ =>
        val copied = dialect.cloneUnit().asInstanceOf[Dialect]
        if (!copied.resolved) {
          val runner = TransformationPipelineRunner(UnhandledErrorHandler)
          runner.run(copied, DialectTransformationPipeline())
        }
        val profile = new AMFDialectValidations(copied).profile()
        profile
    }
  }
}
