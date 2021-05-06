package amf.plugins.document.vocabularies

import amf.core.validation.core.ValidationProfile
import amf.plugins.document.vocabularies.model.document.Dialect
import amf.plugins.document.vocabularies.resolution.pipelines.DialectResolutionPipeline
import amf.plugins.document.vocabularies.validation.AMFDialectValidations

object DialectValidationProfileComputation {

  def computeProfileFor(dialect: Dialect, registry: DialectsRegistry): ValidationProfile = {
    registry.registeredValidationProfileOf(dialect) match {
      case Some(profile) => profile
      case _ =>
        val resolvedDialect = DialectResolutionPipeline().transform(dialect, dialect.errorHandler())
        val profile         = new AMFDialectValidations(resolvedDialect).profile()
        profile
    }
  }

  def computeProfileOf(dialect: Dialect): ValidationProfile = {
    val resolvedDialect = DialectResolutionPipeline().transform(dialect, dialect.errorHandler())
    new AMFDialectValidations(resolvedDialect).profile()
  }
}
