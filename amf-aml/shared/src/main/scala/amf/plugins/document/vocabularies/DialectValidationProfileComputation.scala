package amf.plugins.document.vocabularies

import amf.core.validation.core.ValidationProfile
import amf.plugins.document.vocabularies.model.document.Dialect
import amf.plugins.document.vocabularies.resolution.pipelines.DialectTransformationPipeline
import amf.plugins.document.vocabularies.validation.AMFDialectValidations

object DialectValidationProfileComputation {

  def computeProfileFor(dialect: Dialect, registry: DialectsRegistry): ValidationProfile = {
    val header = dialect.header
    registry.validations.get(header) match {
      case Some(profile) => profile
      case _ =>
        val resolvedDialect = DialectTransformationPipeline().transform(dialect, dialect.errorHandler())
        val profile         = new AMFDialectValidations(resolvedDialect).profile()
        registry.validations += (header -> profile)
        profile
    }
  }
}
