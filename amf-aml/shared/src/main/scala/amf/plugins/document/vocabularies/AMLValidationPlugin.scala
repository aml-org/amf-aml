package amf.plugins.document.vocabularies

import amf.client.remod.amfcore.plugins.validate.{
  AMFValidatePlugin,
  ValidationInfo,
  ValidationOptions,
  ValidationResult
}
import amf.client.remod.amfcore.plugins.{HighPriority, PluginPriority}
import amf.client.remod.parsing.AMLDialectInstanceParsingPlugin
import amf.core.model.document.BaseUnit
import amf.plugins.document.vocabularies.model.document.DialectInstance

import scala.concurrent.{ExecutionContext, Future}

object AMLValidationPlugin {
  protected val id: String = this.getClass.getSimpleName
}

class AMLValidationPlugin() extends AMFValidatePlugin {

  override val id: String = AMLValidationPlugin.id

  override def priority: PluginPriority = HighPriority

  override def applies(info: ValidationInfo): Boolean = info.baseUnit.isInstanceOf[DialectInstance]

  override def validate(unit: BaseUnit, options: ValidationOptions)(
      implicit executionContext: ExecutionContext): Future[ValidationResult] = {
    val dialects = knownDialects(options)
    new AMLValidator(dialects, options.config.constraints)
      .validate(unit, options.profile, options.effectiveValidations)
  }

  private def knownDialects(options: ValidationOptions) =
    options.config.amfConfig.registry.plugins.parsePlugins.collect {
      case plugin: AMLDialectInstanceParsingPlugin => plugin.dialect
    }
}