package amf.plugins.document.vocabularies

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
        val nextDialect =
          if (!copied.resolved) {
            val runner          = TransformationPipelineRunner(copied.errorHandler())
            runner.run(copied, DialectTransformationPipeline())
          }
          else copied
        val profile = new AMFDialectValidations(nextDialect).profile()
        profile
    }
  }
}
