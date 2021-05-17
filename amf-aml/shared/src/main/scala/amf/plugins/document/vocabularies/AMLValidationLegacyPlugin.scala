package amf.plugins.document.vocabularies

import amf.client.plugins.AMFValidationPlugin
import amf.client.remod.amfcore.plugins.validate.{
  AMFValidatePlugin,
  ValidationConfiguration,
  ValidationOptions,
  ValidationResult
}
import amf.client.remod.amfcore.plugins.{HighPriority, PluginPriority}
import amf.core.model.document.BaseUnit
import amf.core.validation.AMFValidationReport
import amf.plugins.document.vocabularies.model.document.DialectInstanceUnit

import scala.concurrent.{ExecutionContext, Future}

object AMLValidationLegacyPlugin {
  def amlPlugin(): AMLValidationLegacyPlugin = {
    def legacyApplies = (unit: BaseUnit) => unit.isInstanceOf[DialectInstanceUnit]

    AMLValidationLegacyPlugin(AMLPlugin(), legacyApplies)
  }
}

case class AMLValidationLegacyPlugin(plugin: AMLPlugin, legacyApplies: BaseUnit => Boolean) extends AMFValidatePlugin {

  override def validate(unit: BaseUnit, options: ValidationOptions)(
      implicit executionContext: ExecutionContext): Future[ValidationResult] = {
    new AMLValidator(plugin.registry).validate(unit, options.profileName, options.validations)
  }

  override val id: String = plugin.ID

  override def applies(element: BaseUnit): Boolean = legacyApplies(element)

  override def priority: PluginPriority = HighPriority
}
